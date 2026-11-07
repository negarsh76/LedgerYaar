package com.ledgeryaar.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime

/**
 * تاریخ شمسی - pure Kotlin بدون وابستگی به java.time (سازگار با iOS)
 */
data class PersianDate(
    val year: Int,
    val month: Int,
    val day: Int
) {
    val monthName: String get() = PERSIAN_MONTHS[month - 1]
    val displayShort: String get() = "$day $monthName"
    val displayFull: String get() = "$day $monthName $year"

    /** فرمت رقمی مثل 1403/04/15 — برای فیلد ورودی تاریخ */
    fun toInputString(): String =
        "$year/${month.toString().padStart(2, '0')}/${day.toString().padStart(2, '0')}"

    companion object {
        val PERSIAN_MONTHS = listOf(
            "فروردین", "اردیبهشت", "خرداد",
            "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر",
            "دی", "بهمن", "اسفند"
        )
        val PERSIAN_MONTHS_EN = listOf(
            "Farvardin", "Ordibehesht", "Khordad",
            "Tir", "Mordad", "Shahrivar",
            "Mehr", "Aban", "Azar",
            "Dey", "Bahman", "Esfand"
        )

        /** پارس رشته‌ی "1403/04/15" -> PersianDate یا null اگر نامعتبر بود */
        fun parse(text: String): PersianDate? {
            val m = Regex("""^(\d{4})/(\d{1,2})/(\d{1,2})$""").find(text.trim()) ?: return null
            val (y, mo, d) = m.destructured
            val year = y.toIntOrNull() ?: return null
            val month = mo.toIntOrNull() ?: return null
            val day = d.toIntOrNull() ?: return null
            return if (PersianCalendarConverter.isValidJalali(year, month, day)) PersianDate(year, month, day) else null
        }
    }
}

/**
 * تبدیل تقویم شمسی به میلادی و برعکس — الگوریتم Borkowski (public domain)، pure Kotlin.
 */
object PersianCalendarConverter {

    fun toJalali(gregorian: LocalDate): PersianDate {
        val gy = gregorian.year
        val gm = gregorian.monthNumber
        val gd = gregorian.dayOfMonth

        val gy2 = gy - 1600
        val gDaysInMonth = if (isGregorianLeap(gy)) GREGORIAN_DAYS_LEAP else GREGORIAN_DAYS_IN_MONTH

        var gDayNo = 365 * gy2 + div(gy2 + 3, 4) - div(gy2 + 99, 100) + div(gy2 + 399, 400)
        for (i in 0 until gm - 1) gDayNo += gDaysInMonth[i]
        gDayNo += gd - 1

        var jDayNo = gDayNo - 79

        val jNp = div(jDayNo, 12053)
        jDayNo %= 12053

        var jy = 979 + 33 * jNp + 4 * div(jDayNo, 1461)
        jDayNo %= 1461

        if (jDayNo >= 366) {
            jy += div(jDayNo - 1, 365)
            jDayNo = (jDayNo - 1) % 365
        }

        var i = 0
        while (i < 11 && jDayNo >= JALALI_DAYS_IN_MONTH[i]) {
            jDayNo -= JALALI_DAYS_IN_MONTH[i]
            i++
        }

        val jm = i + 1
        val jd = jDayNo + 1

        return PersianDate(jy, jm, jd)
    }

    fun toGregorian(persian: PersianDate): LocalDate = toGregorian(persian.year, persian.month, persian.day)

    fun toGregorian(jyIn: Int, jmIn: Int, jdIn: Int): LocalDate {
        val jy = jyIn + 1595
        var days = -355668 + (365 * jy) + (div(jy, 33) * 8) + div((jy % 33) + 3, 4) + jdIn
        days += if (jmIn < 7) (jmIn - 1) * 31 else ((jmIn - 7) * 30) + 186

        var gy = 400 * div(days, 146097)
        days %= 146097

        if (days > 36524) {
            days -= 1
            gy += 100 * div(days, 36524)
            days %= 36524
            if (days >= 365) days += 1
        }

        gy += 4 * div(days, 1461)
        days %= 1461

        if (days > 365) {
            gy += div(days - 1, 365)
            days = (days - 1) % 365
        }

        var gd = days + 1
        val isLeap = isGregorianLeap(gy)
        val salA = intArrayOf(0, 31, if (isLeap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gm = 1
        while (gm <= 12) {
            val v = salA[gm]
            if (gd <= v) break
            gd -= v
            gm++
        }

        return LocalDate(gy, gm, gd)
    }

    fun getCurrentPersianDate(): PersianDate {
        val today = kotlinx.datetime.Clock.System.now()
            .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
            .date
        return toJalali(today)
    }

    fun isJalaliLeap(jy: Int): Boolean {
        val leapRemainders = listOf(1, 5, 9, 13, 17, 22, 26, 30)
        return leapRemainders.contains(jy % 33)
    }

    fun daysInJalaliMonth(month: Int, year: Int): Int = when {
        month <= 6 -> 31
        month <= 11 -> 30
        month == 12 -> if (isJalaliLeap(year)) 30 else 29
        else -> 30
    }

    fun isValidJalali(year: Int, month: Int, day: Int): Boolean {
        if (month < 1 || month > 12) return false
        if (day < 1) return false
        return day <= daysInJalaliMonth(month, year)
    }

    /** اضافه‌کردن n ماه به یک تاریخ شمسی، با محدودکردن روز به آخرین روز ماه مقصد */
    fun addMonths(date: PersianDate, months: Int): PersianDate {
        var totalMonth = date.month - 1 + months
        var year = date.year + totalMonth / 12
        var month = totalMonth % 12
        if (month < 0) { month += 12; year -= 1 }
        month += 1
        val day = minOf(date.day, daysInJalaliMonth(month, year))
        return PersianDate(year, month, day)
    }

    // تقسیم صحیح رو به پایین — java.lang.Math.floorDiv فقط JVM هست و روی iOS کار نمی‌کنه
    private fun div(a: Int, b: Int): Int {
        val q = a / b
        return if (a % b != 0 && (a < 0) != (b < 0)) q - 1 else q
    }

    private fun isGregorianLeap(year: Int) =
        (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

    private val JALALI_DAYS_IN_MONTH = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)
    private val GREGORIAN_DAYS_IN_MONTH = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    private val GREGORIAN_DAYS_LEAP = intArrayOf(31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
}
