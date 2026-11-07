package com.ledgeryaar.ui.screens.cashflow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledgeryaar.domain.model.PersianCalendarConverter
import com.ledgeryaar.i18n.LocalStrings
import com.ledgeryaar.ui.components.LedgerCard
import com.ledgeryaar.ui.components.RowSpaceBetween
import com.ledgeryaar.ui.theme.AppColors
import com.ledgeryaar.util.formatAmount
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CashflowScreen(viewModel: com.ledgeryaar.ui.LedgerViewModel = koinViewModel()) {
    val strings = LocalStrings.current
    val state by viewModel.state.collectAsState()

    val remainingPayments = state.remainingPaymentsThisMonth
    val expectedIncome = state.expectedRemainingIncomeThisMonth
    val deficit = remainingPayments - expectedIncome
    val hasShortage = deficit > 0
    val weeks = state.cashflowForecastWeeks(4)

    LazyColumn(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(strings.navCashflow, style = MaterialTheme.typography.headlineMedium) }
        item {
            LedgerCard {
                Column {
                    Text(
                        if (hasShortage) strings.cashflowShortageWarning else strings.cashflowHealthyMessage,
                        fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        color = if (hasShortage) AppColors.StampRed else AppColors.LedgerGreen
                    )
                    RowSpaceBetween(Modifier.padding(top = 10.dp)) {
                        Column {
                            Text(strings.remainingPayments, fontSize = 11.sp)
                            Text(formatAmount(remainingPayments) + " " + strings.currency, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(if (deficit <= 0) strings.surplus else strings.shortage, fontSize = 11.sp)
                            Text(formatAmount(kotlin.math.abs(deficit)) + " " + strings.currency, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
        items(weeks) { w ->
            LedgerCard {
                RowSpaceBetween {
                    Text("${strings.weekLabel} ${w.number}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(
                        PersianCalendarConverter.toJalali(w.start).displayShort + " ← " + PersianCalendarConverter.toJalali(w.end).displayShort,
                        fontSize = 11.sp
                    )
                }
                Text("${strings.cashNeeded}: ${formatAmount(w.certain)} ${strings.currency}", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}
