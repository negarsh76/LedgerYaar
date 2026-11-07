package com.ledgeryaar.data.db

import androidx.room.AutoMigration
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import com.ledgeryaar.domain.model.Asset
import com.ledgeryaar.domain.model.BankAccount
import com.ledgeryaar.domain.model.Budget
import com.ledgeryaar.domain.model.Goal
import com.ledgeryaar.domain.model.Installment
import com.ledgeryaar.domain.model.Loan
import com.ledgeryaar.domain.model.Person
import com.ledgeryaar.domain.model.PersonType
import com.ledgeryaar.domain.model.Transaction
import com.ledgeryaar.domain.model.TxType
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

// ────────────────────────────────────────────
// Type converters — kotlinx.datetime.LocalDate <-> epoch day (Int)، سازگار با اندروید و iOS
// ────────────────────────────────────────────

class Converters {
    @TypeConverter
    fun fromEpochDay(value: Int?): LocalDate? = value?.let { epochDayToDate(it) }

    @TypeConverter
    fun toEpochDay(date: LocalDate?): Int? = date?.let { dateToEpochDay(it) }
}

// تبدیل تاریخ میلادی <-> epoch day بدون وابستگی به extensionهای kotlinx.datetime
// (الگوریتم civil-days از Howard Hinnant؛ مستقل از نسخهٔ کتابخانه)
internal fun dateToEpochDay(date: LocalDate): Int {
    val m = date.monthNumber
    val y = if (m <= 2) date.year - 1 else date.year
    val era = y.floorDiv(400)
    val yoe = y - era * 400
    val mp = if (m > 2) m - 3 else m + 9
    val doy = (153 * mp + 2) / 5 + date.dayOfMonth - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146097 + doe - 719468
}

internal fun epochDayToDate(epochDay: Int): LocalDate {
    val z = epochDay + 719468
    val era = z.floorDiv(146097)
    val doe = z - era * 146097
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = doy - (153 * mp + 2) / 5 + 1
    val m = if (mp < 10) mp + 3 else mp - 9
    val y = yoe + era * 400 + (if (m <= 2) 1 else 0)
    return LocalDate(y, m, d)
}

// ────────────────────────────────────────────
// Entities
// ────────────────────────────────────────────

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val amount: Double,
    val date: LocalDate,
    val category: String,
    val note: String,
    @ColumnInfo(defaultValue = "0") val needsReview: Boolean = false,
    @ColumnInfo(defaultValue = "''") val source: String = "",
    val rawText: String? = null,
    val merchant: String? = null,
    val dedupeKey: String? = null,
    @ColumnInfo(defaultValue = "0") val sourceTime: Long = 0
)

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val totalAmount: Double,
    val installmentCount: Int,
    val startDate: LocalDate
)

@Entity(tableName = "installments")
data class InstallmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    val number: Int,
    val amount: Double,
    val dueDate: LocalDate,
    val paid: Boolean
)

@Entity(tableName = "people")
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val name: String,
    val amount: Double,
    val dueDate: LocalDate?,
    val paid: Boolean,
    val createdAt: Long
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val amount: Double
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val target: Double,
    val current: Double,
    val dueDate: LocalDate?
)

@Entity(tableName = "bank_accounts")
data class BankAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bank: String,
    val title: String,
    val balance: Double
)

@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val value: Double
)

// ────────────────────────────────────────────
// DAOs
// ────────────────────────────────────────────

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Insert
    suspend fun insert(entity: TransactionEntity): Long

    @Update
    suspend fun update(entity: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT COUNT(*) FROM transactions WHERE dedupeKey = :key")
    suspend fun countByDedupeKey(key: String): Int

    /** تراکنش خودکار با همین مبلغ و نوع در بازهٔ زمانی داده‌شده (برای جلوگیری از ثبت دوباره‌ی پیامک + نوتیف) */
    @Query("SELECT COUNT(*) FROM transactions WHERE source != '' AND type = :type AND amount = :amount AND sourceTime BETWEEN :from AND :to")
    suspend fun countSimilarAuto(type: String, amount: Double, from: Long, to: Long): Int

    @Query("SELECT * FROM transactions")
    suspend fun getAll(): List<TransactionEntity>

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(items: List<TransactionEntity>)
}

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans")
    suspend fun getAllLoan(): List<LoanEntity>

    @Query("SELECT * FROM installments")
    suspend fun getAllInstallment(): List<InstallmentEntity>

    @Query("DELETE FROM loans")
    suspend fun deleteAllLoan()

    @Query("DELETE FROM installments")
    suspend fun deleteAllInstallment()

    @Insert
    suspend fun insertAllLoan(items: List<LoanEntity>)

    @Query("SELECT * FROM loans ORDER BY startDate DESC")
    fun observeLoans(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM installments ORDER BY dueDate ASC")
    fun observeInstallments(): Flow<List<InstallmentEntity>>

    @Insert
    suspend fun insertLoan(entity: LoanEntity): Long

    @Insert
    suspend fun insertInstallments(items: List<InstallmentEntity>)

    @Update
    suspend fun updateInstallment(item: InstallmentEntity)

    @Update
    suspend fun updateLoan(entity: LoanEntity)

    @Query("SELECT * FROM installments WHERE loanId = :loanId")
    suspend fun installmentsForLoan(loanId: Long): List<InstallmentEntity>

    @Query("DELETE FROM loans WHERE id = :id")
    suspend fun deleteLoan(id: Long)

    @Query("DELETE FROM installments WHERE loanId = :loanId")
    suspend fun deleteInstallmentsForLoan(loanId: Long)
}

@Dao
interface PersonDao {
    @Query("SELECT * FROM people")
    suspend fun getAllPerson(): List<PersonEntity>

    @Query("DELETE FROM people")
    suspend fun deleteAllPerson()

    @Insert
    suspend fun insertAllPerson(items: List<PersonEntity>)

    @Query("SELECT * FROM people ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PersonEntity>>

    @Insert
    suspend fun insert(entity: PersonEntity): Long

    @Update
    suspend fun update(entity: PersonEntity)

    @Query("DELETE FROM people WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets")
    suspend fun getAllBudget(): List<BudgetEntity>

    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudget()

    @Insert
    suspend fun insertAllBudget(items: List<BudgetEntity>)

    @Query("SELECT * FROM budgets")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Insert
    suspend fun insert(entity: BudgetEntity): Long

    @Update
    suspend fun update(entity: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals")
    suspend fun getAllGoal(): List<GoalEntity>

    @Query("DELETE FROM goals")
    suspend fun deleteAllGoal()

    @Insert
    suspend fun insertAllGoal(items: List<GoalEntity>)

    @Query("SELECT * FROM goals")
    fun observeAll(): Flow<List<GoalEntity>>

    @Insert
    suspend fun insert(entity: GoalEntity): Long

    @Update
    suspend fun update(entity: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface AssetDao {
    @Query("SELECT * FROM bank_accounts")
    suspend fun getAllBank(): List<BankAccountEntity>

    @Query("SELECT * FROM assets")
    suspend fun getAllAsset(): List<AssetEntity>

    @Query("DELETE FROM bank_accounts")
    suspend fun deleteAllBank()

    @Query("DELETE FROM assets")
    suspend fun deleteAllAsset()

    @Insert
    suspend fun insertAllBank(items: List<BankAccountEntity>)

    @Insert
    suspend fun insertAllAsset(items: List<AssetEntity>)

    @Query("SELECT * FROM bank_accounts")
    fun observeBanks(): Flow<List<BankAccountEntity>>

    @Query("SELECT * FROM assets")
    fun observeAssets(): Flow<List<AssetEntity>>

    @Insert
    suspend fun insertBank(entity: BankAccountEntity): Long

    @Insert
    suspend fun insertAsset(entity: AssetEntity): Long

    @Update
    suspend fun updateBank(entity: BankAccountEntity)

    @Update
    suspend fun updateAsset(entity: AssetEntity)

    @Query("DELETE FROM bank_accounts WHERE id = :id")
    suspend fun deleteBank(id: Long)

    @Query("DELETE FROM assets WHERE id = :id")
    suspend fun deleteAsset(id: Long)
}

// ────────────────────────────────────────────
// Database
// ────────────────────────────────────────────

@Database(
    entities = [
        TransactionEntity::class, LoanEntity::class, InstallmentEntity::class,
        PersonEntity::class, BudgetEntity::class, GoalEntity::class,
        BankAccountEntity::class, AssetEntity::class
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)]
)
@TypeConverters(Converters::class)
abstract class LedgerDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun loanDao(): LoanDao
    abstract fun personDao(): PersonDao
    abstract fun budgetDao(): BudgetDao
    abstract fun goalDao(): GoalDao
    abstract fun assetDao(): AssetDao
}

// ────────────────────────────────────────────
// Mappers
// ────────────────────────────────────────────

fun TransactionEntity.toDomain() = Transaction(id, TxType.valueOf(type), amount, date, category, note, needsReview, source, rawText, merchant, dedupeKey, sourceTime)
fun Transaction.toEntity() = TransactionEntity(id, type.name, amount, date, category, note, needsReview, source, rawText, merchant, dedupeKey, sourceTime)

fun LoanEntity.toDomain() = Loan(id, title, totalAmount, installmentCount, startDate)
fun Loan.toEntity() = LoanEntity(id, title, totalAmount, installmentCount, startDate)

fun InstallmentEntity.toDomain() = Installment(id, loanId, number, amount, dueDate, paid)
fun Installment.toEntity() = InstallmentEntity(id, loanId, number, amount, dueDate, paid)

fun PersonEntity.toDomain() = Person(id, PersonType.valueOf(type), name, amount, dueDate, paid, createdAt)
fun Person.toEntity() = PersonEntity(id, type.name, name, amount, dueDate, paid, createdAt)

fun BudgetEntity.toDomain() = Budget(id, category, amount)
fun Budget.toEntity() = BudgetEntity(id, category, amount)

fun GoalEntity.toDomain() = Goal(id, title, target, current, dueDate)
fun Goal.toEntity() = GoalEntity(id, title, target, current, dueDate)

fun BankAccountEntity.toDomain() = BankAccount(id, bank, title, balance)
fun BankAccount.toEntity() = BankAccountEntity(id, bank, title, balance)

fun AssetEntity.toDomain() = Asset(id, title, value)
fun Asset.toEntity() = AssetEntity(id, title, value)

