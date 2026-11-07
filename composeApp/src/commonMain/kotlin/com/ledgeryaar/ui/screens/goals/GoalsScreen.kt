package com.ledgeryaar.ui.screens.goals

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
import com.ledgeryaar.domain.model.Goal
import com.ledgeryaar.domain.model.PersianCalendarConverter
import com.ledgeryaar.domain.period.currentYearMonth
import com.ledgeryaar.domain.period.toYearMonth
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
import org.koin.compose.viewmodel.koinViewModel

/** اهداف مالی = چیزهایی که می‌خواهی بخری/جمع کنی؛ بودجه‌بندی از روی همین‌ها سهم ماهانه را حساب می‌کند */
@Composable
fun GoalsScreen(viewModel: LedgerViewModel = koinViewModel()) {
    val strings = LocalStrings.current
    val state by viewModel.state.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Goal?>(null) }
    var contributing by remember { mutableStateOf<Goal?>(null) }

    Scaffold(floatingActionButton = {
        FloatingActionButton(onClick = { editing = null; showDialog = true }) { Icon(Icons.Filled.Add, contentDescription = strings.add) }
    }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text(strings.navGoals, style = MaterialTheme.typography.headlineMedium) }
            if (state.goals.isEmpty()) {
                item { EmptyState(strings.noGoalsYet) }
            } else {
                items(state.goals, key = { it.id }) { g ->
                    val progress = if (g.target > 0) (g.current / g.target).toFloat().coerceIn(0f, 1f) else 0f
                    val remaining = (g.target - g.current).coerceAtLeast(0.0)
                    val due = g.dueDate
                    val monthsLeft = due?.let { maxOf(1, it.toYearMonth().index - currentYearMonth().index + 1) }
                    LedgerCard {
                        RowSpaceBetween {
                            Column(Modifier.weight(1f)) {
                                Text(g.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                due?.let { Text(PersianCalendarConverter.toJalali(it).displayFull, fontSize = 11.sp) }
                            }
                            EditDeleteButtons(
                                onEdit = { editing = g; showDialog = true },
                                onDelete = { viewModel.deleteGoal(g.id) }
                            )
                        }
                        LinearProgressIndicator(progress = { progress }, color = AppColors.Bronze, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
                        Text("${formatAmount(g.current)} / ${formatAmount(g.target)} ${strings.currency}  (${(progress * 100).toInt()}%)", fontSize = 12.sp)
                        if (remaining > 0 && monthsLeft != null) {
                            Text(
                                strings.t(
                                    "ماهی ${formatAmount(remaining / monthsLeft)} ${strings.currency} کنار بگذار ($monthsLeft ماه مانده)",
                                    "Save ${formatAmount(remaining / monthsLeft)} ${strings.currency}/month ($monthsLeft months left)"
                                ),
                                fontSize = 11.sp, color = AppColors.Bronze, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        } else if (remaining <= 0) {
                            Text(strings.t("به هدف رسیدی! 🎉", "Goal reached! 🎉"), fontSize = 12.sp, color = AppColors.LedgerGreen, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                        }
                        OutlinedButton(onClick = { contributing = g }, modifier = Modifier.padding(top = 8.dp)) {
                            Text(strings.t("+ افزودن پس‌انداز", "+ Add savings"), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        GoalDialog(
            initial = editing, strings = strings,
            onDismiss = { showDialog = false },
            onSave = { g -> viewModel.saveGoal(g); showDialog = false }
        )
    }
    contributing?.let { g ->
        ContributeDialog(
            strings = strings, onDismiss = { contributing = null },
            onConfirm = { add -> viewModel.saveGoal(g.copy(current = g.current + add)); contributing = null }
        )
    }
}

@Composable
private fun ContributeDialog(strings: AppStrings, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(0L) }
    val effectiveUnit = if (unit == 0L) LocalInputUnit.current else unit
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.t("افزودن پس‌انداز", "Add savings")) },
        text = {
            FormColumn {
                AmountField(amount, { amount = it }, effectiveUnit, { unit = it }, strings.amountLabel)
                error?.let { Text(it, color = AppColors.StampRed, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = parseAmount(amount, effectiveUnit)
                if (a == null || a <= 0) error = strings.invalidAmount else onConfirm(a)
            }) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
private fun GoalDialog(initial: Goal?, strings: AppStrings, onDismiss: () -> Unit, onSave: (Goal) -> Unit) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var target by remember { mutableStateOf(initial?.let { plainNumber(it.target) } ?: "") }
    var targetUnit by remember { mutableStateOf(if (initial != null) 1L else 0L) }
    val effTarget = if (targetUnit == 0L) LocalInputUnit.current else targetUnit
    var current by remember { mutableStateOf(initial?.let { plainNumber(it.current) } ?: "0") }
    var currentUnit by remember { mutableStateOf(1L) }
    var dueText by remember { mutableStateOf(initial?.dueDate?.let { PersianCalendarConverter.toJalali(it).toInputString() } ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial != null) strings.t("ویرایش هدف", "Edit goal") else strings.addGoal) },
        text = {
            FormColumn {
                OutlinedTextField(value = title, onValueChange = { title = it }, singleLine = true, label = { Text(strings.goalTitleLabel) }, modifier = Modifier.fillMaxWidth())
                AmountField(target, { target = it }, effTarget, { targetUnit = it }, strings.targetAmountLabel)
                AmountField(current, { current = it }, currentUnit, { currentUnit = it }, strings.currentAmountLabel)
                OutlinedTextField(
                    value = dueText, onValueChange = { dueText = it }, singleLine = true,
                    label = { Text(strings.dueDateOptionalLabel + " (" + strings.dateHint + ")") }, modifier = Modifier.fillMaxWidth()
                )
                error?.let { Text(it, color = AppColors.StampRed, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val t = parseAmount(target, effTarget)
                val c = parseAmount(current, currentUnit) ?: 0.0
                val duePersian = if (dueText.isBlank()) null else parseJalaliInput(dueText)
                when {
                    title.isBlank() -> error = strings.invalidName
                    t == null || t <= 0 -> error = strings.invalidAmount
                    dueText.isNotBlank() && duePersian == null -> error = strings.invalidDate
                    else -> onSave(
                        Goal(
                            id = initial?.id ?: 0L, title = title.trim(), target = t, current = c,
                            dueDate = duePersian?.let { PersianCalendarConverter.toGregorian(it) }
                        )
                    )
                }
            }) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}
