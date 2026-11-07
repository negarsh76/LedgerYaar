package com.ledgeryaar.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledgeryaar.domain.model.TxType
import com.ledgeryaar.domain.model.categoryKeys
import com.ledgeryaar.domain.period.PeriodHolder
import com.ledgeryaar.i18n.LocalStrings
import com.ledgeryaar.i18n.t
import com.ledgeryaar.ui.LocalCustomCategories
import com.ledgeryaar.ui.categoryName
import com.ledgeryaar.ui.theme.AppColors
import com.ledgeryaar.util.formatAmount
import com.ledgeryaar.util.parseAmount
import org.koin.compose.koinInject

// ────────────────────────────────────────────
// فیلد مبلغ با واحد (هزار / میلیون / میلیارد) و پیش‌نمایش عدد کامل
// ────────────────────────────────────────────

private data class UnitOption(val multiplier: Long, val fa: String, val en: String)

private val UNIT_OPTIONS = listOf(
    UnitOption(1L, "عادی", "×1"),
    UnitOption(1_000L, "هزار", "Thousand"),
    UnitOption(1_000_000L, "میلیون", "Million"),
    UnitOption(1_000_000_000L, "میلیارد", "Billion")
)

@Composable
fun AmountField(
    text: String,
    onTextChange: (String) -> Unit,
    unit: Long,
    onUnitChange: (Long) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            label = { Text(label) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            UNIT_OPTIONS.forEach { u ->
                FilterChip(
                    selected = unit == u.multiplier,
                    onClick = { onUnitChange(u.multiplier) },
                    label = { Text(strings.t(u.fa, u.en), fontSize = 12.sp) }
                )
            }
        }
        val value = parseAmount(text, unit)
        if (value != null && value > 0) {
            Text(
                "= " + formatAmount(value) + " " + strings.currency,
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AppColors.LedgerGreen
            )
        }
    }
}

// ────────────────────────────────────────────
// دکمه‌های ویرایش / حذف (حذف با تأیید)
// ────────────────────────────────────────────

@Composable
fun EditDeleteButtons(onEdit: () -> Unit, onDelete: () -> Unit) {
    val strings = LocalStrings.current
    var confirm by remember { mutableStateOf(false) }
    Row {
        IconButton(onClick = onEdit) {
            Icon(Icons.Filled.Edit, contentDescription = strings.t("ویرایش", "Edit"), tint = AppColors.LedgerGreen)
        }
        IconButton(onClick = { confirm = true }) {
            Icon(Icons.Filled.Delete, contentDescription = strings.delete, tint = AppColors.StampRed)
        }
    }
    if (confirm) {
        ConfirmDeleteDialog(onConfirm = { confirm = false; onDelete() }, onDismiss = { confirm = false })
    }
}

@Composable
fun ConfirmDeleteDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.t("حذف شود؟", "Delete this item?")) },
        text = { Text(strings.t("این کار قابل بازگشت نیست.", "This cannot be undone.")) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(strings.delete, color = AppColors.StampRed) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

// ────────────────────────────────────────────
// انتخاب دسته + ساخت دستهٔ جدید
// ────────────────────────────────────────────

@Composable
fun CategoryField(
    type: TxType,
    selected: String,
    onSelect: (String) -> Unit,
    onCreate: (String, TxType) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val custom = LocalCustomCategories.current
    val keys = categoryKeys(type, custom)
    var expanded by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }

    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = categoryName(selected), onValueChange = {}, readOnly = true,
            label = { Text(strings.categoryLabel) },
            modifier = Modifier.fillMaxWidth()
        )
        // لایهٔ شفاف برای باز شدن منو با لمس (readOnly فیلد کلیک را نمی‌گیرد)
        Box(Modifier.matchParentSize().clickable { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            keys.forEach { k ->
                DropdownMenuItem(text = { Text(categoryName(k)) }, onClick = { onSelect(k); expanded = false })
            }
            DropdownMenuItem(
                text = { Text(strings.t("+ دستهٔ جدید", "+ New category"), color = AppColors.LedgerGreen, fontWeight = FontWeight.Bold) },
                onClick = { expanded = false; showNew = true }
            )
        }
    }
    if (showNew) {
        NewCategoryDialog(type = type, onDismiss = { showNew = false }, onConfirm = { name -> onCreate(name, type); showNew = false })
    }
}

@Composable
fun NewCategoryDialog(type: TxType, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val strings = LocalStrings.current
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (type == TxType.INCOME) strings.t("دستهٔ جدید درآمد", "New income category")
                else strings.t("دستهٔ جدید هزینه", "New expense category")
            )
        },
        text = {
            OutlinedTextField(
                value = name, onValueChange = { name = it }, singleLine = true,
                label = { Text(strings.t("نام دسته", "Category name")) }, modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onConfirm(name) }) { Text(strings.save) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

// ────────────────────────────────────────────
// انتخاب ماه و سال (مشترک بین همهٔ صفحه‌ها)
// ────────────────────────────────────────────

@Composable
fun MonthSelector(allowAll: Boolean = false, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    val holder: PeriodHolder = koinInject()
    val period by holder.state.collectAsState()

    LedgerCard(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = { holder.previous() }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = strings.t("ماه قبل", "Previous month"))
            }
            TextButton(onClick = { holder.goToToday() }) {
                Text(
                    if (period.showAll && allowAll) strings.t("همهٔ زمان‌ها", "All time")
                    else period.ym.monthName(strings.isFa) + " " + period.ym.year,
                    fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = { holder.next() }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = strings.t("ماه بعد", "Next month"))
            }
        }
        if (allowAll) {
            Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !period.showAll, onClick = { holder.setShowAll(false) },
                    label = { Text(strings.t("همین ماه", "This month"), fontSize = 12.sp) }
                )
                FilterChip(
                    selected = period.showAll, onClick = { holder.setShowAll(true) },
                    label = { Text(strings.t("نمایش همه", "Show all"), fontSize = 12.sp) }
                )
            }
        }
    }
}

/** ستون فرم داخل دیالوگ — اسکرول‌پذیر تا با کیبورد و واحدها از صفحه بیرون نزند */
@Composable
fun FormColumn(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}
