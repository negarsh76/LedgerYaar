package com.ledgeryaar.domain.analytics

import com.ledgeryaar.domain.model.PersonType
import com.ledgeryaar.domain.model.PersianCalendarConverter
import com.ledgeryaar.domain.model.Transaction
import com.ledgeryaar.domain.model.TxType
import com.ledgeryaar.domain.period.YearMonth
import com.ledgeryaar.domain.period.toYearMonth
import com.ledgeryaar.domain.usecase.LedgerData
import kotlinx.datetime.LocalDate
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong

// ────────────────────────────────────────────
// آمار ماهانه / سالانه
// ────────────────────────────────────────────

data class MonthStats(
    val ym: YearMonth,
    val income: Double,
    val expense: Double,
    val incomeByCategory: Map<String, Double>,
    val expenseByCategory: Map<String, Double>,
    val count: Int
) {
    val net: Double get() = income - expense
    val hasData: Boolean get() = count > 0
}

fun statsOf(ym: YearMonth, list: List<Transaction>): MonthStats {
    val incomes = list.filter { it.type == TxType.INCOME }
    val expenses = list.filter { it.type == TxType.EXPENSE }
    return MonthStats(
        ym = ym,
        income = incomes.map { it.amount }.sum(),
        expense = expenses.map { it.amount }.sum(),
        incomeByCategory = incomes.groupBy { it.category }.mapValues { e -> e.value.map { it.amount }.sum() },
        expenseByCategory = expenses.groupBy { it.category }.mapValues { e -> e.value.map { it.amount }.sum() },
        count = list.size
    )
}

fun List<Transaction>.groupedByMonth(): Map<YearMonth, List<Transaction>> = groupBy { it.date.toYearMonth() }

fun List<Transaction>.monthStats(ym: YearMonth): MonthStats =
    statsOf(ym, filter { it.date >= ym.startDate() && it.date <= ym.endDate() })

fun List<Transaction>.yearStats(year: Int): List<MonthStats> {
    val grouped = groupedByMonth()
    return (1..12).map { m ->
        val ym = YearMonth(year, m)
        statsOf(ym, grouped[ym].orEmpty())
    }
}

/** جمع دسته‌ها در چند ماه */
fun List<MonthStats>.sumByCategory(expense: Boolean): List<Pair<String, Double>> {
    val acc = mutableMapOf<String, Double>()
    forEach { ms ->
        val map = if (expense) ms.expenseByCategory else ms.incomeByCategory
        map.forEach { (k, v) -> acc[k] = (acc[k] ?: 0.0) + v }
    }
    return acc.entries.map { it.key to it.value }.sortedByDescending { it.second }
}

// ────────────────────────────────────────────
// پیش‌بینی و برنامه‌ریزی ماه (آماری، روی خود دستگاه)
// ────────────────────────────────────────────

enum class TipKind {
    NO_DATA, LOW_DATA, DEFICIT, LOW_SAVINGS, TOP_CATEGORY, HEAVY_COMMITMENTS,
    GOAL_PRESSURE, OVERSPEND_PACE, OVERDUE, ON_TRACK
}

data class Tip(val kind: TipKind, val amount: Double = 0.0, val label: String = "", val percent: Int = 0)

data class GoalPlan(val title: String, val monthlyNeeded: Double, val monthsLeft: Int)

data class Forecast(
    val ym: YearMonth,
    val monthsOfHistory: Int,
    val confidence: Int,
    val expectedIncome: Double,
    val incomeSoFar: Double,
    val expenseSoFar: Double,
    val commitments: Double,
    val receivablesDue: Double,
    val goalSavings: Double,
    /** پولی که بعد از اقساط/بدهی‌ها و پس‌انداز اهداف، برای خرج روزمرهٔ کل ماه می‌ماند */
    val spendable: Double,
    val remainingSpendable: Double,
    val daysLeft: Int,
    val dailyAllowance: Double,
    val projectedExpense: Double,
    val projectedLeftover: Double,
    val suggestedLimits: List<Pair<String, Double>>,
    val goalPlans: List<GoalPlan>,
    val tips: List<Tip>
)

/** پیش‌بینی یک سری ماهانه (قدیمی‌ترین اول): میانگین وزنی + روند خطی محدودشده */
fun predictNext(values: List<Double>): Double {
    val n = values.size
    if (n == 0) return 0.0
    if (n < 3) return values.sum() / n
    val weights = (1..n).map { it.toDouble() }
    val weighted = values.indices.map { values[it] * weights[it] }.sum() / weights.sum()
    val xs = (0 until n).map { it.toDouble() }
    val xMean = xs.sum() / n
    val yMean = values.sum() / n
    val den = xs.map { (it - xMean) * (it - xMean) }.sum()
    val slope = if (den == 0.0) 0.0 else xs.indices.map { (xs[it] - xMean) * (values[it] - yMean) }.sum() / den
    val trend = yMean + slope * (n - xMean)
    val clamped = if (yMean > 0) trend.coerceIn(yMean * 0.6, yMean * 1.4) else trend
    return (0.6 * weighted + 0.4 * clamped).coerceAtLeast(0.0)
}

/** گرد کردن «قشنگ» تا دو رقم معنادار */
fun niceRound(x: Double): Double {
    if (x <= 0.0) return 0.0
    val magnitude = 10.0.pow(floor(log10(x)) - 1)
    return (x / magnitude).roundToLong() * magnitude
}

fun buildForecast(data: LedgerData, ym: YearMonth, today: LocalDate): Forecast {
    val grouped = data.transactions.groupedByMonth()
    val todayYm = today.toYearMonth()
    val isCurrent = ym == todayYm
    val isPast = ym.index < todayYm.index

    // تاریخچه: تا ۶ ماه قبل از ماه انتخابی که داده دارند (قدیمی‌ترین اول)
    val history = (1..6).map { ym.plusMonths(-it) }
        .filter { grouped[it].orEmpty().isNotEmpty() }
        .map { statsOf(it, grouped[it].orEmpty()) }
        .reversed()
    val n = history.size

    val cur = statsOf(ym, grouped[ym].orEmpty())
    val predIncome = predictNext(history.map { it.income })
    val predExpense = predictNext(history.map { it.expense })

    val expectedIncome = if (isPast) cur.income else maxOf(cur.income, predIncome)

    val start = ym.startDate()
    val end = ym.endDate()
    val installmentsDue = data.installments
        .filter { !it.paid && it.dueDate >= start && it.dueDate <= end }
        .map { it.amount }.sum()
    val debtsDue = data.people
        .filter { p ->
            val d = p.dueDate
            p.type == PersonType.DEBT && !p.paid && d != null && d >= start && d <= end
        }
        .map { it.amount }.sum()
    val commitments = installmentsDue + debtsDue
    val receivablesDue = data.people
        .filter { p ->
            val d = p.dueDate
            p.type == PersonType.RECEIVABLE && !p.paid && d != null && d >= start && d <= end
        }
        .map { it.amount }.sum()

    // اهداف: هر هدفِ دارای موعد، سهم ماهانهٔ لازم
    val goalPlans = data.goals.mapNotNull { g ->
        val due = g.dueDate ?: return@mapNotNull null
        val remaining = g.target - g.current
        if (remaining <= 0) return@mapNotNull null
        val monthsLeft = maxOf(1, due.toYearMonth().index - ym.index + 1)
        GoalPlan(g.title, remaining / monthsLeft, monthsLeft)
    }
    val goalSavings = goalPlans.map { it.monthlyNeeded }.sum()

    val spendable = expectedIncome - commitments - goalSavings
    val remainingSpendable = spendable - cur.expense

    val daysInMonth = ym.daysInMonth()
    val todayDay = PersianCalendarConverter.toJalali(today).day
    val daysLeft = when {
        isCurrent -> daysInMonth - todayDay + 1
        isPast -> 0
        else -> daysInMonth
    }
    val dailyAllowance = if (daysLeft > 0) maxOf(0.0, remainingSpendable) / daysLeft else 0.0

    val pace = if (isCurrent && todayDay > 0) cur.expense / todayDay * daysInMonth else 0.0
    val projectedExpense = when {
        isPast -> cur.expense
        isCurrent -> maxOf(cur.expense, if (n > 0) 0.5 * pace + 0.5 * predExpense else pace)
        else -> predExpense
    }
    val projectedLeftover = expectedIncome - projectedExpense - commitments - goalSavings

    // سقف پیشنهادی هر دسته بر اساس میانگین تاریخچه، کوچک‌شده تا در بودجه جا شود
    val avgByCat = mutableMapOf<String, Double>()
    history.forEach { ms -> ms.expenseByCategory.forEach { (k, v) -> avgByCat[k] = (avgByCat[k] ?: 0.0) + v } }
    val avgList = avgByCat.entries.map { it.key to (it.value / maxOf(n, 1)) }.filter { it.second > 0 }
    val avgTotal = avgList.map { it.second }.sum()
    val variableBudget = maxOf(0.0, spendable)
    val factor = if (avgTotal > variableBudget && avgTotal > 0) variableBudget / avgTotal else 1.0
    val suggested = avgList
        .map { it.first to niceRound(it.second * factor) }
        .filter { it.second > 0 }
        .sortedByDescending { it.second }
        .take(8)

    val confidence = when (n) {
        0 -> 0
        1 -> 25
        2 -> 45
        3 -> 65
        4 -> 75
        5 -> 85
        else -> 92
    }

    val tips = mutableListOf<Tip>()
    if (n == 0 && !cur.hasData) {
        tips.add(Tip(TipKind.NO_DATA))
    } else {
        if (n < 2) tips.add(Tip(TipKind.LOW_DATA))
        if (projectedLeftover < 0) tips.add(Tip(TipKind.DEFICIT, amount = abs(projectedLeftover)))
        if (expectedIncome > 0) {
            val saveRate = (expectedIncome - projectedExpense) / expectedIncome
            if (saveRate < 0.10 && projectedLeftover >= 0) tips.add(Tip(TipKind.LOW_SAVINGS, percent = (saveRate * 100).toInt()))
            val commitRate = commitments / expectedIncome
            if (commitRate > 0.40) tips.add(Tip(TipKind.HEAVY_COMMITMENTS, percent = (commitRate * 100).toInt()))
        }
        val topSource = if (cur.expenseByCategory.isNotEmpty()) cur.expenseByCategory else avgByCat
        val topTotal = topSource.values.sum()
        val top = topSource.entries.maxByOrNull { it.value }
        if (top != null && topTotal > 0 && top.value / topTotal > 0.35) {
            tips.add(Tip(TipKind.TOP_CATEGORY, amount = top.value, label = top.key, percent = (top.value / topTotal * 100).toInt()))
        }
        if (goalSavings > 0) tips.add(Tip(TipKind.GOAL_PRESSURE, amount = goalSavings))
        if (isCurrent && spendable > 0 && pace > spendable * 1.1) tips.add(Tip(TipKind.OVERSPEND_PACE, amount = pace - spendable))
        val overdue = data.installments.filter { !it.paid && it.dueDate < today }.map { it.amount }.sum() +
            data.people.filter { p ->
                val d = p.dueDate
                p.type == PersonType.DEBT && !p.paid && d != null && d < today
            }.map { it.amount }.sum()
        if (overdue > 0) tips.add(Tip(TipKind.OVERDUE, amount = overdue))
        if (tips.none { it.kind == TipKind.DEFICIT || it.kind == TipKind.OVERSPEND_PACE || it.kind == TipKind.LOW_SAVINGS }) {
            tips.add(Tip(TipKind.ON_TRACK))
        }
    }

    return Forecast(
        ym = ym, monthsOfHistory = n, confidence = confidence,
        expectedIncome = expectedIncome, incomeSoFar = cur.income, expenseSoFar = cur.expense,
        commitments = commitments, receivablesDue = receivablesDue, goalSavings = goalSavings,
        spendable = spendable, remainingSpendable = remainingSpendable,
        daysLeft = daysLeft, dailyAllowance = dailyAllowance,
        projectedExpense = projectedExpense, projectedLeftover = projectedLeftover,
        suggestedLimits = suggested, goalPlans = goalPlans, tips = tips
    )
}
