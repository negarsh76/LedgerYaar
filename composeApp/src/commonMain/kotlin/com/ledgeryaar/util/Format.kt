package com.ledgeryaar.util

import kotlin.math.roundToLong

private val PERSIAN_DIGITS = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

/** جداکننده‌ی هزارگان دستی — چون java.text.NumberFormat روی iOS در دسترس نیست */
fun groupThousands(value: Long): String {
    val negative = value < 0
    val s = kotlin.math.abs(value).toString()
    val sb = StringBuilder()
    for ((index, char) in s.reversed().withIndex()) {
        if (index != 0 && index % 3 == 0) sb.append(',')
        sb.append(char)
    }
    return (if (negative) "-" else "") + sb.reverse().toString()
}

fun toPersianDigits(input: String): String =
    input.map { ch -> if (ch.isDigit()) PERSIAN_DIGITS[ch - '0'] else ch }.joinToString("")

fun formatAmount(amount: Double, persianDigits: Boolean = false): String {
    val grouped = groupThousands(amount.roundToLong())
    return if (persianDigits) toPersianDigits(grouped) else grouped
}

/** تبدیل ارقام فارسی/عربی به انگلیسی و حذف جداکننده‌ها — تا کیبورد فارسی هم کار کند */
fun normalizeDigits(input: String): String {
    val sb = StringBuilder()
    for (ch in input) {
        when (ch) {
            in '۰'..'۹' -> sb.append('0' + (ch - '۰'))
            in '٠'..'٩' -> sb.append('0' + (ch - '٠'))
            '٫' -> sb.append('.')
            ',', '٬', ' ', '،' -> {}
            else -> sb.append(ch)
        }
    }
    return sb.toString()
}

/** مبلغ تایپ‌شده × ضریب واحد (هزار/میلیون/میلیارد)؛ اگر نامعتبر بود null */
fun parseAmount(text: String, multiplier: Long = 1L): Double? {
    val v = normalizeDigits(text).toDoubleOrNull() ?: return null
    return v * multiplier
}

/** عدد کامل بدون اعشار اضافه، برای پر کردن فیلد هنگام ویرایش */
fun plainNumber(amount: Double): String =
    if (amount == amount.roundToLong().toDouble()) amount.roundToLong().toString() else amount.toString()

/** خلاصه‌نویسی مبلغ بزرگ: 2.5 میلیون / 3 میلیارد */
fun compactAmount(amount: Double, fa: Boolean): String {
    val abs = kotlin.math.abs(amount)
    val (div, faName, enName) = when {
        abs >= 1_000_000_000 -> Triple(1_000_000_000.0, "میلیارد", "B")
        abs >= 1_000_000 -> Triple(1_000_000.0, "میلیون", "M")
        abs >= 1_000 -> Triple(1_000.0, "هزار", "K")
        else -> return formatAmount(amount)
    }
    val scaled = amount / div
    val rounded = (scaled * 10).roundToLong() / 10.0
    val text = if (rounded == rounded.roundToLong().toDouble()) rounded.roundToLong().toString() else rounded.toString()
    return if (fa) "$text $faName" else "$text$enName"
}

/** پارس تاریخ شمسی تایپ‌شده (با ارقام فارسی یا انگلیسی) */
fun parseJalaliInput(text: String): com.ledgeryaar.domain.model.PersianDate? =
    com.ledgeryaar.domain.model.PersianDate.parse(normalizeDigits(text))
