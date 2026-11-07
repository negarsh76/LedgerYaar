package com.ledgeryaar.autoimport

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import kotlinx.coroutines.launch

/** هر پیامک تازه را می‌گیرد؛ اگر پیامک بانکی بود، ثبت می‌کند */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return
        // پیامک‌های بلند در چند تکه می‌رسند؛ به ترتیب فرستنده کنار هم می‌گذاریم
        val grouped = messages.groupBy { it.originatingAddress ?: "" }
        val pending = goAsync()
        ImportRunner.scope.launch {
            try {
                grouped.forEach { (sender, parts) ->
                    val body = parts.joinToString("") { it.messageBody ?: "" }
                    val time = parts.first().timestampMillis.takeIf { it > 0 } ?: System.currentTimeMillis()
                    ImportRunner.handle(context, body, sender, time, "sms")
                }
            } finally {
                pending.finish()
            }
        }
    }
}
