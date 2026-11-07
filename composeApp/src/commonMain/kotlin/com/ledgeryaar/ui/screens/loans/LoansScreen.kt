package com.ledgeryaar.ui.screens.loans

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledgeryaar.domain.model.Loan
import com.ledgeryaar.domain.model.PersianCalendarConverter
import com.ledgeryaar.domain.period.PeriodHolder
import com.ledgeryaar.i18n.AppStrings
import com.ledgeryaar.i18n.LocalStrings
import com.ledgeryaar.i18n.t
import com.ledgeryaar.ui.LedgerViewModel
import com.ledgeryaar.ui.LocalInputUnit
import com.ledgeryaar.ui.components.*
import com.ledgeryaar.ui.theme.AppColors
import com.ledgeryaar.util.formatAmount
import com.ledgeryaar.util.parseAmount
import com.ledgeryaar.util.parseJalaliInput
import com.ledgeryaar.util.plainNumber
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoansScreen(viewModel: LedgerViewModel = koinViewModel()) {
    val strings = LocalStrings.current
    val holder: PeriodHolder = koinInject()
    val period by holder.state.collectAsState()
    val state by viewModel.state.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Loan?>(null) }

    val dueThisMonth = state.installments.filter { period.ym.contains(it.dueDate) }.sortedBy { it.dueDate }
    val dueTotal = dueThisMonth.map { it.amount }.sum()
    val duePaid = dueThisMonth.filter { it.paid }.map { it.amount }.sum()

    Scaffold(floatingActionButton = {
        FloatingActionButton(onClick = { editing = null; showDialog = true }) { Icon(Icons.Filled.Add, contentDescription = strings.add) }
    }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text(strings.navLoans, style = MaterialTheme.typography.headlineMedium) }
            item { MonthSelector() }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(strings.t("اقساط این ماه", "Due this month"), formatAmount(dueTotal) + " " + strings.currency, AppColors.StampRed, Modifier.weight(1f))
                    StatTile(strings.t("پرداخت‌شده", "Paid"), formatAmount(duePaid) + " " + strings.currency, AppColors.LedgerGreen, Modifier.weight(1f))
                }
            }
            if (dueThisMonth.isNotEmpty()) {
                item {
                    LedgerCard {
                        Text(strings.t("اقساط سررسید این ماه", "Installments due this month"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Column(Modifier.padding(top = 6.dp)) {
                            dueThisMonth.forEach { inst ->
                                RowSpaceBetween {
                                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                        Checkbox(checked = inst.paid, onCheckedChange = { viewModel.toggleInstallment(inst) })
                                        Column {
                                            Text(state.loanTitle(inst.loanId) + " — " + inst.number, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            Text(PersianCalendarConverter.toJalali(inst.dueDate).displayFull, fontSize = 10.sp)
                                        }
                                    }
                                    Text(formatAmount(inst.amount) + " " + strings.currency, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            if (state.loans.isEmpty()) {
                item { EmptyState(strings.noLoansYet) }
            } else {
                items(state.loans, key = { it.id }) { loan ->
                    val list = state.installments.filter { it.loanId == loan.id }.sortedBy { it.number }
                    val paidCount = list.count { it.paid }
                    val remaining = list.filter { !it.paid }.map { it.amount }.sum()
                    val progress = if (list.isNotEmpty()) paidCount.toFloat() / list.size else 0f
                    var expanded by remember { mutableStateOf(false) }

                    LedgerCard {
                        RowSpaceBetween {
                            Column(Modifier.weight(1f)) {
                                Text(loan.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("$paidCount ${strings.installmentsPaidOf} ${list.size}", fontSize = 11.sp)
                            }
                            EditDeleteButtons(
                                onEdit = { editing = loan; showDialog = true },
                                onDelete = { viewModel.deleteLoan(loan.id) }
                            )
                        }
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                        Text(
                            "${strings.remaining}: ${formatAmount(remaining)} ${strings.currency}",
                            fontSize = 12.sp, color = AppColors.StampRed, fontWeight = FontWeight.SemiBold
                        )
                        TextButton(onClick = { expanded = !expanded }) {
                            Text(if (expanded) strings.t("بستن اقساط", "Hide installments") else strings.t("نمایش اقساط", "Show installments"))
                        }
                        if (expanded) {
                            HorizontalDivider(Modifier.padding(vertical = 4.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                list.forEach { inst ->
                                    RowSpaceBetween {
                                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                            Checkbox(checked = inst.paid, onCheckedChange = { viewModel.toggleInstallment(inst) })
                                            Column {
                                                Text("${strings.installmentLabel} ${inst.number}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                Text(PersianCalendarConverter.toJalali(inst.dueDate).displayFull, fontSize = 10.sp)
                                            }
                                        }
                                        Text(formatAmount(inst.amount) + " " + strings.currency, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        LoanDialog(
            initial = editing, strings = strings,
            onDismiss = { showDialog = false },
            onSave = { loan -> viewModel.saveLoan(loan); showDialog = false }
        )
    }
}

@Composable
private fun LoanDialog(initial: Loan?, strings: AppStrings, onDismiss: () -> Unit, onSave: (Loan) -> Unit) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var total by remember { mutableStateOf(initial?.let { plainNumber(it.totalAmount) } ?: "") }
    var unit by remember { mutableStateOf(if (initial != null) 1L else 0L) }
    val effectiveUnit = if (unit == 0L) LocalInputUnit.current else unit
    var count by remember { mutableStateOf(initial?.installmentCount?.toString() ?: "12") }
    var startText by remember {
        mutableStateOf(
            initial?.let { PersianCalendarConverter.toJalali(it.startDate).toInputString() }
                ?: PersianCalendarConverter.getCurrentPersianDate().toInputString()
        )
    }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial != null) strings.t("ویرایش وام", "Edit loan") else strings.addLoan) },
        text = {
            FormColumn {
                OutlinedTextField(value = title, onValueChange = { title = it }, singleLine = true, label = { Text(strings.loanTitleLabel) }, modifier = Modifier.fillMaxWidth())
                AmountField(total, { total = it }, effectiveUnit, { unit = it }, strings.totalAmountLabel)
                OutlinedTextField(value = count, onValueChange = { count = it }, singleLine = true, label = { Text(strings.installmentCountLabel) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = startText, onValueChange = { startText = it }, singleLine = true, label = { Text(strings.firstDueDateLabel + " (" + strings.dateHint + ")") }, modifier = Modifier.fillMaxWidth())
                if (initial != null) {
                    Text(
                        strings.t("با ویرایش، اقساط دوباره ساخته می‌شوند؛ وضعیت «پرداخت‌شده» برای قسط‌های هم‌شماره حفظ می‌شود.",
                            "Installments are regenerated; paid status is kept for matching numbers."),
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                error?.let { Text(it, color = AppColors.StampRed, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val totalAmount = parseAmount(total, effectiveUnit)
                val n = com.ledgeryaar.util.normalizeDigits(count).toIntOrNull()
                val persian = parseJalaliInput(startText)
                when {
                    title.isBlank() -> error = strings.invalidName
                    totalAmount == null || totalAmount <= 0 -> error = strings.invalidAmount
                    n == null || n <= 0 -> error = strings.invalidAmount
                    persian == null -> error = strings.invalidDate
                    else -> onSave(
                        Loan(
                            id = initial?.id ?: 0L, title = title.trim(), totalAmount = totalAmount,
                            installmentCount = n, startDate = PersianCalendarConverter.toGregorian(persian)
                        )
                    )
                }
            }) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}
