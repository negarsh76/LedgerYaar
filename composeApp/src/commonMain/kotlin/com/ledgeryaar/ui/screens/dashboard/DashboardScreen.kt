package com.ledgeryaar.ui.screens.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledgeryaar.domain.analytics.buildForecast
import com.ledgeryaar.domain.analytics.monthStats
import com.ledgeryaar.domain.model.PersianCalendarConverter
import com.ledgeryaar.domain.model.TxType
import com.ledgeryaar.domain.period.PeriodHolder
import com.ledgeryaar.domain.period.todayDate
import com.ledgeryaar.i18n.LocalStrings
import com.ledgeryaar.i18n.t
import com.ledgeryaar.ui.LedgerViewModel
import com.ledgeryaar.ui.categoryName
import com.ledgeryaar.ui.components.*
import com.ledgeryaar.ui.theme.AppColors
import com.ledgeryaar.util.formatAmount
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DashboardScreen(onOpenReview: () -> Unit = {}, viewModel: LedgerViewModel = koinViewModel()) {
    val strings = LocalStrings.current
    val holder: PeriodHolder = koinInject()
    val period by holder.state.collectAsState()
    val state by viewModel.state.collectAsState()
    val c = strings.currency
    val ym = period.ym
    val today = todayDate()

    val cur = remember(state, ym) { state.transactions.monthStats(ym) }
    val forecast = remember(state, ym) { buildForecast(state, ym, today) }
    // شش ماه منتهی به ماه انتخابی (برای نمودار روند)
    val trend = remember(state, ym) {
        val all = state.transactions
        (5 downTo 0).map { all.monthStats(ym.plusMonths(-it)) }
    }
    val savingsRate = if (cur.income > 0) (cur.net / cur.income * 100).toInt() else 0
    val daysPassed = if (ym == com.ledgeryaar.domain.period.currentYearMonth()) PersianCalendarConverter.toJalali(today).day else ym.daysInMonth()
    val dailyAvg = if (daysPassed > 0) cur.expense / daysPassed else 0.0
    val topExpense = cur.expenseByCategory.entries.sortedByDescending { it.value }.take(6).map { it.key to it.value }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(strings.navDashboard, style = MaterialTheme.typography.headlineMedium)
            Text(strings.dashboardSubtitle, modifier = Modifier.padding(top = 4.dp), fontSize = 13.sp)
        }
        // تراکنش‌هایی که از پیامک/نوتیف بانک آمده و منتظر تأیید تو هستند
        val pendingCount = state.transactions.count { it.needsReview }
        if (pendingCount > 0) {
            item {
                LedgerCard(Modifier.clickable { onOpenReview() }) {
                    RowSpaceBetween {
                        Column(Modifier.weight(1f)) {
                            Text(
                                strings.t("$pendingCount تراکنش از پیامک بانک", "$pendingCount transactions from bank SMS"),
                                fontWeight = FontWeight.Bold, fontSize = 14.sp
                            )
                            Text(
                                strings.t("بزن و بگو بابت چی بوده", "Tap to say what they were for"),
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(strings.t("بررسی ←", "Review →"), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item { MonthSelector() }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(strings.monthIncome, formatAmount(cur.income) + " " + c, AppColors.LedgerGreen, Modifier.weight(1f))
                StatTile(strings.monthExpense, formatAmount(cur.expense) + " " + c, AppColors.StampRed, Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    strings.monthNet, formatAmount(cur.net) + " " + c,
                    if (cur.net >= 0) AppColors.LedgerGreen else AppColors.StampRed, Modifier.weight(1f)
                )
                StatTile(strings.netWorth, formatAmount(state.netWorth) + " " + c, modifier = Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    strings.t("نرخ پس‌انداز", "Savings rate"), "$savingsRate%",
                    if (savingsRate >= 10) AppColors.LedgerGreen else AppColors.StampRed, Modifier.weight(1f)
                )
                StatTile(strings.t("میانگین خرج روزانه", "Avg daily spend"), formatAmount(dailyAvg) + " " + c, modifier = Modifier.weight(1f))
            }
        }
        // کارت هوشمند: وضعیت ماه
        item {
            LedgerCard {
                Text(strings.t("وضعیت هوشمند این ماه", "Smart status"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RowSpaceBetween {
                        Text(strings.t("قابل خرج باقی‌مانده", "Left to spend"), fontSize = 12.sp)
                        Text(
                            formatAmount(forecast.remainingSpendable) + " " + c, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            color = if (forecast.remainingSpendable >= 0) AppColors.LedgerGreen else AppColors.StampRed
                        )
                    }
                    if (forecast.daysLeft > 0) {
                        RowSpaceBetween {
                            Text(strings.t("سقف خرج امروز", "Safe to spend per day"), fontSize = 12.sp)
                            Text(formatAmount(forecast.dailyAllowance) + " " + c, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    RowSpaceBetween {
                        Text(strings.t("پیش‌بینی مانده‌ی آخر ماه", "Projected month-end"), fontSize = 12.sp)
                        Text(
                            formatAmount(forecast.projectedLeftover) + " " + c, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            color = if (forecast.projectedLeftover >= 0) AppColors.LedgerGreen else AppColors.StampRed
                        )
                    }
                    RowSpaceBetween {
                        Text(strings.t("اقساط و بدهی سررسید این ماه", "Installments & debts due"), fontSize = 12.sp)
                        Text(formatAmount(forecast.commitments) + " " + c, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppColors.StampRed)
                    }
                }
            }
        }
        // روند شش‌ماهه
        item {
            LedgerCard {
                Text(strings.t("روند ۶ ماه اخیر", "Last 6 months"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("■ " + strings.t("درآمد", "Income"), fontSize = 11.sp, color = AppColors.LedgerGreen)
                    Text("■ " + strings.t("هزینه", "Expense"), fontSize = 11.sp, color = AppColors.StampRed)
                }
                Spacer(Modifier.height(8.dp))
                DualBarChart(
                    labels = trend.map { it.ym.monthName(strings.isFa) },
                    income = trend.map { it.income },
                    expense = trend.map { it.expense }
                )
            }
        }
        // هزینه‌ها بر اساس دسته
        item {
            LedgerCard {
                Text(strings.t("هزینه‌ها بر اساس دسته", "Spending by category"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                if (topExpense.isEmpty()) EmptyState(strings.noExpensesYet)
                else DonutWithLegend(topExpense.map { categoryName(it.first) to it.second }, c)
            }
        }
        // بدهی/طلب/وام
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(strings.totalActiveDebt, formatAmount(state.activeDebtTotal) + " " + c, AppColors.StampRed, Modifier.weight(1f))
                StatTile(strings.totalActiveReceivable, formatAmount(state.activeReceivableTotal) + " " + c, AppColors.LedgerGreen, Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(strings.t("اقساط باقی‌مانده", "Remaining installments"), formatAmount(state.remainingInstallmentsTotal) + " " + c, AppColors.StampRed, Modifier.weight(1f))
                StatTile(strings.t("موجودی بانک‌ها", "Bank balance"), formatAmount(state.bankTotal) + " " + c, modifier = Modifier.weight(1f))
            }
        }
        // اهداف
        if (state.goals.isNotEmpty()) {
            item {
                LedgerCard {
                    Text(strings.navGoals, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.goals.take(4).forEach { g ->
                            ShareBar(
                                g.title, g.current, g.target,
                                formatAmount(g.current) + " / " + formatAmount(g.target), AppColors.Bronze
                            )
                        }
                    }
                }
            }
        }
        // سررسیدها
        item {
            LedgerCard {
                Text(strings.upcomingDues, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
                val dues = state.upcomingDues(loanLabel = { id -> state.loanTitle(id) }, debtLabel = { name -> name })
                if (dues.isEmpty()) {
                    EmptyState(strings.noUpcomingDues)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        dues.forEach { due ->
                            RowSpaceBetween {
                                Column(Modifier.weight(1f)) {
                                    Text(due.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text(PersianCalendarConverter.toJalali(due.date).displayFull, fontSize = 11.sp)
                                }
                                Text(formatAmount(due.amount) + " " + c, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
        // آخرین تراکنش‌های ماه
        item {
            LedgerCard {
                Text(strings.recentTransactions, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
                val recent = state.transactions.filter { ym.contains(it.date) }.sortedByDescending { it.date }.take(8)
                if (recent.isEmpty()) {
                    EmptyState(strings.noTransactionsYet)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        recent.forEach { tx ->
                            RowSpaceBetween {
                                Column(Modifier.weight(1f)) {
                                    Text(categoryName(tx.category), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        PersianCalendarConverter.toJalali(tx.date).displayFull + (if (tx.note.isNotBlank()) " · ${tx.note}" else ""),
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    (if (tx.type == TxType.INCOME) "+" else "-") + formatAmount(tx.amount) + " " + c,
                                    fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                    color = if (tx.type == TxType.INCOME) AppColors.LedgerGreen else AppColors.StampRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
