package com.ledgeryaar.platform

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.provider.Telephony
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.app.NotificationManagerCompat
import com.ledgeryaar.autoimport.BankNotificationListener
import com.ledgeryaar.autoimport.ImportRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.ref.WeakReference

class AndroidPlatformBridge(private val app: Context) : PlatformBridge {

    private val _permissions = MutableStateFlow(readPermissions())
    override val permissions: StateFlow<PermissionState> = _permissions

    private var activityRef: WeakReference<ComponentActivity>? = null
    private var smsLauncher: ActivityResultLauncher<Array<String>>? = null
    private var notifLauncher: ActivityResultLauncher<String>? = null
    private var openDocLauncher: ActivityResultLauncher<Array<String>>? = null
    private var pickCallback: ((String?) -> Unit)? = null

    /** باید در onCreate اکتیویتی صدا زده شود (قبل از STARTED) */
    fun attach(activity: ComponentActivity) {
        activityRef = WeakReference(activity)
        smsLauncher = activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            refreshPermissions()
        }
        notifLauncher = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            refreshPermissions()
        }
        openDocLauncher = activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            val cb = pickCallback
            pickCallback = null
            if (uri == null) { cb?.invoke(null); return@registerForActivityResult }
            val text = runCatching {
                app.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            cb?.invoke(text)
        }
    }

    private fun granted(p: String) = ContextCompat.checkSelfPermission(app, p) == PackageManager.PERMISSION_GRANTED

    private fun readPermissions(): PermissionState {
        val hasTelephony = app.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
        return PermissionState(
            smsSupported = true,
            smsGranted = hasTelephony && granted(Manifest.permission.RECEIVE_SMS) && granted(Manifest.permission.READ_SMS),
            notificationListenerEnabled = NotificationManagerCompat.getEnabledListenerPackages(app).contains(app.packageName),
            postNotificationsGranted = Build.VERSION.SDK_INT < 33 || granted(Manifest.permission.POST_NOTIFICATIONS)
        )
    }

    override fun refreshPermissions() { _permissions.value = readPermissions() }

    /** اولین اجرا: یک‌جا اجازهٔ پیامک و نوتیف را می‌پرسد */
    fun requestInitialPermissionsOnce() {
        val prefs = app.getSharedPreferences("onboarding", Context.MODE_PRIVATE)
        if (prefs.getBoolean("asked", false)) return
        prefs.edit().putBoolean("asked", true).apply()
        val list = mutableListOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
        if (Build.VERSION.SDK_INT >= 33) list += Manifest.permission.POST_NOTIFICATIONS
        smsLauncher?.launch(list.toTypedArray())
    }

    override fun requestSmsPermission() {
        smsLauncher?.launch(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS))
    }

    override fun requestPostNotifications() {
        if (Build.VERSION.SDK_INT >= 33) notifLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS)
        else openAppNotificationSettings()
    }

    private fun openAppNotificationSettings() {
        val i = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, app.packageName)
        start(i)
    }

    override fun openNotificationListenerSettings() {
        val component = ComponentName(app, BankNotificationListener::class.java).flattenToString()
        val direct = if (Build.VERSION.SDK_INT >= 30) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component)
        } else null
        if (direct == null || !start(direct)) {
            start(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    private fun start(intent: Intent): Boolean {
        val act = activityRef?.get()
        return runCatching {
            if (act != null) act.startActivity(intent)
            else app.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
    }

    override suspend fun scanSmsInbox(days: Int): Int = withContext(Dispatchers.IO) {
        if (!granted(Manifest.permission.READ_SMS)) return@withContext 0
        val since = System.currentTimeMillis() - days * 24L * 60 * 60 * 1000
        val found = mutableListOf<Triple<String, String?, Long>>()
        app.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
            "${Telephony.Sms.DATE} >= ?",
            arrayOf(since.toString()),
            "${Telephony.Sms.DATE} ASC"
        )?.use { c ->
            val iAddr = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val iBody = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val iDate = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
            while (c.moveToNext()) {
                val body = c.getString(iBody) ?: continue
                found += Triple(body, c.getString(iAddr), c.getLong(iDate))
            }
        }
        var count = 0
        for ((body, sender, date) in found) {
            if (ImportRunner.handle(app, body, sender, date, "sms", notify = false, force = true)) count++
        }
        count
    }

    override fun sendBackupByEmail(email: String, fileName: String, content: String, subject: String, body: String) {
        val dir = File(app.cacheDir, "backups").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() } // فقط آخرین پشتیبان در حافظهٔ موقت می‌ماند
        val file = File(dir, fileName).apply { writeText(content) }
        val uri = FileProvider.getUriForFile(app, app.packageName + ".fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        start(Intent.createChooser(send, subject))
    }

    override fun pickBackupFile(onResult: (String?) -> Unit) {
        val launcher = openDocLauncher
        if (launcher == null) { onResult(null); return }
        pickCallback = onResult
        launcher.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*"))
    }
}
