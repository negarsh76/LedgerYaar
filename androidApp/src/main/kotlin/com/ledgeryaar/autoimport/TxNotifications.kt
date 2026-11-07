package com.ledgeryaar.autoimport

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.ledgeryaar.MainActivity
import com.ledgeryaar.R
import com.ledgeryaar.domain.model.CategoryId
import com.ledgeryaar.domain.model.Transaction
import com.ledgeryaar.domain.model.TxType
import com.ledgeryaar.util.formatAmount

/** ساخت و نمایش نوتیف‌های اپ */
object TxNotifications {
    const val CHANNEL_TX = "auto_tx"
    const val CHANNEL_REMINDER = "reminders"
    const val EXTRA_TX_ID = "tx_id"
    const val EXTRA_CATEGORY = "category"
    const val EXTRA_OPEN_ROUTE = "open_route"
    const val KEY_REPLY = "reply_note"
    const val ACTION_CONFIRM = "com.ledgeryaar.CONFIRM_TX"
    const val ACTION_REPLY = "com.ledgeryaar.REPLY_TX"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_TX, "تراکنش‌های بانکی جدید", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "وقتی پیامک یا نوتیف بانک خوانده و ثبت شد"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDER, "یادآوری قسط و بدهی", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun openAppIntent(context: Context, route: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_ROUTE, route)
        }
        return PendingIntent.getActivity(
            context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun categoryLabel(key: String): String = when (key) {
        CategoryId.FOOD.name -> "خوراک"
        CategoryId.RESTAURANT.name -> "رستوران"
        CategoryId.TRANSPORT.name -> "رفت‌وآمد"
        CategoryId.BILLS.name -> "قبض"
        CategoryId.ENTERTAINMENT.name -> "تفریح"
        CategoryId.SHOPPING.name -> "خرید"
        CategoryId.HEALTH.name -> "سلامت"
        CategoryId.EDUCATION.name -> "آموزش"
        CategoryId.SALARY.name -> "حقوق"
        CategoryId.PROJECT.name -> "پروژه"
        CategoryId.BONUS.name -> "پاداش"
        CategoryId.OTHER_EXPENSE.name, CategoryId.OTHER_INCOME.name -> "متفرقه"
        else -> "دستهٔ من"
    }

    /**
     * نوتیف «تراکنش جدید»:
     *  - دکمهٔ «بابت چی بود؟» — همان‌جا تایپ می‌کنی (مثلاً «ناهار با بچه‌ها») و دسته خودش حدس زده می‌شود
     *  - دکمهٔ «درسته: <دسته>» — دستهٔ پیشنهادی را تأیید می‌کند
     *  - لمس خود نوتیف ← صفحهٔ بررسی
     */
    fun showNewTransaction(context: Context, tx: Transaction, currencyLabel: String) {
        if (!canPost(context)) return
        ensureChannels(context)
        val id = tx.id.toInt()
        val sign = if (tx.type == TxType.INCOME) "واریز" else "برداشت"
        val title = "$sign ${formatAmount(tx.amount)} $currencyLabel"
        val body = buildString {
            append("بابت چی بود؟")
            if (tx.note.isNotBlank()) append(" · ").append(tx.note)
        }

        val replyIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_REPLY
            putExtra(EXTRA_TX_ID, tx.id)
        }
        val replyPending = PendingIntent.getBroadcast(
            context, id * 10 + 1, replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val remoteInput = RemoteInput.Builder(KEY_REPLY).setLabel("مثلاً: ناهار، اسنپ، قبض برق").build()
        val replyAction = NotificationCompat.Action.Builder(0, "بابت چی بود؟", replyPending)
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(false)
            .build()

        val confirmIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_CONFIRM
            putExtra(EXTRA_TX_ID, tx.id)
            putExtra(EXTRA_CATEGORY, tx.category)
        }
        val confirmPending = PendingIntent.getBroadcast(
            context, id * 10 + 2, confirmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val n = NotificationCompat.Builder(context, CHANNEL_TX)
            .setSmallIcon(R.drawable.ic_stat_ledger)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openAppIntent(context, "review", id * 10))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .addAction(replyAction)
            .addAction(0, "درسته: ${categoryLabel(tx.category)}", confirmPending)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notificationId(tx.id), n)
        } catch (_: SecurityException) { }
    }

    fun notificationId(txId: Long): Int = 100_000 + (txId % 1_000_000).toInt()

    fun cancel(context: Context, txId: Long) {
        NotificationManagerCompat.from(context).cancel(notificationId(txId))
    }

    fun showReminder(context: Context, id: Int, title: String, body: String, route: String) {
        if (!canPost(context)) return
        ensureChannels(context)
        val n = NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_stat_ledger)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openAppIntent(context, route, id))
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, n)
        } catch (_: SecurityException) { }
    }
}
