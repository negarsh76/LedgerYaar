package com.ledgeryaar.ui.screens.assets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledgeryaar.domain.model.Asset
import com.ledgeryaar.domain.model.BankAccount
import com.ledgeryaar.i18n.AppStrings
import com.ledgeryaar.i18n.LocalStrings
import com.ledgeryaar.i18n.t
import com.ledgeryaar.ui.LedgerViewModel
import com.ledgeryaar.ui.LocalInputUnit
import com.ledgeryaar.ui.components.*
import com.ledgeryaar.ui.theme.AppColors
import com.ledgeryaar.util.formatAmount
import com.ledgeryaar.util.parseAmount
import com.ledgeryaar.util.plainNumber
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AssetsScreen(viewModel: LedgerViewModel = koinViewModel()) {
    val strings = LocalStrings.current
    val state by viewModel.state.collectAsState()
    var bankDialog by remember { mutableStateOf(false) }
    var editingBank by remember { mutableStateOf<BankAccount?>(null) }
    var assetDialog by remember { mutableStateOf(false) }
    var editingAsset by remember { mutableStateOf<Asset?>(null) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text(strings.navAssets, style = MaterialTheme.typography.headlineMedium) }
        item { StatTile(strings.netWorthLabel, formatAmount(state.netWorth) + " " + strings.currency, modifier = Modifier.fillMaxWidth()) }
        item {
            LedgerCard {
                RowSpaceBetween {
                    Text(strings.bankAccounts, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    TextButton(onClick = { editingBank = null; bankDialog = true }) { Text("+ ${strings.add}") }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                if (state.bankAccounts.isEmpty()) EmptyState(strings.noBankAccountsYet)
                else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.bankAccounts.forEach { b ->
                        RowSpaceBetween {
                            Column(Modifier.weight(1f)) {
                                Text(b.bank, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(b.title, fontSize = 11.sp)
                                Text(formatAmount(b.balance) + " " + strings.currency, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            EditDeleteButtons(
                                onEdit = { editingBank = b; bankDialog = true },
                                onDelete = { viewModel.deleteBank(b.id) }
                            )
                        }
                    }
                }
            }
        }
        item {
            LedgerCard {
                RowSpaceBetween {
                    Text(strings.otherAssets, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    TextButton(onClick = { editingAsset = null; assetDialog = true }) { Text("+ ${strings.add}") }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                if (state.assets.isEmpty()) EmptyState(strings.noAssetsYet)
                else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.assets.forEach { a ->
                        RowSpaceBetween {
                            Column(Modifier.weight(1f)) {
                                Text(a.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(formatAmount(a.value) + " " + strings.currency, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            EditDeleteButtons(
                                onEdit = { editingAsset = a; assetDialog = true },
                                onDelete = { viewModel.deleteAsset(a.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (bankDialog) {
        BankDialog(editingBank, strings, { bankDialog = false }) { b -> viewModel.saveBank(b); bankDialog = false }
    }
    if (assetDialog) {
        AssetDialog(editingAsset, strings, { assetDialog = false }) { a -> viewModel.saveAsset(a); assetDialog = false }
    }
}

@Composable
private fun BankDialog(initial: BankAccount?, strings: AppStrings, onDismiss: () -> Unit, onSave: (BankAccount) -> Unit) {
    var bank by remember { mutableStateOf(initial?.bank ?: "") }
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var balance by remember { mutableStateOf(initial?.let { plainNumber(it.balance) } ?: "") }
    var unit by remember { mutableStateOf(if (initial != null) 1L else 0L) }
    val effectiveUnit = if (unit == 0L) LocalInputUnit.current else unit
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial != null) strings.t("ویرایش حساب", "Edit account") else strings.bankAccounts) },
        text = {
            FormColumn {
                OutlinedTextField(value = bank, onValueChange = { bank = it }, singleLine = true, label = { Text(strings.bankNameLabel) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = title, onValueChange = { title = it }, singleLine = true, label = { Text(strings.accountTitleLabel) }, modifier = Modifier.fillMaxWidth())
                AmountField(balance, { balance = it }, effectiveUnit, { unit = it }, strings.balanceLabel)
                error?.let { Text(it, color = AppColors.StampRed, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val b = parseAmount(balance, effectiveUnit)
                when {
                    bank.isBlank() -> error = strings.invalidName
                    b == null -> error = strings.invalidAmount
                    else -> onSave(BankAccount(id = initial?.id ?: 0L, bank = bank.trim(), title = title.trim(), balance = b))
                }
            }) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
private fun AssetDialog(initial: Asset?, strings: AppStrings, onDismiss: () -> Unit, onSave: (Asset) -> Unit) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var value by remember { mutableStateOf(initial?.let { plainNumber(it.value) } ?: "") }
    var unit by remember { mutableStateOf(if (initial != null) 1L else 0L) }
    val effectiveUnit = if (unit == 0L) LocalInputUnit.current else unit
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial != null) strings.t("ویرایش دارایی", "Edit asset") else strings.otherAssets) },
        text = {
            FormColumn {
                OutlinedTextField(value = title, onValueChange = { title = it }, singleLine = true, label = { Text(strings.assetTitleLabel) }, modifier = Modifier.fillMaxWidth())
                AmountField(value, { value = it }, effectiveUnit, { unit = it }, strings.assetValueLabel)
                error?.let { Text(it, color = AppColors.StampRed, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val v = parseAmount(value, effectiveUnit)
                when {
                    title.isBlank() -> error = strings.invalidName
                    v == null -> error = strings.invalidAmount
                    else -> onSave(Asset(id = initial?.id ?: 0L, title = title.trim(), value = v))
                }
            }) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}
