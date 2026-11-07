package com.ledgeryaar.data.repository

import com.ledgeryaar.data.db.*
import com.ledgeryaar.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

interface LedgerRepository {
    fun transactions(): Flow<List<Transaction>>
    fun loans(): Flow<List<Loan>>
    fun installments(): Flow<List<Installment>>
    fun people(): Flow<List<Person>>
    fun budgets(): Flow<List<Budget>>
    fun goals(): Flow<List<Goal>>
    fun bankAccounts(): Flow<List<BankAccount>>
    fun assets(): Flow<List<Asset>>

    suspend fun addTransaction(tx: Transaction)
    suspend fun updateTransaction(tx: Transaction)
    suspend fun deleteTransaction(id: Long)

    suspend fun addLoan(loan: Loan)
    suspend fun updateLoan(loan: Loan)
    suspend fun deleteLoan(id: Long)
    suspend fun toggleInstallment(item: Installment)

    suspend fun addPerson(person: Person)
    suspend fun updatePerson(person: Person)
    suspend fun deletePerson(id: Long)

    suspend fun addBudget(budget: Budget)
    suspend fun updateBudget(budget: Budget)
    suspend fun deleteBudget(id: Long)

    suspend fun addGoal(goal: Goal)
    suspend fun updateGoal(goal: Goal)
    suspend fun deleteGoal(id: Long)

    suspend fun addBank(bank: BankAccount)
    suspend fun updateBank(bank: BankAccount)
    suspend fun deleteBank(id: Long)
    suspend fun addAsset(asset: Asset)
    suspend fun updateAsset(asset: Asset)
    suspend fun deleteAsset(id: Long)
}

class LedgerRepositoryImpl(private val db: LedgerDatabase) : LedgerRepository {

    override fun transactions(): Flow<List<Transaction>> =
        db.transactionDao().observeAll().map { list -> list.map { it.toDomain() } }

    override fun loans(): Flow<List<Loan>> =
        db.loanDao().observeLoans().map { list -> list.map { it.toDomain() } }

    override fun installments(): Flow<List<Installment>> =
        db.loanDao().observeInstallments().map { list -> list.map { it.toDomain() } }

    override fun people(): Flow<List<Person>> =
        db.personDao().observeAll().map { list -> list.map { it.toDomain() } }

    override fun budgets(): Flow<List<Budget>> =
        db.budgetDao().observeAll().map { list -> list.map { it.toDomain() } }

    override fun goals(): Flow<List<Goal>> =
        db.goalDao().observeAll().map { list -> list.map { it.toDomain() } }

    override fun bankAccounts(): Flow<List<BankAccount>> =
        db.assetDao().observeBanks().map { list -> list.map { it.toDomain() } }

    override fun assets(): Flow<List<Asset>> =
        db.assetDao().observeAssets().map { list -> list.map { it.toDomain() } }

    override suspend fun addTransaction(tx: Transaction) { db.transactionDao().insert(tx.toEntity()) }
    override suspend fun updateTransaction(tx: Transaction) = db.transactionDao().update(tx.toEntity())
    override suspend fun deleteTransaction(id: Long) = db.transactionDao().deleteById(id)

    override suspend fun addLoan(loan: Loan) {
        val loanId = db.loanDao().insertLoan(loan.toEntity())
        val installments = generateInstallments(loanId, loan).map { it.toEntity() }
        db.loanDao().insertInstallments(installments)
    }

    /** ویرایش وام: اقساط از نو ساخته می‌شوند ولی وضعیت «پرداخت‌شده» برای شماره‌های مشترک حفظ می‌شود */
    override suspend fun updateLoan(loan: Loan) {
        val paidNumbers = db.loanDao().installmentsForLoan(loan.id).filter { it.paid }.map { it.number }.toSet()
        db.loanDao().updateLoan(loan.toEntity())
        db.loanDao().deleteInstallmentsForLoan(loan.id)
        val fresh = generateInstallments(loan.id, loan).map { it.copy(paid = it.number in paidNumbers).toEntity() }
        db.loanDao().insertInstallments(fresh)
    }

    override suspend fun deleteLoan(id: Long) {
        db.loanDao().deleteInstallmentsForLoan(id)
        db.loanDao().deleteLoan(id)
    }

    override suspend fun toggleInstallment(item: Installment) =
        db.loanDao().updateInstallment(item.copy(paid = !item.paid).toEntity())

    override suspend fun addPerson(person: Person) {
        val now = Clock.System.now().toEpochMilliseconds()
        db.personDao().insert(person.copy(createdAt = now).toEntity())
    }
    override suspend fun updatePerson(person: Person) = db.personDao().update(person.toEntity())
    override suspend fun deletePerson(id: Long) = db.personDao().deleteById(id)

    override suspend fun addBudget(budget: Budget) { db.budgetDao().insert(budget.toEntity()) }
    override suspend fun updateBudget(budget: Budget) = db.budgetDao().update(budget.toEntity())
    override suspend fun deleteBudget(id: Long) = db.budgetDao().deleteById(id)

    override suspend fun addGoal(goal: Goal) { db.goalDao().insert(goal.toEntity()) }
    override suspend fun updateGoal(goal: Goal) = db.goalDao().update(goal.toEntity())
    override suspend fun deleteGoal(id: Long) = db.goalDao().deleteById(id)

    override suspend fun addBank(bank: BankAccount) { db.assetDao().insertBank(bank.toEntity()) }
    override suspend fun updateBank(bank: BankAccount) = db.assetDao().updateBank(bank.toEntity())
    override suspend fun deleteBank(id: Long) = db.assetDao().deleteBank(id)
    override suspend fun addAsset(asset: Asset) { db.assetDao().insertAsset(asset.toEntity()) }
    override suspend fun updateAsset(asset: Asset) = db.assetDao().updateAsset(asset.toEntity())
    override suspend fun deleteAsset(id: Long) = db.assetDao().deleteAsset(id)
}
