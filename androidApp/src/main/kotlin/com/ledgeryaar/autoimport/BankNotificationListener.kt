package com.ledgeryaar.autoimport

import android.app.Notification
import android.provider.Telephony
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.launch

/**
 * نوتیف اپ‌های بانکی (مثل بلوبانک که بیشتر با نوتیف خبر می‌دهد تا پیامک) را می‌خواند.
 * بانک‌های ایران API عمومی برای حساب شخصی ندارند؛ این مطمئن‌ترین راه است.
 */
class BankNotificationListener : NotificationListenerService() {

    /** تکه‌هایی از نام بستهٔ اپ‌های بانکی و پرداخت ایرانی */
    private val bankPackageHints = listOf(
        "blu", "samanpr", "saman", "bank", "mellat", "bmi", "melli", "pasargad", "bpi", "tejarat",
        "sepah", "refah", "keshavarzi", "maskan", "ayandeh", "parsian", "resalat", "sinabank", "enbank",
        "eghtesad", "karafarin", "postbank", "sadad", "mobilebank", "mobank", "shahrbank", "tosee",
        "sarmayeh", "wepod", "vepod", "bankino"
    )

    private fun isBankApp(pkg: String): Boolean {
        if (pkg == packageName) return false
        // اپ پیامک پیش‌فرض را رد می‌کنیم چون پیامک‌ها را SmsReceiver می‌گیرد
        val smsApp = runCatching { Telephony.Sms.getDefaultSmsPackage(this) }.getOrNull()
        if (pkg == smsApp) return false
        val p = pkg.lowercase()
        return bankPackageHints.any { p.contains(it) }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!isBankApp(sbn.packageName)) return
        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val full = "$title\n$text".trim()
        if (full.isEmpty()) return
        val appContext = applicationContext
        ImportRunner.scope.launch {
            ImportRunner.handle(appContext, full, sbn.packageName, sbn.postTime, "notif")
        }
    }
}
