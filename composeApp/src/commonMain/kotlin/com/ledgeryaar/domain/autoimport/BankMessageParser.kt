package com.ledgeryaar.domain.autoimport

import com.ledgeryaar.domain.model.TxType

/** واحد پولی که داخل متن پیامک/نوتیف بانک آمده */
enum class MessageUnit { RIAL, TOMAN, UNKNOWN }

/** نتیجهٔ خواندن یک پیامک یا نوتیف بانکی */
data class ParsedBankMessage(
    val type: TxType,
    /** مبلغ همان‌طور که در پیام آمده (بدون تبدیل واحد) */
    val amount: Double,
    val unit: MessageUnit,
    /** مانده‌ی حساب بعد از تراکنش، اگر در پیام بود */
    val balance: Double?,
    /** نام بانک (اگر از متن یا فرستنده معلوم شد) */
    val bank: String?,
    /** توضیح/طرف حساب (مثلاً «اسنپ»، «فروشگاه رفاه»)؛ برای یادگیری دسته استفاده می‌شود */
    val merchant: String?
)

/**
 * خواننده‌ی پیامک‌ها و نوتیف‌های بانکی ایران (ملی، ملت، سامان/بلو، پاسارگاد، تجارت، صادرات، رفاه و …).
 *
 * قالب پیامک‌ها بین بانک‌ها فرق دارد، پس به‌جای یک الگوی ثابت با چند نشانه کار می‌کنیم:
 *  - کلمه‌های برداشت/خرید/انتقال یا علامت منفی ← هزینه
 *  - کلمه‌های واریز/دریافت/سود یا علامت مثبت ← درآمد
 *  - عدد بعد از «مبلغ» یا کلمهٔ کلیدی ← مبلغ؛ عدد بعد از «مانده/موجودی» ← مانده
 * پیام‌های رمز پویا/کد تأیید و تبلیغاتی کنار گذاشته می‌شوند.
 */
object BankMessageParser {

    private val OTP_WORDS = listOf(
        "رمز", "کد تایید", "کد تأیید", "کدتایید", "رمز پویا", "رمزپویا", "otp", "password", "verification", "code:"
    )

    private val EXPENSE_WORDS = listOf(
        "برداشت", "خرید", "انتقال از", "کسر", "پرداخت", "انتقال وجه", "کارت به کارت", "پایا", "ساتنا",
        "قبض", "withdraw", "purchase", "debit"
    )
    private val INCOME_WORDS = listOf(
        "واریز", "انتقال به حساب شما", "دریافت", "سود", "بستانکار", "deposit", "credit"
    )

    private val BANK_NAMES = listOf(
        "بلو", "بلوبانک", "سامان", "ملی", "ملت", "پاسارگاد", "تجارت", "صادرات", "سپه", "رفاه", "مسکن",
        "کشاورزی", "آینده", "پارسیان", "اقتصاد نوین", "سینا", "رسالت", "شهر", "دی", "سرمایه", "خاورمیانه",
        "قوامین", "توسعه تعاون", "پست بانک", "کارآفرین", "مهر ایران", "گردشگری", "ایران زمین", "تات", "ویپاد"
    )

    private const val NUM = """([0-9][0-9,٬]*[0-9]|[0-9])"""

    private val AMOUNT_AFTER_KEYWORD = Regex(
        """(?:مبلغ|برداشت|واریز|خرید|انتقال|کسر|پرداخت|دریافت|amount)\s*[:：]?\s*(?:ريال|ریال|تومان)?\s*[:：]?\s*([-+]?)\s*$NUM\s*([-+]?)""",
        RegexOption.IGNORE_CASE
    )
    private val BALANCE = Regex(
        """(?:مانده|موجودی|ماند|balance)\s*(?:حساب)?\s*[:：]?\s*(?:ريال|ریال|تومان)?\s*[:：]?\s*([-+]?)\s*$NUM""",
        RegexOption.IGNORE_CASE
    )
    /** عدد با جداکننده‌ی هزارگان و علامت کنارش: 1,250,000-  یا  +500,000 */
    private val SIGNED_AMOUNT = Regex("""([-+])\s*([0-9]{1,3}(?:[,٬][0-9]{3})+)|([0-9]{1,3}(?:[,٬][0-9]{3})+)\s*([-+])""")
    /** هر عدد هزارگان‌دار (بدون علامت) — آخرین راه پیدا کردن مبلغ */
    private val GROUPED_AMOUNT = Regex("""[0-9]{1,3}(?:[,٬][0-9]{3})+""")
    private val MERCHANT = Regex("""(?:بابت|شرح|پذیرنده|فروشگاه|در\s+فروشگاه|به\s+نام|نزد)\s*[:：]?\s*([^\n\r:0-9،,]{2,40})""")

    fun normalize(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            sb.append(
                when (ch) {
                    in '۰'..'۹' -> '0' + (ch - '۰')
                    in '٠'..'٩' -> '0' + (ch - '٠')
                    'ي' -> 'ی'
                    'ك' -> 'ک'
                    '‌' -> ' ' // نیم‌فاصله
                    '−', '–', '—' -> '-'
                    else -> ch
                }
            )
        }
        return sb.toString()
    }

    private fun toNumber(raw: String): Double? {
        val digits = raw.filter { it.isDigit() }
        if (digits.isEmpty() || digits.length > 15) return null
        return digits.toDoubleOrNull()
    }

    /** شماره کارت/حساب ستاره‌دار (6037****1234) مبلغ نیست */
    private fun isMaskedNumber(text: String, range: IntRange): Boolean {
        val before = text.getOrNull(range.first - 1)
        val after = text.getOrNull(range.last + 1)
        return before == '*' || after == '*' || before == '#' || after == '#'
    }

    fun parse(rawText: String, sender: String? = null): ParsedBankMessage? {
        val text = normalize(rawText)
        val lower = text.lowercase()

        if (OTP_WORDS.any { lower.contains(it) } && !lower.contains("مانده") && !lower.contains("موجودی")) return null

        // مانده را جدا می‌کنیم تا با مبلغ قاطی نشود
        val balanceMatch = BALANCE.find(text)
        val balance = balanceMatch?.let { m ->
            toNumber(m.groupValues[2])?.let { if (m.groupValues[1] == "-") -it else it }
        }
        val textNoBalance = if (balanceMatch != null) text.removeRange(balanceMatch.range) else text

        // ۱) مبلغ بعد از کلمهٔ کلیدی
        var amount: Double? = null
        var sign: Char? = null
        AMOUNT_AFTER_KEYWORD.findAll(textNoBalance).forEach { m ->
            if (amount != null) return@forEach
            if (isMaskedNumber(textNoBalance, m.range)) return@forEach
            val n = toNumber(m.groupValues[2]) ?: return@forEach
            if (n < 1000) return@forEach // احتمالاً شماره یا تاریخ است
            amount = n
            val s = (m.groupValues[1] + m.groupValues[3]).trim()
            if (s.isNotEmpty()) sign = s.first()
        }
        // ۲) عدد هزارگان‌دار با علامت
        if (amount == null) {
            SIGNED_AMOUNT.find(textNoBalance)?.let { m ->
                val numStr = m.groupValues[2].ifEmpty { m.groupValues[3] }
                amount = toNumber(numStr)
                sign = (m.groupValues[1].ifEmpty { m.groupValues[4] }).firstOrNull()
            }
        }
        // ۳) اولین عدد هزارگان‌دار، مثل «۲۵۰,۰۰۰ ریال از حساب شما برداشت شد»
        if (amount == null) {
            GROUPED_AMOUNT.findAll(textNoBalance)
                .firstOrNull { !isMaskedNumber(textNoBalance, it.range) && (toNumber(it.value) ?: 0.0) >= 1000 }
                ?.let { amount = toNumber(it.value) }
        }
        val finalAmount = amount ?: return null
        if (finalAmount <= 0) return null

        val hasExpenseWord = EXPENSE_WORDS.any { lower.contains(it) }
        val hasIncomeWord = INCOME_WORDS.any { lower.contains(it) }
        val type = when {
            sign == '-' -> TxType.EXPENSE
            sign == '+' -> TxType.INCOME
            hasIncomeWord && !hasExpenseWord -> TxType.INCOME
            hasExpenseWord && !hasIncomeWord -> TxType.EXPENSE
            // «انتقال به حساب شما» و «واریز» اولویت دارند
            lower.contains("واریز") -> TxType.INCOME
            hasExpenseWord -> TxType.EXPENSE
            else -> return null // هیچ نشانه‌ای از تراکنش نیست (احتمالاً تبلیغ)
        }

        val unit = when {
            lower.contains("تومان") -> MessageUnit.TOMAN
            lower.contains("ریال") || lower.contains("rial") || lower.contains("irr") -> MessageUnit.RIAL
            else -> MessageUnit.UNKNOWN
        }

        val bank = BANK_NAMES.firstOrNull { name ->
            Regex("""(?:بانک\s*)$name|$name\s*بانک|^$name""").containsMatchIn(text)
        } ?: sender?.let { s -> BANK_NAMES.firstOrNull { s.contains(it) } }

        val merchant = MERCHANT.find(text)?.groupValues?.get(1)
            ?.replace(Regex("""\s+"""), " ")
            ?.trim()
            ?.takeIf { it.length >= 2 }

        return ParsedBankMessage(type, finalAmount, unit, balance, bank, merchant)
    }
}
