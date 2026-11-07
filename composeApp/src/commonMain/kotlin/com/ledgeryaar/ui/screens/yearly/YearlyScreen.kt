package com.ledgeryaar.ui.screens.yearly

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledgeryaar.domain.analytics.sumByCategory
import com.ledgeryaar.domain.analytics.yearStats
import com.ledgeryaar.domain.period.currentYearMonth
import com.ledgeryaar.i18n.LocalStrings
import com.ledgeryaar.i18n.t
import com.ledgeryaar.ui.LedgerViewModel
import com.ledgeryaar.ui.categoryName
import com.ledgeryaar.ui.components.*
import com.ledgeryaar.ui.theme.AppColors
import com.ledgeryaar.util.formatAmount
import org.koin.compose.viewmodel.koinViewModel

/** عملکرد سالانه: سود/زیان، جدول ماهانه، نمودارها و تحلیل همهٔ دسته‌ها */
@Composable
fun YearlyScreen(viewModel: LedgerViewModel = koinViewModel()) {
    val strings = LocalStrings.current
    val state by viewModel.state.collectAsState()
    var year by remember { mutableStateOf(currentYearMonth().year) }
    val c = strings.currency

    val months = remember(state, year) { state.transactions.yearStats(year) }
    val totalIncome = months.map { it.income }.sum()
    val totalExpense = months.map { it.expense }.sum()
    val profit = totalIncome - totalExpense
    val activeMonths = months.count { it.hasData }.coerceAtLeast(1)
    val savingsRate = if (totalIncome > 0) (profit / totalIncome * 100).toInt() else 0
    val expenseCats = months.sumByCategory(expense = true)
    val incomeCats = months.sumByCategory(expense = false)
    val best = months.filter { it.hasData }.maxByOrNull { it.net }
    val worst = months.filter { it.hasData }.minByOrNull { it.net }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Text(strings.t("عملکرد سالانه", "Yearly report"), style = MaterialTheme.typography.headlineMedium) }
        item {
            LedgerCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = { year -= 1 }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null) }
                    TextButton(onClick = { year = currentYearMonth().year }) {
                        Text(strings.t("سال ", "Year ") + year, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(onClick = { year += 1 }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(strings.t("کل درآمد", "Total income"), formatAmount(totalIncome) + " " + c, AppColors.LedgerGreen, Modifier.weight(1f))
                StatTile(strings.t("کل هزینه", "Total expenses"), formatAmount(totalExpense) + " " + c, AppColors.StampRed, Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    if (profit >= 0) strings.t("سود سال", "Net profit") else strings.t("زیان سال", "Net loss"),
                    formatAmount(profit) + " " + c,
                    if (profit >= 0) AppColors.LedgerGreen else AppColors.StampRed, Modifier.weight(1f)
                )
                StatTile(strings.t("نرخ پس‌انداز", "Savings rate"), "$savingsRate%", modifier = Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(strings.t("میانگین درآمد ماهانه", "Avg monthly income"), formatAmount(totalIncome / activeMonths) + " " + c, modifier = Modifier.weight(1f))
                StatTile(strings.t("میانگین هزینه ماهانه", "Avg monthly expense"), formatAmount(totalExpense / activeMonths) + " " + c, modifier = Modifier.weight(1f))
            }
        }
        item {
            LedgerCard {
                Text(strings.t("نمودار ماهانه", "Monthly chart"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("■ " + strings.t("درآمد", "Income"), fontSize = 11.sp, color = AppColors.LedgerGreen)
                    Text("■ " + strings.t("هزینه", "Expense"), fontSize = 11.sp, color = AppColors.StampRed)
                }
                Spacer(Modifier.height(8.dp))
                DualBarChart(
                    labels = months.map { it.ym.monthName(strings.isFa).take(if (strings.isFa) 4 else 3) },
                    income = months.map { it.income },
                    expense = months.map { it.expense }
                )
            }
        }
        item {
            LedgerCard {
                Text(strings.t("جدول ماهانه", "Monthly table"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                TableRow(strings.t("ماه", "Month"), strings.t("درآمد", "Income"), strings.t("هزینه", "Expense"), strings.t("خالص", "Net"), header = true)
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                months.forEach { m ->
                    TableRow(
                        m.ym.monthName(strings.isFa),
                        chartAmount(m.income, strings.isFa), chartAmount(m.expense, strings.isFa), chartAmount(m.net, strings.isFa),
                        netColor = if (!m.hasData) null else if (m.net >= 0) AppColors.LedgerGreen else AppColors.StampRed
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                TableRow(
                    strings.t("جمع", "Total"),
                    chartAmount(totalIncome, strings.isFa), chartAmount(totalExpense, strings.isFa), chartAmount(profit, strings.isFa),
                    header = true, netColor = if (profit >= 0) AppColors.LedgerGreen else AppColors.StampRed
                )
            }
        }
        if (best != null && worst != null) {
            item {
                LedgerCard {
                    RowSpaceBetween {
                        Text(strings.t("بهترین ماه", "Best month"), fontSize = 12.sp)
                        Text(best.ym.monthName(strings.isFa) + "  " + formatAmount(best.net) + " " + c, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColors.LedgerGreen)
                    }
                    Spacer(Modifier.height(6.dp))
                    RowSpaceBetween {
                        Text(strings.t("ضعیف‌ترین ماه", "Weakest month"), fontSize = 12.sp)
                        Text(worst.ym.monthName(strings.isFa) + "  " + formatAmount(worst.net) + " " + c, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColors.StampRed)
                    }
                }
            }
        }
        item {
            LedgerCard {
                Text(strings.t("هزینه‌ها بر اساس دسته", "Expenses by category"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                if (expenseCats.isEmpty()) EmptyState(strings.noExpensesYet) else {
                    DonutWithLegend(expenseCats.take(8).map { categoryName(it.first) to it.second }, c)
                    Spacer(Modifier.height(10.dp))
                    CategoryTable(expenseCats.map { categoryName(it.first) to it.second }, totalExpense, activeMonths, c, strings.isFa)
                }
            }
        }
        item {
            LedgerCard {
                Text(strings.t("درآمدها بر اساس دسته", "Income by category"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                if (incomeCats.isEmpty()) EmptyState(strings.noIncomesYet) else {
                    CategoryTable(incomeCats.map { categoryName(it.first) to it.second }, totalIncome, activeMonths, c, strings.isFa)
                }
            }
        }
    }
}

@Composable
private fun TableRow(a: String, b: String, c: String, d: String, header: Boolean = false, netColor: androidx.compose.ui.graphics.Color? = null) {
    val weight = if (header) FontWeight.Bold else FontWeight.Normal
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(a, Modifier.weight(1.1f), fontSize = 11.sp, fontWeight = weight)
        Text(b, Modifier.weight(1f), fontSize = 11.sp, fontWeight = weight, color = AppColors.LedgerGreen)
        Text(c, Modifier.weight(1f), fontSize = 11.sp, fontWeight = weight, color = AppColors.StampRed)
        Text(d, Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = netColor ?: MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun CategoryTable(rows: List<Pair<String, Double>>, total: Double, activeMonths: Int, currency: String, fa: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(if (fa) "دسته" else "Category", Modifier.weight(1.2f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(if (fa) "جمع سال" else "Year", Modifier.weight(1.2f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(if (fa) "میانگین/ماه" else "Avg/mo", Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("%", Modifier.weight(0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        HorizontalDivider()
        rows.forEach { (name, sum) ->
            Row(Modifier.fillMaxWidth()) {
                Text(name, Modifier.weight(1.2f), fontSize = 11.sp)
                Text(chartAmount(sum, fa) + " " + currency, Modifier.weight(1.2f), fontSize = 11.sp)
                Text(chartAmount(sum / activeMonths, fa), Modifier.weight(1f), fontSize = 11.sp)
                Text((if (total > 0) (sum / total * 100).toInt() else 0).toString() + "%", Modifier.weight(0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
