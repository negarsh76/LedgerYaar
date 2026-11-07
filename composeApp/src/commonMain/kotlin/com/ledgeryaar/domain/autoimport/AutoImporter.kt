package com.ledgeryaar.domain.autoimport

import com.ledgeryaar.data.db.LedgerDatabase
import com.ledgeryaar.data.db.toDomain
import com.ledgeryaar.data.db.toEntity
import com.ledgeryaar.data.repository.CurrencyOption
import com.ledgeryaar.data.repository.SettingsRepository
import com.ledgeryaar.domain.model.Transaction
import com.ledgeryaar.domain.model.TxType
import com.ledgeryaar.domain.model.categoryKeys
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * پیامک/نوتیف بانک ← تراکنش «در انتظار بررسی».
 * تراکنش همان لحظه در دفتر ثبت می‌شود (تا جمع‌ها درست باشد) ولی با needsReview=true،
 * تا تو از نوتیف یا صفحهٔ «بررسی» بگویی بابت چی بوده.
 */
class AutoImporter(
    private val db: LedgerDatabase,
    private val settings: SettingsRepository
) {
    private val mutex = Mutex()

    /**
     * @return تراکنش ثبت‌شده، یا null اگر پیام بانکی نبود / تکراری بود / ورود خودکار خاموش است
     */
    suspend fun import(
        text: String,
        sender: String?,
        timeMillis: Long,
        source: String,
        force: Boolean = false
    ): Transaction? = mutex.withLock {
        val s = settings.settings.first()
        if (!s.autoImport && !force) return null
        val parsed = BankMessageParser.parse(text, sender) ?: return null

        val amount = convertAmount(parsed.amount, parsed.unit, s.bankDefaultRial, s.currency)
        if (amount <= 0) return null

        val dao = db.transactionDao()
        val dedupeKey = parsed.balance?.let { "${parsed.type}|${parsed.amount.toLong()}|${it.toLong()}" }
        if (dedupeKey != null && dao.countByDedupeKey(dedupeKey) > 0) return null
        // پیامک و نوتیف همان تراکنش معمولاً با چند ثانیه فاصله می‌رسند
        val window = 10 * 60 * 1000L
        if (dao.countSimilarAuto(parsed.type.name, amount, timeMillis - window, timeMillis + window) > 0) return null

        val validKeys = categoryKeys(parsed.type, s.customCategories)
        val guessText = listOfNotNull(parsed.merchant, text).joinToString(" ")
        val category = CategoryGuesser.guess(guessText, parsed.type, s.learnedRules, validKeys)
            ?: CategoryGuesser.fallback(parsed.type)

        val date = Instant.fromEpochMilliseconds(timeMillis)
            .toLocalDateTime(TimeZone.currentSystemDefault()).date

        val note = listOfNotNull(parsed.bank?.let { "بانک $it" }, parsed.merchant).joinToString(" · ")
        val tx = Transaction(
            type = parsed.type,
            amount = amount,
            date = date,
            category = category,
            note = note,
            needsReview = true,
            source = source,
            rawText = text.take(500),
            merchant = parsed.merchant,
            dedupeKey = dedupeKey,
            sourceTime = timeMillis
        )
        val id = dao.insert(tx.toEntity())
        tx.copy(id = id)
    }

    /** تأیید از نوتیف یا صفحهٔ بررسی؛ اگر دسته عوض شد، یاد گرفته می‌شود */
    suspend fun confirm(id: Long, category: String? = null, note: String? = null) {
        val dao = db.transactionDao()
        val tx = dao.getById(id)?.toDomain() ?: return
        val s = settings.settings.first()
        val validKeys = categoryKeys(tx.type, s.customCategories)

        // اگر کاربر فقط نوشته «بابت چی بود» و دسته نداده، از روی نوشته‌اش حدس بزن
        val finalCategory = category
            ?: note?.let { CategoryGuesser.guess(it, tx.type, s.learnedRules, validKeys) }
            ?: tx.category
        val finalNote = when {
            note.isNullOrBlank() -> tx.note
            tx.note.isBlank() -> note.trim()
            else -> note.trim() + " · " + tx.note
        }
        dao.update(tx.copy(category = finalCategory, note = finalNote, needsReview = false).toEntity())

        if (finalCategory != tx.category || category != null) {
            learn(tx, finalCategory)
        }
    }

    suspend fun confirmAll() {
        val dao = db.transactionDao()
        dao.getAll().filter { it.needsReview }.forEach { dao.update(it.copy(needsReview = false)) }
    }

    private suspend fun learn(tx: Transaction, category: String) {
        val keyword = tx.merchant?.let { CategoryGuesser.keyOf(it) }?.takeIf { it.length >= 2 } ?: return
        settings.learnRule(keyword, category)
    }

    suspend fun pendingCount(): Int = db.transactionDao().getAll().count { it.needsReview }

    companion object {
        fun convertAmount(amount: Double, unit: MessageUnit, defaultRial: Boolean, currency: CurrencyOption): Double {
            val inRial = when (unit) {
                MessageUnit.RIAL -> true
                MessageUnit.TOMAN -> false
                MessageUnit.UNKNOWN -> defaultRial
            }
            return when (currency) {
                CurrencyOption.TOMAN -> if (inRial) kotlin.math.round(amount / 10.0) else amount
                CurrencyOption.RIAL -> if (inRial) amount else amount * 10
                else -> amount
            }
        }
    }
}
