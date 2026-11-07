package com.ledgeryaar.data.backup

import com.ledgeryaar.data.db.*
import com.ledgeryaar.data.repository.SettingsRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * پشتیبان کامل دفتر به شکل یک فایل JSON:
 * همهٔ تراکنش‌ها، وام‌ها و اقساط، بدهی/طلب، بودجه، اهداف، حساب‌ها، دارایی‌ها،
 * دسته‌های دلخواه و قانون‌های یادگرفته.
 * تاریخ‌ها به‌صورت epoch day (عدد روز) ذخیره می‌شوند.
 */
@Serializable
data class BackupFile(
    val app: String = "LedgerYaar",
    val formatVersion: Int = 1,
    val createdAt: Long,
    val transactions: List<TxDto>,
    val loans: List<LoanDto>,
    val installments: List<InstallmentDto>,
    val people: List<PersonDto>,
    val budgets: List<BudgetDto>,
    val goals: List<GoalDto>,
    val banks: List<BankDto>,
    val assets: List<AssetDto>,
    val categoriesRaw: String = "",
    val nextCategoryId: Long = 1,
    val rulesRaw: String = ""
)

@Serializable data class TxDto(
    val id: Long, val type: String, val amount: Double, val date: Int, val category: String, val note: String,
    val needsReview: Boolean = false, val source: String = "", val rawText: String? = null,
    val merchant: String? = null, val dedupeKey: String? = null, val sourceTime: Long = 0
)
@Serializable data class LoanDto(val id: Long, val title: String, val totalAmount: Double, val installmentCount: Int, val startDate: Int)
@Serializable data class InstallmentDto(val id: Long, val loanId: Long, val number: Int, val amount: Double, val dueDate: Int, val paid: Boolean)
@Serializable data class PersonDto(val id: Long, val type: String, val name: String, val amount: Double, val dueDate: Int?, val paid: Boolean, val createdAt: Long)
@Serializable data class BudgetDto(val id: Long, val category: String, val amount: Double)
@Serializable data class GoalDto(val id: Long, val title: String, val target: Double, val current: Double, val dueDate: Int?)
@Serializable data class BankDto(val id: Long, val bank: String, val title: String, val balance: Double)
@Serializable data class AssetDto(val id: Long, val title: String, val value: Double)

class BackupService(
    private val db: LedgerDatabase,
    private val settings: SettingsRepository
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false; encodeDefaults = true }

    suspend fun export(nowMillis: Long): String {
        val (cats, nextId, rules) = settings.exportRaw()
        val file = BackupFile(
            createdAt = nowMillis,
            transactions = db.transactionDao().getAll().map {
                TxDto(it.id, it.type, it.amount, dateToEpochDay(it.date), it.category, it.note,
            it.needsReview, it.source, it.rawText, it.merchant, it.dedupeKey, it.sourceTime)
            },
            loans = db.loanDao().getAllLoan().map { LoanDto(it.id, it.title, it.totalAmount, it.installmentCount, dateToEpochDay(it.startDate)) },
            installments = db.loanDao().getAllInstallment().map { InstallmentDto(it.id, it.loanId, it.number, it.amount, dateToEpochDay(it.dueDate), it.paid) },
            people = db.personDao().getAllPerson().map { PersonDto(it.id, it.type, it.name, it.amount, it.dueDate?.let(::dateToEpochDay), it.paid, it.createdAt) },
            budgets = db.budgetDao().getAllBudget().map { BudgetDto(it.id, it.category, it.amount) },
            goals = db.goalDao().getAllGoal().map { GoalDto(it.id, it.title, it.target, it.current, it.dueDate?.let(::dateToEpochDay)) },
            banks = db.assetDao().getAllBank().map { BankDto(it.id, it.bank, it.title, it.balance) },
            assets = db.assetDao().getAllAsset().map { AssetDto(it.id, it.title, it.value) },
            categoriesRaw = cats, nextCategoryId = nextId, rulesRaw = rules
        )
        return json.encodeToString(BackupFile.serializer(), file)
    }

    /** همهٔ داده‌های فعلی با محتوای فایل جایگزین می‌شود. @return تعداد تراکنش‌های بازیابی‌شده */
    suspend fun restore(content: String): Int {
        val f = json.decodeFromString(BackupFile.serializer(), content)
        require(f.app == "LedgerYaar") { "not a LedgerYaar backup" }
        db.transactionDao().deleteAll()
        db.loanDao().deleteAllInstallment()
        db.loanDao().deleteAllLoan()
        db.personDao().deleteAllPerson()
        db.budgetDao().deleteAllBudget()
        db.goalDao().deleteAllGoal()
        db.assetDao().deleteAllBank()
        db.assetDao().deleteAllAsset()

        db.transactionDao().insertAll(f.transactions.map {
            TransactionEntity(it.id, it.type, it.amount, epochDayToDate(it.date), it.category, it.note,
                it.needsReview, it.source, it.rawText, it.merchant, it.dedupeKey, it.sourceTime)
        })
        db.loanDao().insertAllLoan(f.loans.map { LoanEntity(it.id, it.title, it.totalAmount, it.installmentCount, epochDayToDate(it.startDate)) })
        db.loanDao().insertInstallments(f.installments.map { InstallmentEntity(it.id, it.loanId, it.number, it.amount, epochDayToDate(it.dueDate), it.paid) })
        db.personDao().insertAllPerson(f.people.map { PersonEntity(it.id, it.type, it.name, it.amount, it.dueDate?.let(::epochDayToDate), it.paid, it.createdAt) })
        db.budgetDao().insertAllBudget(f.budgets.map { BudgetEntity(it.id, it.category, it.amount) })
        db.goalDao().insertAllGoal(f.goals.map { GoalEntity(it.id, it.title, it.target, it.current, it.dueDate?.let(::epochDayToDate)) })
        db.assetDao().insertAllBank(f.banks.map { BankAccountEntity(it.id, it.bank, it.title, it.balance) })
        db.assetDao().insertAllAsset(f.assets.map { AssetEntity(it.id, it.title, it.value) })
        settings.restoreRaw(f.categoriesRaw, f.nextCategoryId, f.rulesRaw)
        return f.transactions.size
    }
}
