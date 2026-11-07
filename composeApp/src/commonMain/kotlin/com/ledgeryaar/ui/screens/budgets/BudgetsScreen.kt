package com.ledgeryaar.ui.screens.budgets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledgeryaar.domain.analytics.Forecast
import com.ledgeryaar.domain.analytics.Tip
import com.ledgeryaar.domain.analytics.TipKind
import com.ledgeryaar.domain.analytics.buildForecast
import com.ledgeryaar.domain.analytics.monthStats
import com.ledgeryaar.domain.model.Budget
import com.ledgeryaar.domain.model.TxType
import com.ledgeryaar.domain.model.categoryKeys
import com.ledgeryaar.domain.period.PeriodHolder
import com.ledgeryaar.domain.period.todayDate
import com.ledgeryaar.i18n.AppStrings
import com.ledgeryaar.i18n.LocalStrings
import com.ledgeryaar.i18n.t
import com.ledgeryaar.ui.LedgerViewModel
import com.ledgeryaar.ui.LocalCustomCategories
import com.ledgeryaar.ui.LocalInputUnit
import com.ledgeryaar.ui.categoryName
import com.ledgeryaar.ui.components.*
import com.ledgeryaar.ui.theme.AppColors
import com.ledgeryaar.util.formatAmount
import com.ledgeryaar.util.parseAmount
import com.ledgeryaar.util.plainNumber
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BudgetsScreen(viewModel: LedgerViewModel = koinViewModel()) {
    val strings = LocalStrings.current
    val holder: PeriodHolder = koinInject()
    val period by holder.state.collectAsState()
    val state by viewModel.state.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Budget?>(null) }

    val forecast = remember(state, period.ym) { buildForecast(state, period.ym, todayDate()) }
    val spentByCategory = remember(state, period.ym) { state.transactions.monthStats(period.ym).expenseByCategory }

    Scaffold(floatingActionButton = {
        FloatingActionButton(onClick = { editing = null; showDialog = true }) { Icon(Icons.Filled.Add, contentDescription = strings.add) }
    }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text(strings.navBudgets, style = MaterialTheme.typography.headlineMedium) }
            item { MonthSelector() }
            item { PlanCard(forecast, strings) }
            item { TipsCard(forecast, strings) }
            if (forecast.suggestedLimits.isNotEmpty()) {
                item { SuggestionsCard(forecast, strings, onApply = { viewModel.applyBudgets(forecast.suggestedLimits) }) }
            }
            if (forecast.goalPlans.isNotEmpty()) {
                item {
                    LedgerCard {
                        Text(strings.t("برای رسیدن به اهدافت (خریدهای آینده)", "To reach your goals (planned purchases)"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            forecast.goalPlans.forEach { g ->
                                RowSpaceBetween {
                                    Column(Modifier.weight(1f)) {
                                        Text(g.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        Text(strings.t("${g.monthsLeft} ماه تا موعد", "${g.monthsLeft} months left"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(
                                        formatAmount(g.monthlyNeeded) + " " + strings.currency + strings.t(" / ماه", " / month"),
                                        fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColors.Bronze
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item { Text(strings.t("سقف هزینهٔ دسته‌ها", "Category limits"), style = MaterialTheme.typography.titleMedium) }
            if (state.budgets.isEmpty()) {
                item { EmptyState(strings.noBudgetsYet) }
            } else {
                items(state.budgets, key = { it.id }) { b ->
                    val spent = spentByCategory[b.category] ?: 0.0
                    val progress = if (b.amount > 0) (spent / b.amount).toFloat().coerceIn(0f, 1f) else 0f
                    val over = spent > b.amount
                    LedgerCard {
                        RowSpaceBetween {
                            Text(categoryName(b.category), fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            EditDeleteButtons(
                                onEdit = { editing = b; showDialog = true },
                                onDelete = { viewModel.deleteBudget(b.id) }
                            )
                        }
                        LinearProgressIndicator(
                            progress = { progress }, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            color = if (over) AppColors.StampRed else MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            "${formatAmount(spent)} / ${formatAmount(b.amount)} ${strings.currency}" + if (over) " · ${strings.overBudget}" else "",
                            fontSize = 12.sp, color = if (over) AppColors.StampRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        BudgetDialog(
            initial = editing, strings = strings,
            onDismiss = { showDialog = false },
            onCreateCategory = { name, t -> viewModel.addCategory(name, t) },
            onSave = { category, amount ->
                val old = editing
                if (old != null && old.category != category) viewModel.deleteBudget(old.id)
                viewModel.saveBudget(category, amount)
                showDialog = false
            }
        )
    }
}

@Composable
private fun PlanCard(f: Forecast, strings: AppStrings) {
    val c = strings.currency
    LedgerCard {
        Text(strings.t("برنامهٔ مالی این ماه", "This month's plan"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(
            strings.t("پیش‌بینی آماری روی خود گوشی؛ هرچه داده‌ی بیشتری وارد کنی دقیق‌تر می‌شود.",
                "On-device statistical forecast; it gets more accurate as you add data."),
            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp)
        )
        Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            PlanRow(strings.t("درآمد مورد انتظار", "Expected income"), f.expectedIncome, c, AppColors.LedgerGreen)
            PlanRow(strings.t("− اقساط و بدهی‌های سررسید", "− Installments & debts due"), f.commitments, c, AppColors.StampRed)
            PlanRow(strings.t("− پس‌انداز لازم برای اهداف", "− Savings for goals"), f.goalSavings, c, AppColors.Bronze)
            HorizontalDivider()
            PlanRow(
                strings.t("= قابل خرج در کل ماه", "= Spendable this month"), f.spendable, c,
                if (f.spendable >= 0) AppColors.LedgerGreen else AppColors.StampRed, bold = true
            )
            PlanRow(strings.t("تا الان خرج شده", "Spent so far"), f.expenseSoFar, c, MaterialTheme.colorScheme.onSurface)
            PlanRow(
                strings.t("باقی‌مانده برای خرج", "Left to spend"), f.remainingSpendable, c,
                if (f.remainingSpendable >= 0) AppColors.LedgerGreen else AppColors.StampRed, bold = true
            )
        }
        if (f.daysLeft > 0) {
            Text(
                strings.t(
                    "سقف مجاز خرج روزانه: ${formatAmount(f.dailyAllowance)} $c  (${f.daysLeft} روز مانده)",
                    "Safe daily spending: ${formatAmount(f.dailyAllowance)} $c  (${f.daysLeft} days left)"
                ),
                fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AppColors.LedgerGreen, modifier = Modifier.padding(top = 10.dp)
            )
        }
        Text(
            strings.t(
                "پیش‌بینی مانده‌ی آخر ماه: ${formatAmount(f.projectedLeftover)} $c",
                "Projected month-end balance: ${formatAmount(f.projectedLeftover)} $c"
            ),
            fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            color = if (f.projectedLeftover >= 0) AppColors.LedgerGreen else AppColors.StampRed,
            modifier = Modifier.padding(top = 6.dp)
        )
        if (f.receivablesDue > 0) {
            Text(
                strings.t("طلب‌های سررسید این ماه (در محاسبه نیامده): ${formatAmount(f.receivablesDue)} $c",
                    "Receivables due (not counted): ${formatAmount(f.receivablesDue)} $c"),
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(strings.t("اطمینان پیش‌بینی", "Forecast confidence") + " ${f.confidence}%", fontSize = 11.sp, modifier = Modifier.padding(end = 8.dp))
            LinearProgressIndicator(progress = { f.confidence / 100f }, modifier = Modifier.weight(1f), color = AppColors.Bronze)
        }
        Text(
            strings.t("بر اساس ${f.monthsOfHistory} ماه سابقه", "Based on ${f.monthsOfHistory} months of history"),
            fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PlanRow(label: String, value: Double, currency: String, color: androidx.compose.ui.graphics.Color, bold: Boolean = false) {
    RowSpaceBetween {
        Text(label, fontSize = 12.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(
            formatAmount(value) + " " + currency, fontSize = 12.sp, color = color,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}

@Composable
private fun TipsCard(f: Forecast, strings: AppStrings) {
    LedgerCard {
        Text(strings.t("توصیه‌ها", "Advice"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            f.tips.forEach { tip ->
                val label = if (tip.label.isNotEmpty()) categoryName(tip.label) else ""
                Text("• " + tipText(strings, tip, label), fontSize = 12.sp)
            }
        }
    }
}

private fun tipText(s: AppStrings, tip: Tip, label: String): String {
    val amt = formatAmount(tip.amount) + " " + s.currency
    return when (tip.kind) {
        TipKind.NO_DATA -> s.t("هنوز داده‌ای ثبت نشده. چند درآمد و هزینه وارد کن تا برنامه‌ریزی شروع شود.",
            "No data yet. Add some income and expenses to start planning.")
        TipKind.LOW_DATA -> s.t("سابقهٔ کمی داری؛ پیش‌بینی با ثبت ماه‌های بیشتر دقیق‌تر می‌شود.",
            "Little history so far; the forecast improves with more months.")
        TipKind.DEFICIT -> s.t("با روند فعلی آخر ماه حدود $amt کسری داری. هزینه‌های غیرضروری را کم کن یا درآمد اضافه‌ای در نظر بگیر.",
            "At this pace you end the month about $amt short. Trim non-essentials or add income.")
        TipKind.LOW_SAVINGS -> s.t("نرخ پس‌انداز این ماه حدود ${tip.percent}٪ است؛ هدف خوب حداقل ۱۰ تا ۲۰٪ است.",
            "Your savings rate is about ${tip.percent}%; aim for at least 10–20%.")
        TipKind.TOP_CATEGORY -> s.t("«$label» ${tip.percent}٪ از هزینه‌هایت را می‌بلعد ($amt). اینجا بیشترین جای صرفه‌جویی است.",
            "“$label” takes ${tip.percent}% of your spending ($amt) — the best place to cut.")
        TipKind.HEAVY_COMMITMENTS -> s.t("اقساط و بدهی‌های این ماه ${tip.percent}٪ درآمدت را می‌برد؛ از وام تازه پرهیز کن.",
            "Installments and debts take ${tip.percent}% of income; avoid new loans.")
        TipKind.GOAL_PRESSURE -> s.t("برای رسیدن به اهدافت این ماه باید حدود $amt کنار بگذاری.",
            "To stay on track with your goals, set aside about $amt this month.")
        TipKind.OVERSPEND_PACE -> s.t("با سرعت خرج فعلی، حدود $amt بیشتر از مبلغ قابل خرج می‌روی. خرج روزانه را کم کن.",
            "At the current pace you will overspend by about $amt. Slow down daily spending.")
        TipKind.OVERDUE -> s.t("اقساط/بدهی‌های عقب‌افتاده به مبلغ $amt داری؛ اول آن‌ها را تسویه کن.",
            "You have overdue payments of $amt; settle them first.")
        TipKind.ON_TRACK -> s.t("مسیرت خوب است. با همین روند ادامه بده.", "You're on track. Keep it up.")
    }
}

@Composable
private fun SuggestionsCard(f: Forecast, strings: AppStrings, onApply: () -> Unit) {
    LedgerCard {
        Text(strings.t("سقف پیشنهادی هر دسته", "Suggested category limits"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(
            strings.t("بر اساس میانگین ماه‌های قبل و مبلغ قابل خرج این ماه.", "Based on your past months and what you can spend."),
            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            f.suggestedLimits.forEach { (cat, amount) ->
                RowSpaceBetween {
                    Text(categoryName(cat), fontSize = 12.sp)
                    Text(formatAmount(amount) + " " + strings.currency, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Button(onClick = onApply, modifier = Modifier.padding(top = 10.dp)) {
            Text(strings.t("اعمال این سقف‌ها", "Apply these limits"))
        }
    }
}

@Composable
private fun BudgetDialog(
    initial: Budget?,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onCreateCategory: (String, TxType) -> Unit,
    onSave: (String, Double) -> Unit
) {
    val custom = LocalCustomCategories.current
    var category by remember { mutableStateOf(initial?.category ?: categoryKeys(TxType.EXPENSE, custom).first()) }
    var amount by remember { mutableStateOf(initial?.let { plainNumber(it.amount) } ?: "") }
    var unit by remember { mutableStateOf(if (initial != null) 1L else 0L) }
    val effectiveUnit = if (unit == 0L) LocalInputUnit.current else unit
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial != null) strings.t("ویرایش سقف", "Edit limit") else strings.addBudget) },
        text = {
            FormColumn {
                CategoryField(TxType.EXPENSE, category, { category = it }, onCreateCategory)
                AmountField(amount, { amount = it }, effectiveUnit, { unit = it }, strings.budgetLimitLabel)
                error?.let { Text(it, color = AppColors.StampRed, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = parseAmount(amount, effectiveUnit)
                if (a == null || a <= 0) error = strings.invalidAmount else onSave(category, a)
            }) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}
