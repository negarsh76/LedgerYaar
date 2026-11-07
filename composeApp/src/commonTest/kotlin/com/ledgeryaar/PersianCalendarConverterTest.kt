package com.ledgeryaar

import com.ledgeryaar.domain.model.Loan
import com.ledgeryaar.domain.model.PersianCalendarConverter
import com.ledgeryaar.domain.model.PersianDate
import com.ledgeryaar.domain.model.generateInstallments
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PersianCalendarConverterTest {

    @Test
    fun knownGregorianToJalaliConversion() {
        // 2024-03-20 برابر است با 1403/01/01 (نوروز)
        val jalali = PersianCalendarConverter.toJalali(LocalDate(2024, 3, 20))
        assertEquals(1403, jalali.year)
        assertEquals(1, jalali.month)
        assertEquals(1, jalali.day)
    }

    @Test
    fun roundTripConversionIsStable() {
        val original = LocalDate(2025, 8, 25)
        val jalali = PersianCalendarConverter.toJalali(original)
        val back = PersianCalendarConverter.toGregorian(jalali)
        assertEquals(original, back)
    }

    @Test
    fun parseAndFormatAreConsistent() {
        val parsed = PersianDate.parse("1403/04/15")
        assertEquals(PersianDate(1403, 4, 15), parsed)
        assertEquals("1403/04/15", parsed!!.toInputString())
    }

    @Test
    fun invalidDateIsRejected() {
        assertEquals(null, PersianDate.parse("1403/13/01"))
        assertEquals(null, PersianDate.parse("not-a-date"))
    }

    @Test
    fun addMonthsRollsYearOver() {
        val date = PersianDate(1403, 11, 30)
        val result = PersianCalendarConverter.addMonths(date, 3)
        assertEquals(1404, result.year)
        assertEquals(2, result.month)
    }

    @Test
    fun generateInstallmentsProducesCorrectCountAndTotal() {
        val loan = Loan(title = "وام خرید", totalAmount = 120_000_000.0, installmentCount = 12, startDate = LocalDate(2024, 3, 20))
        val installments = generateInstallments(loanId = 1L, loan = loan)
        assertEquals(12, installments.size)
        assertTrue(kotlin.math.abs(installments.sumOf { it.amount } - loan.totalAmount) < 12.0)
        // هر قسط باید یک ماه بعد از قسط قبلی باشد
        for (i in 1 until installments.size) {
            assertTrue(installments[i].dueDate > installments[i - 1].dueDate)
        }
    }
}
