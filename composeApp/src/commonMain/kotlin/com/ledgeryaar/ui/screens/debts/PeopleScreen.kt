package com.ledgeryaar.ui.screens.debts

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
import com.ledgeryaar.domain.model.Person
import com.ledgeryaar.domain.model.PersonType
import com.ledgeryaar.domain.model.PersianCalendarConverter
import com.ledgeryaar.domain.period.PeriodHolder
import com.ledgeryaar.domain.period.toYearMonth
import com.ledgeryaar.domain.period.todayDate
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
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/** بدهی‌ها و طلب‌ها: موارد تسویه‌نشده همیشه دیده می‌شوند؛ تسویه‌شده‌ها فقط در ماهِ خودشان (یا با «نمایش همه») */
@Composable
fun PeopleScreen(type: PersonType, viewModel: LedgerViewModel = koinViewModel()) {
    val strings = LocalStrings.current
    val holder: PeriodHolder = koinInject()
    val period by holder.state.collectAsState()
    val state by viewModel.state.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Person?>(null) }

    val all = state.people.filter { it.type == type }
    val today = todayDate()
    fun anchorMonth(p: Person) = (p.dueDate
        ?: Instant.fromEpochMilliseconds(p.createdAt).toLocalDateTime(TimeZone.currentSystemDefault()).date).toYearMonth()
    val visible = all
        .filter { period.showAll || !it.paid || anchorMonth(it) == period.ym }
        .sortedWith(compareBy<Person> { it.paid }.thenBy { it.dueDate ?: LocalDate(2999, 1, 1) })
    val total = all.filter { !it.paid }.map { it.amount }.sum()
    val dueInMonth = all.filter { p ->
        val d = p.dueDate
        !p.paid && d != null && period.ym.contains(d)
    }.map { it.amount }.sum()

    val color = if (type == PersonType.DEBT) AppColors.StampRed else AppColors.LedgerGreen
    val title = if (type == PersonType.DEBT) strings.navDebts else strings.navReceivables
    val addLabel = if (type == PersonType.DEBT) strings.addDebtParty else strings.addReceivableParty

    Scaffold(floatingActionButton = {
        FloatingActionButton(onClick = { editing = null; showDialog = true }) { Icon(Icons.Filled.Add, contentDescription = strings.add) }
    }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text(title, style = MaterialTheme.typography.headlineMedium) }
            item { MonthSelector(allowAll = true) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(
                        if (type == PersonType.DEBT) strings.totalActiveDebt else strings.totalActiveReceivable,
                        formatAmount(total) + " " + strings.currency, color, Modifier.weight(1f)
                    )
                    StatTile(
                        strings.t("سررسید این ماه", "Due this month"),
                        formatAmount(dueInMonth) + " " + strings.currency, color, Modifier.weight(1f)
                    )
                }
            }
            if (visible.isEmpty()) {
                item { EmptyState(strings.noItemsYet) }
            } else {
                items(visible, key = { it.id }) { person ->
                    val d = person.dueDate
                    val overdue = !person.paid && d != null && d < today
                    LedgerCard {
                        RowSpaceBetween {
                            Column(Modifier.weight(1f)) {
                                Text(person.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(
                                    (d?.let { PersianCalendarConverter.toJalali(it).displayFull } ?: "—") +
                                        if (person.paid) " · ${strings.settled}" else if (overdue) " · ${strings.overdue}" else "",
                                    fontSize = 11.sp, color = if (overdue) AppColors.StampRed else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(formatAmount(person.amount) + " " + strings.currency, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = color)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedButton(onClick = { viewModel.togglePersonPaid(person) }) {
                                        Text(if (person.paid) strings.reopen else strings.settle, fontSize = 11.sp)
                                    }
                                    EditDeleteButtons(
                                        onEdit = { editing = person; showDialog = true },
                                        onDelete = { viewModel.deletePerson(person.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        PersonDialog(
            initial = editing, type = type, addLabel = addLabel, strings = strings,
            onDismiss = { showDialog = false },
            onSave = { p -> viewModel.savePerson(p); showDialog = false }
        )
    }
}

@Composable
private fun PersonDialog(initial: Person?, type: PersonType, addLabel: String, strings: AppStrings, onDismiss: () -> Unit, onSave: (Person) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var amount by remember { mutableStateOf(initial?.let { plainNumber(it.amount) } ?: "") }
    var unit by remember { mutableStateOf(if (initial != null) 1L else 0L) }
    val effectiveUnit = if (unit == 0L) LocalInputUnit.current else unit
    var dueText by remember {
        mutableStateOf(initial?.dueDate?.let { PersianCalendarConverter.toJalali(it).toInputString() } ?: "")
    }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial != null) strings.t("ویرایش", "Edit") else addLabel) },
        text = {
            FormColumn {
                OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text(strings.personNameLabel) }, modifier = Modifier.fillMaxWidth())
                AmountField(amount, { amount = it }, effectiveUnit, { unit = it }, strings.amountLabel)
                OutlinedTextField(
                    value = dueText, onValueChange = { dueText = it }, singleLine = true,
                    label = { Text(strings.dueDateOptionalLabel + " (" + strings.dateHint + ")") }, modifier = Modifier.fillMaxWidth()
                )
                error?.let { Text(it, color = AppColors.StampRed, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amt = parseAmount(amount, effectiveUnit)
                val duePersian = if (dueText.isBlank()) null else parseJalaliInput(dueText)
                when {
                    name.isBlank() -> error = strings.invalidName
                    amt == null || amt <= 0 -> error = strings.invalidAmount
                    dueText.isNotBlank() && duePersian == null -> error = strings.invalidDate
                    else -> onSave(
                        Person(
                            id = initial?.id ?: 0L, type = type, name = name.trim(), amount = amt,
                            dueDate = duePersian?.let { PersianCalendarConverter.toGregorian(it) },
                            paid = initial?.paid ?: false, createdAt = initial?.createdAt ?: 0L
                        )
                    )
                }
            }) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}
