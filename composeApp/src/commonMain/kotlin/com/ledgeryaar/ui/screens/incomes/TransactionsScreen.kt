package com.ledgeryaar.ui.screens.incomes

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
import com.ledgeryaar.domain.model.PersianCalendarConverter
import com.ledgeryaar.domain.model.PersianDate
import com.ledgeryaar.domain.model.Transaction
import com.ledgeryaar.domain.model.TxType
import com.ledgeryaar.domain.model.categoryKeys
import com.ledgeryaar.domain.period.PeriodHolder
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
import com.ledgeryaar.util.parseJalaliInput
import com.ledgeryaar.util.plainNumber
import kotlinx.datetime.minus
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/** صفحهٔ مشترک درآمدها و هزینه‌ها — فقط نوع تراکنش فرق دارد */
@Composable
fun TransactionsScreen(type: TxType, viewModel: LedgerViewModel = koinViewModel()) {
    val strings = LocalStrings.current
    val holder: PeriodHolder = koinInject()
    val period by holder.state.collectAsState()
    val state by viewModel.state.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Transaction?>(null) }

    val all = state.transactions.filter { it.type == type }
    val visible = (if (period.showAll) all else all.filter { period.ym.contains(it.date) }).sortedByDescending { it.date }
    val total = visible.map { it.amount }.sum()
    val byCategory = visible.groupBy { it.category }
        .map { entry -> entry.key to entry.value.map { it.amount }.sum() }
        .sortedByDescending { it.second }
    val color = if (type == TxType.INCOME) AppColors.LedgerGreen else AppColors.StampRed
    val title = if (type == TxType.INCOME) strings.navIncomes else strings.navExpenses

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showDialog = true }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Filled.Add, contentDescription = strings.add)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text(title, style = MaterialTheme.typography.headlineMedium) }
            item { MonthSelector(allowAll = true) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(
                        label = if (type == TxType.INCOME) strings.monthIncome else strings.monthExpense,
                        value = formatAmount(total) + " " + strings.currency,
                        valueColor = color, modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = strings.t("تعداد", "Count"),
                        value = visible.size.toString(), modifier = Modifier.weight(1f)
                    )
                }
            }
            if (byCategory.size > 1) {
                item {
                    LedgerCard {
                        Text(strings.t("سهم هر دسته", "By category"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val max = byCategory.first().second
                            byCategory.take(6).forEach { (cat, sum) ->
                                ShareBar(categoryName(cat), sum, max, formatAmount(sum) + " " + strings.currency, color)
                            }
                        }
                    }
                }
            }
            if (visible.isEmpty()) {
                item { EmptyState(if (type == TxType.INCOME) strings.noIncomesYet else strings.noExpensesYet) }
            } else {
                items(visible, key = { it.id }) { tx ->
                    LedgerCard {
                        RowSpaceBetween {
                            Column(Modifier.weight(1f)) {
                                Text(categoryName(tx.category), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(
                                    PersianCalendarConverter.toJalali(tx.date).displayFull + (if (tx.note.isNotBlank()) " · ${tx.note}" else ""),
                                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(formatAmount(tx.amount) + " " + strings.currency, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = color)
                                EditDeleteButtons(
                                    onEdit = { editing = tx; showDialog = true },
                                    onDelete = { viewModel.deleteTransaction(tx.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        val defaultDate = if (period.ym == com.ledgeryaar.domain.period.currentYearMonth())
            PersianCalendarConverter.getCurrentPersianDate().toInputString()
        else PersianDate(period.ym.year, period.ym.month, 1).toInputString()
        TransactionDialog(
            initial = editing, type = type, strings = strings, defaultDate = defaultDate,
            onDismiss = { showDialog = false },
            onCreateCategory = { name, t -> viewModel.addCategory(name, t) },
            onSave = { tx -> viewModel.saveTransaction(tx); showDialog = false }
        )
    }
}

@Composable
private fun TransactionDialog(
    initial: Transaction?,
    type: TxType,
    strings: AppStrings,
    defaultDate: String,
    onDismiss: () -> Unit,
    onCreateCategory: (String, TxType) -> Unit,
    onSave: (Transaction) -> Unit
) {
    val custom = LocalCustomCategories.current
    var amountText by remember { mutableStateOf(initial?.let { plainNumber(it.amount) } ?: "") }
    var unit by remember { mutableStateOf(if (initial != null) 1L else 0L) }
    val defaultUnit = LocalInputUnit.current
    val effectiveUnit = if (unit == 0L) defaultUnit else unit
    var dateText by remember { mutableStateOf(initial?.let { PersianCalendarConverter.toJalali(it.date).toInputString() } ?: defaultDate) }
    var category by remember { mutableStateOf(initial?.category ?: categoryKeys(type, custom).first()) }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial != null) strings.t("ویرایش", "Edit")
                else if (type == TxType.INCOME) strings.addIncome else strings.addExpense
            )
        },
        text = {
            FormColumn {
                AmountField(amountText, { amountText = it }, effectiveUnit, { unit = it }, strings.amountLabel)
                OutlinedTextField(
                    value = dateText, onValueChange = { dateText = it }, singleLine = true,
                    label = { Text(strings.dateHint) }, modifier = Modifier.fillMaxWidth()
                )
                // میان‌بر تاریخ: لازم نیست تاریخ را تایپ کنی
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val today = com.ledgeryaar.domain.period.todayDate()
                    listOf(0 to strings.t("امروز", "Today"), 1 to strings.t("دیروز", "Yesterday"), 2 to strings.t("پریروز", "2 days ago")).forEach { (back, label) ->
                        val d = PersianCalendarConverter.toJalali(today.minus(back, kotlinx.datetime.DateTimeUnit.DAY)).toInputString()
                        FilterChip(selected = dateText == d, onClick = { dateText = d }, label = { Text(label, fontSize = 12.sp) })
                    }
                }
                CategoryField(type, category, { category = it }, onCreateCategory)
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text(strings.noteLabel) }, modifier = Modifier.fillMaxWidth()
                )
                error?.let { Text(it, color = AppColors.StampRed, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = parseAmount(amountText, effectiveUnit)
                val persian = parseJalaliInput(dateText)
                when {
                    amount == null || amount <= 0 -> error = strings.invalidAmount
                    persian == null -> error = strings.invalidDate
                    else -> onSave(
                        (initial ?: Transaction(type = type, amount = amount, date = PersianCalendarConverter.toGregorian(persian), category = category)).copy(
                            type = type, amount = amount,
                            date = PersianCalendarConverter.toGregorian(persian),
                            category = category, note = note, needsReview = false
                        )
                    )
                }
            }) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}
