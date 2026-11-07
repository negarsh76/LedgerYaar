package com.ledgeryaar.domain.period

import com.ledgeryaar.domain.model.PersianCalendarConverter
import com.ledgeryaar.domain.model.PersianDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/** ماه و سال شمسی */
data class YearMonth(val year: Int, val month: Int) {
    val index: Int get() = year * 12 + (month - 1)

    fun plusMonths(n: Int): YearMonth {
        val i = index + n
        return YearMonth(i.floorDiv(12), i.mod(12) + 1)
    }

    fun startDate(): LocalDate = PersianCalendarConverter.toGregorian(year, month, 1)

    fun daysInMonth(): Int = PersianCalendarConverter.daysInJalaliMonth(month, year)

    fun endDate(): LocalDate = PersianCalendarConverter.toGregorian(year, month, daysInMonth())

    fun contains(date: LocalDate): Boolean = date >= startDate() && date <= endDate()

    fun monthName(fa: Boolean): String =
        if (fa) PersianDate.PERSIAN_MONTHS[month - 1] else PersianDate.PERSIAN_MONTHS_EN[month - 1]
}

fun LocalDate.toYearMonth(): YearMonth {
    val j = PersianCalendarConverter.toJalali(this)
    return YearMonth(j.year, j.month)
}

fun todayDate(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

fun currentYearMonth(): YearMonth = todayDate().toYearMonth()

data class Period(val ym: YearMonth, val showAll: Boolean = false)

/**
 * ماه/سال انتخاب‌شده‌ی مشترک بین همه‌ی صفحه‌ها.
 * وقتی ماه واقعی عوض می‌شود (و کاربر روی ماه «جاری» بوده)، خودکار به ماه جدید می‌رود؛
 * داده‌های ماه قبل در دیتابیس می‌مانند و با رفتن به همان ماه دیده می‌شوند.
 */
class PeriodHolder {
    private var lastAutoMonth: YearMonth = currentYearMonth()
    private val _state = MutableStateFlow(Period(lastAutoMonth))
    val state: StateFlow<Period> = _state.asStateFlow()

    fun set(ym: YearMonth) { _state.value = _state.value.copy(ym = ym) }
    fun next() { set(_state.value.ym.plusMonths(1)) }
    fun previous() { set(_state.value.ym.plusMonths(-1)) }
    fun goToToday() { set(currentYearMonth()) }
    fun setShowAll(all: Boolean) { _state.value = _state.value.copy(showAll = all) }

    fun refreshIfNewMonth() {
        val now = currentYearMonth()
        if (now != lastAutoMonth) {
            if (_state.value.ym == lastAutoMonth) set(now)
            lastAutoMonth = now
        }
    }
}
