package com.ledgeryaar.autoimport

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import kotlinx.coroutines.launch

/** دکمه‌های نوتیف تراکنش: «درسته» و جواب «بابت چی بود؟» */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val txId = intent.getLongExtra(TxNotifications.EXTRA_TX_ID, -1L)
        if (txId <= 0) return
        val pending = goAsync()
        ImportRunner.scope.launch {
            try {
                when (intent.action) {
                    TxNotifications.ACTION_CONFIRM -> {
                        ImportRunner.confirm(txId, intent.getStringExtra(TxNotifications.EXTRA_CATEGORY), null)
                    }
                    TxNotifications.ACTION_REPLY -> {
                        val note = RemoteInput.getResultsFromIntent(intent)
                            ?.getCharSequence(TxNotifications.KEY_REPLY)?.toString()?.trim()
                        // دسته null است تا از روی نوشته‌ات حدس زده شود
                        ImportRunner.confirm(txId, null, note?.takeIf { it.isNotEmpty() })
                    }
                }
                TxNotifications.cancel(context, txId)
            } finally {
                pending.finish()
            }
        }
    }
}
