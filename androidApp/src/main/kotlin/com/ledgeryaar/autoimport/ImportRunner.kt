package com.ledgeryaar.autoimport

import android.content.Context
import com.ledgeryaar.data.repository.SettingsRepository
import com.ledgeryaar.domain.autoimport.AutoImporter
import com.ledgeryaar.i18n.AppLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** نقطهٔ مشترک پیامک‌گیر و نوتیف‌خوان: ثبت + نمایش نوتیف */
object ImportRunner : KoinComponent {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val importer: AutoImporter by inject()
    private val settings: SettingsRepository by inject()

    suspend fun handle(
        context: Context, text: String, sender: String?, time: Long, source: String,
        notify: Boolean = true, force: Boolean = false
    ): Boolean {
        val tx = runCatching { importer.import(text, sender, time, source, force) }.getOrNull() ?: return false
        if (notify) {
            val s = settings.settings.first()
            val label = if (s.language == AppLanguage.FA) s.currency.fa else s.currency.en
            TxNotifications.showNewTransaction(context.applicationContext, tx, label)
        }
        return true
    }

    suspend fun confirm(txId: Long, category: String?, note: String?) {
        importer.confirm(txId, category, note)
    }
}
