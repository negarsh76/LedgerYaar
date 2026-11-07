package com.ledgeryaar.domain.model

import kotlinx.datetime.LocalDate

enum class TxType { INCOME, EXPENSE }
enum class PersonType { DEBT, RECEIVABLE }

enum class CategoryId {
    // expense
    FOOD, RESTAURANT, TRANSPORT, BILLS, ENTERTAINMENT, SHOPPING, HEALTH, EDUCATION, OTHER_EXPENSE,
    // income
    SALARY, PROJECT, BONUS, OTHER_INCOME
}

val EXPENSE_CATEGORIES = listOf(
    CategoryId.FOOD, CategoryId.RESTAURANT, CategoryId.TRANSPORT, CategoryId.BILLS,
    CategoryId.ENTERTAINMENT, CategoryId.SHOPPING, CategoryId.HEALTH, CategoryId.EDUCATION, CategoryId.OTHER_EXPENSE
)

val INCOME_CATEGORIES = listOf(
    CategoryId.SALARY, CategoryId.PROJECT, CategoryId.BONUS, CategoryId.OTHER_INCOME
)

/** دستهٔ دلخواه کاربر؛ کلید ذخیره‌شده در تراکنش‌ها به شکل c_<id> است */
data class CustomCategory(val id: Long, val name: String, val type: TxType) {
    val key: String get() = "c_$id"
}

fun builtinCategory(key: String): CategoryId? = CategoryId.values().firstOrNull { it.name == key }

/** کلید همهٔ دسته‌ها (پیش‌فرض + دلخواه) برای یک نوع */
fun categoryKeys(type: TxType, custom: List<CustomCategory>): List<String> {
    val base = if (type == TxType.INCOME) INCOME_CATEGORIES else EXPENSE_CATEGORIES
    return base.map { it.name } + custom.filter { it.type == type }.map { it.key }
}

data class Transaction(
    val id: Long = 0,
    val type: TxType,
    val amount: Double,
    val date: LocalDate,
    val category: String,
    val note: String = "",
    /** تراکنشی که خودکار از پیامک/نوتیف بانک آمده و هنوز تأیید نشده */
    val needsReview: Boolean = false,
    /** منبع: "" (دستی)، "sms" یا "notif" */
    val source: String = "",
    /** متن اصلی پیامک/نوتیف (برای نمایش در صفحهٔ بررسی) */
    val rawText: String? = null,
    /** طرف حساب استخراج‌شده؛ برای یادگرفتن دسته */
    val merchant: String? = null,
    /** کلید جلوگیری از ثبت تکراری (مثلاً بر اساس مانده) */
    val dedupeKey: String? = null,
    /** زمان پیامک/نوتیف به میلی‌ثانیه */
    val sourceTime: Long = 0
)

data class Loan(
    val id: Long = 0,
    val title: String,
    val totalAmount: Double,
    val installmentCount: Int,
    val startDate: LocalDate
)

data class Installment(
    val id: Long = 0,
    val loanId: Long,
    val number: Int,
    val amount: Double,
    val dueDate: LocalDate,
    val paid: Boolean = false
)

data class Person(
    val id: Long = 0,
    val type: PersonType,
    val name: String,
    val amount: Double,
    val dueDate: LocalDate? = null,
    val paid: Boolean = false,
    val createdAt: Long = 0
)

data class Budget(
    val id: Long = 0,
    val category: String,
    val amount: Double
)

data class Goal(
    val id: Long = 0,
    val title: String,
    val target: Double,
    val current: Double,
    val dueDate: LocalDate? = null
)

data class BankAccount(
    val id: Long = 0,
    val bank: String,
    val title: String,
    val balance: Double
)

data class Asset(
    val id: Long = 0,
    val title: String,
    val value: Double
)

/** تولید اقساط ماهانه بر پایه‌ی تقویم شمسی، شروع از تاریخ اولین قسط */
fun generateInstallments(loanId: Long, loan: Loan): List<Installment> {
    val perInstallment = kotlin.math.round(loan.totalAmount / loan.installmentCount)
    val startJalali = PersianCalendarConverter.toJalali(loan.startDate)
    return (1..loan.installmentCount).map { n ->
        val due = PersianCalendarConverter.addMonths(startJalali, n - 1)
        Installment(
            loanId = loanId,
            number = n,
            amount = perInstallment,
            dueDate = PersianCalendarConverter.toGregorian(due)
        )
    }
}
