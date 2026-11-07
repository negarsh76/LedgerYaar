package com.ledgeryaar.ui.screens.review

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledgeryaar.data.repository.LedgerRepository
import com.ledgeryaar.domain.autoimport.AutoImporter
import com.ledgeryaar.domain.model.PersianCalendarConverter
import com.ledgeryaar.domain.model.Transaction
import com.ledgeryaar.domain.model.TxType
import com.ledgeryaar.domain.model.categoryKeys
import com.ledgeryaar.i18n.LocalStrings
import com.ledgeryaar.i18n.t
import com.ledgeryaar.platform.PlatformBridge
import com.ledgeryaar.ui.LocalCustomCategories
import com.ledgeryaar.ui.categoryName
import com.ledgeryaar.ui.components.EmptyState
import com.ledgeryaar.ui.components.LedgerCard
import com.ledgeryaar.ui.components.RowSpaceBetween
import com.ledgeryaar.ui.theme.AppColors
import com.ledgeryaar.util.formatAmount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

class ReviewViewModel(
    private val repo: LedgerRepository,
    private val importer: AutoImporter,
    private val platform: PlatformBridge
) : ViewModel() {
    val pending: StateFlow<List<Transaction>> = repo.transactions()
        .map { list -> list.filter { it.needsReview }.sortedByDescending { it.sourceTime } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scanMessage = MutableStateFlow<String?>(null)
    val permissions = platform.permissions

    fun confirm(id: Long, category: String?, note: String?) {
        viewModelScope.launch { importer.confirm(id, category, note?.takeIf { it.isNotBlank() }) }
    }
    fun confirmAll() { viewModelScope.launch { importer.confirmAll() } }
    fun dismiss(id: Long) { viewModelScope.launch { repo.deleteTransaction(id) } }
    fun scan(days: Int, foundText: (Int) -> String) {
        viewModelScope.launch {
            val n = platform.scanSmsInbox(days)
            scanMessage.value = foundText(n)
        }
    }
    fun requestSms() = platform.requestSmsPermission()
}

/**
 * صفحهٔ «بررسی خودکارها»: تراکنش‌هایی که از پیامک/نوتیف بانک آمده‌اند.
 * برای هر کدام فقط دسته را با یک لمس انتخاب کن و اگر خواستی بنویس بابت چی بوده.
 */
@Composable
fun ReviewScreen(viewModel: ReviewViewModel = koinViewModel()) {
    val strings = LocalStrings.current
    val pending by viewModel.pending.collectAsState()
    val scanMsg by viewModel.scanMessage.collectAsState()
    val perms by viewModel.permissions.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(strings.t("بررسی تراکنش‌های خودکار", "Review auto-imported"), style = MaterialTheme.typography.headlineMedium)
            Text(
                strings.t(
                    "این‌ها را اپ خودش از پیامک یا نوتیف بانک برداشته. فقط بگو بابت چی بوده؛ دفعه‌های بعد خودش یاد می‌گیرد.",
                    "These were picked up from bank SMS / notifications. Just say what each was for — the app learns for next time."
                ),
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)
            )
        }
        if (perms.smsSupported) {
            item {
                LedgerCard {
                    Text(strings.t("خواندن پیامک‌های قبلی", "Import past SMS"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    if (!perms.smsGranted) {
                        Text(strings.t("اول اجازهٔ خواندن پیامک را بده.", "Grant SMS permission first."), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        Button(onClick = { viewModel.requestSms() }, modifier = Modifier.padding(top = 8.dp)) {
                            Text(strings.t("اجازه دادن", "Allow"))
                        }
                    } else {
                        Row(Modifier.padding(top = 8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(7, 30, 90).forEach { d ->
                                OutlinedButton(onClick = {
                                    viewModel.scan(d) { n -> strings.t("$n تراکنش جدید پیدا شد", "$n new transactions found") }
                                }) { Text(strings.t("$d روز اخیر", "Last $d days")) }
                            }
                        }
                    }
                    scanMsg?.let { Text(it, fontSize = 12.sp, color = AppColors.LedgerGreen, modifier = Modifier.padding(top = 6.dp)) }
                }
            }
        }
        if (pending.isEmpty()) {
            item { EmptyState(strings.t("همه چیز بررسی شده ✓", "All caught up ✓")) }
        } else {
            item {
                RowSpaceBetween {
                    Text(strings.t("${pending.size} مورد در انتظار", "${pending.size} pending"), fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { viewModel.confirmAll() }) { Text(strings.t("تأیید همه", "Confirm all")) }
                }
            }
            items(pending, key = { it.id }) { tx ->
                PendingCard(
                    tx = tx,
                    onConfirm = { cat, note -> viewModel.confirm(tx.id, cat, note) },
                    onDismiss = { viewModel.dismiss(tx.id) }
                )
            }
        }
    }
}

@Composable
private fun PendingCard(tx: Transaction, onConfirm: (String?, String?) -> Unit, onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    val custom = LocalCustomCategories.current
    var category by remember(tx.id) { mutableStateOf(tx.category) }
    var note by remember(tx.id) { mutableStateOf("") }
    var showRaw by remember(tx.id) { mutableStateOf(false) }
    val color = if (tx.type == TxType.INCOME) AppColors.LedgerGreen else AppColors.StampRed

    LedgerCard {
        RowSpaceBetween {
            Column(Modifier.weight(1f)) {
                Text(
                    (if (tx.type == TxType.INCOME) "+ " else "− ") + formatAmount(tx.amount) + " " + strings.currency,
                    fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color
                )
                Text(
                    PersianCalendarConverter.toJalali(tx.date).displayFull +
                        (if (tx.note.isNotBlank()) " · ${tx.note}" else "") +
                        " · " + if (tx.source == "sms") strings.t("پیامک", "SMS") else strings.t("نوتیف", "Notification"),
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = { showRaw = !showRaw }) { Text(strings.t("متن", "Text"), fontSize = 12.sp) }
        }
        if (showRaw && tx.rawText != null) {
            Text(tx.rawText, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 8, overflow = TextOverflow.Ellipsis)
        }
        Row(
            Modifier.padding(top = 8.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categoryKeys(tx.type, custom).forEach { key ->
                FilterChip(
                    selected = category == key,
                    onClick = { category = key },
                    label = { Text(categoryName(key), fontSize = 12.sp) }
                )
            }
        }
        OutlinedTextField(
            value = note, onValueChange = { note = it }, singleLine = true,
            label = { Text(strings.t("بابت چی بود؟ (اختیاری)", "What was it for? (optional)")) },
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
        )
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { onConfirm(category, note) }, modifier = Modifier.weight(1f)) {
                Text(strings.t("تأیید", "Confirm"))
            }
            OutlinedButton(onClick = onDismiss) {
                Text(strings.t("تراکنش نیست، حذف", "Not a transaction"), fontSize = 12.sp)
            }
        }
    }
}
