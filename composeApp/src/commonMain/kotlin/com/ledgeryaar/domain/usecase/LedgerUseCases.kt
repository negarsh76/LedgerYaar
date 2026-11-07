package com.ledgeryaar.domain.usecase

import com.ledgeryaar.data.repository.LedgerRepository
import com.ledgeryaar.data.repository.SettingsRepository
import com.ledgeryaar.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

// ────────────────────────────────────────────
// Actions — همهٔ عملیات نوشتن (افزودن / ویرایش / حذف) در یک‌جا
// ────────────────────────────────────────────

class LedgerActions(
    private val repo: LedgerRepository,
    private val settings: SettingsRepository
) {
    // تراکنش‌ها
    suspend fun saveTransaction(tx: Transaction) =
        if (tx.id == 0L) repo.addTransaction(tx) else repo.updateTransaction(tx)
    suspend fun deleteTransaction(id: Long) = repo.deleteTransaction(id)

    // وام‌ها و اقساط
    suspend fun saveLoan(loan: Loan) = if (loan.id == 0L) repo.addLoan(loan) else repo.updateLoan(loan)
    suspend fun deleteLoan(id: Long) = repo.deleteLoan(id)
    suspend fun toggleInstallment(item: Installment) = repo.toggleInstallment(item)

    // بدهی / طلب
    suspend fun savePerson(person: Person) =
        if (person.id == 0L) repo.addPerson(person) else repo.updatePerson(person)
    suspend fun togglePersonPaid(person: Person) = repo.updatePerson(person.copy(paid = !person.paid))
    suspend fun deletePerson(id: Long) = repo.deletePerson(id)

    // بودجه: برای هر دسته فقط یک سقف (اگر بود به‌روزرسانی می‌شود)
    suspend fun saveBudget(category: String, amount: Double) {
        val existing = repo.budgets().first().firstOrNull { it.category == category }
        if (existing == null) repo.addBudget(Budget(category = category, amount = amount))
        else repo.updateBudget(existing.copy(amount = amount))
    }
    suspend fun applyBudgets(limits: List<Pair<String, Double>>) {
        limits.forEach { (category, amount) -> saveBudget(category, amount) }
    }
    suspend fun deleteBudget(id: Long) = repo.deleteBudget(id)

    // اهداف
    suspend fun saveGoal(goal: Goal) = if (goal.id == 0L) repo.addGoal(goal) else repo.updateGoal(goal)
    suspend fun deleteGoal(id: Long) = repo.deleteGoal(id)

    // حساب‌ها و دارایی‌ها
    suspend fun saveBank(bank: BankAccount) = if (bank.id == 0L) repo.addBank(bank) else repo.updateBank(bank)
    suspend fun deleteBank(id: Long) = repo.deleteBank(id)
    suspend fun saveAsset(asset: Asset) = if (asset.id == 0L) repo.addAsset(asset) else repo.updateAsset(asset)
    suspend fun deleteAsset(id: Long) = repo.deleteAsset(id)

    // دسته‌بندی‌های دلخواه
    suspend fun addCategory(name: String, type: TxType) = settings.addCategory(name, type)
    suspend fun renameCategory(id: Long, name: String) = settings.renameCategory(id, name)
    suspend fun deleteCategory(id: Long) = settings.deleteCategory(id)
}

// ────────────────────────────────────────────
// Aggregate state — همه‌ی داده‌های خام دفتر کل، به‌همراه محاسبات مشتق‌شده
// ────────────────────────────────────────────

data class LedgerData(
    val transactions: List<Transaction> = emptyList(),
    val loans: List<Loan> = emptyList(),
    val installments: List<Installment> = emptyList(),
    val people: List<Person> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val bankAccounts: List<BankAccount> = emptyList(),
    val assets: List<Asset> = emptyList()
) {
    private val today get() = Clock.System.todayIn(TimeZone.currentSystemDefault())
    private val todayJalali get() = PersianCalendarConverter.toJalali(today)

    /** اول ماه شمسی جاری (نه میلادی) — چون کل اپ با تقویم شمسی کار می‌کند */
    private val monthStart: kotlinx.datetime.LocalDate get() =
        PersianCalendarConverter.toGregorian(todayJalali.year, todayJalali.month, 1)

    private val daysInCurrentJalaliMonth: Int get() =
        PersianCalendarConverter.daysInJalaliMonth(todayJalali.month, todayJalali.year)

    private val monthEnd: kotlinx.datetime.LocalDate get() =
        PersianCalendarConverter.toGregorian(todayJalali.year, todayJalali.month, daysInCurrentJalaliMonth)

    val incomes get() = transactions.filter { it.type == TxType.INCOME }
    val expenses get() = transactions.filter { it.type == TxType.EXPENSE }

    val monthIncome get() = incomes.filter { it.date >= monthStart }.sumOf { it.amount }
    val monthExpense get() = expenses.filter { it.date >= monthStart }.sumOf { it.amount }
    val monthNet get() = monthIncome - monthExpense

    val bankTotal get() = bankAccounts.sumOf { it.balance }
    val assetsTotal get() = assets.sumOf { it.value }
    val activeDebtTotal get() = people.filter { it.type == PersonType.DEBT && !it.paid }.sumOf { it.amount }
    val activeReceivableTotal get() = people.filter { it.type == PersonType.RECEIVABLE && !it.paid }.sumOf { it.amount }
    val remainingInstallmentsTotal get() = installments.filter { !it.paid }.sumOf { it.amount }
    val netWorth get() = bankTotal + assetsTotal + activeReceivableTotal - activeDebtTotal - remainingInstallmentsTotal

    data class DueItem(val label: String, val amount: Double, val date: kotlinx.datetime.LocalDate)

    fun upcomingDues(loanLabel: (Long) -> String, debtLabel: (String) -> String): List<DueItem> {
        val list = mutableListOf<DueItem>()
        installments.filter { !it.paid && it.dueDate >= today }
            .forEach { list.add(DueItem(loanLabel(it.loanId) + " — ${it.number}", it.amount, it.dueDate)) }
        people.filter { it.type == PersonType.DEBT && !it.paid && it.dueDate != null }
            .forEach { list.add(DueItem(debtLabel(it.name), it.amount, it.dueDate!!)) }
        return list.sortedBy { it.date }.take(8)
    }

    fun loanTitle(loanId: Long): String = loans.find { it.id == loanId }?.title ?: ""

    data class CashflowWeek(val number: Int, val start: kotlinx.datetime.LocalDate, val end: kotlinx.datetime.LocalDate, val certain: Double)

    fun cashflowForecastWeeks(weeks: Int = 4): List<CashflowWeek> {
        val result = mutableListOf<CashflowWeek>()
        var start = today
        repeat(weeks) { w ->
            val end = start.plus(6, DateTimeUnit.DAY)
            val installmentsTotal: Double = installments
                .filter { !it.paid && it.dueDate >= start && it.dueDate <= end }
                .sumOf { it.amount }
            val debtsTotal: Double = people
                .filter { p ->
                    val d = p.dueDate
                    p.type == PersonType.DEBT && !p.paid && d != null && d >= start && d <= end
                }
                .sumOf { it.amount }
            val certain: Double = installmentsTotal + debtsTotal
            result.add(CashflowWeek(w + 1, start, end, certain))
            start = end.plus(1, DateTimeUnit.DAY)
        }
        return result
    }

    val expectedRemainingIncomeThisMonth: Double get() {
        val daysPassed = todayJalali.day
        val dailyAvg = if (daysPassed > 0) monthIncome / daysPassed else 0.0
        return dailyAvg * (daysInCurrentJalaliMonth - todayJalali.day)
    }

    val remainingPaymentsThisMonth: Double get() {
        return installments.filter { !it.paid && it.dueDate in today..monthEnd }.sumOf { it.amount } +
            people.filter { it.type == PersonType.DEBT && !it.paid && it.dueDate != null && it.dueDate!! in today..monthEnd }.sumOf { it.amount }
    }

    fun budgetSpent(category: String): Double =
        expenses.filter { it.category == category && it.date >= monthStart }.sumOf { it.amount }
}

class ObserveLedgerDataUseCase(private val repo: LedgerRepository) {
    operator fun invoke(): Flow<LedgerData> = kotlinx.coroutines.flow.combine(
        repo.transactions(), repo.loans(), repo.installments(), repo.people(),
        repo.budgets(), repo.goals(), repo.bankAccounts(), repo.assets()
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        LedgerData(
            transactions = values[0] as List<Transaction>,
            loans = values[1] as List<Loan>,
            installments = values[2] as List<Installment>,
            people = values[3] as List<Person>,
            budgets = values[4] as List<Budget>,
            goals = values[5] as List<Goal>,
            bankAccounts = values[6] as List<BankAccount>,
            assets = values[7] as List<Asset>
        )
    }
}
