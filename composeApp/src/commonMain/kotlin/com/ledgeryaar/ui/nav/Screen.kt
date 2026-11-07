package com.ledgeryaar.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import com.ledgeryaar.i18n.AppStrings
import com.ledgeryaar.i18n.t

sealed class Screen(
    val route: String,
    val icon: ImageVector,
    val title: (AppStrings) -> String
) {
    data object Dashboard :
        Screen("dashboard", Icons.Filled.Home, { it.navDashboard })

    data object Review :
        Screen("review", Icons.Filled.Notifications, { it.t("بررسی پیامک‌های بانک", "Review bank SMS") })

    data object Incomes :
        Screen("incomes", Icons.Filled.KeyboardArrowUp, { it.navIncomes })

    data object Expenses :
        Screen("expenses", Icons.Filled.KeyboardArrowDown, { it.navExpenses })

    data object Loans :
        Screen("loans", Icons.Filled.List, { it.navLoans })

    data object Debts :
        Screen("debts", Icons.Filled.KeyboardArrowDown, { it.navDebts })

    data object Receivables :
        Screen("receivables", Icons.Filled.Favorite, { it.navReceivables })

    data object Budgets :
        Screen("budgets", Icons.Filled.List, { it.navBudgets })

    data object Goals :
        Screen("goals", Icons.Filled.Star, { it.navGoals })

    data object Assets :
        Screen("assets", Icons.Filled.Home, { it.navAssets })

    data object Cashflow :
        Screen("cashflow", Icons.Filled.DateRange, { it.navCashflow })

    data object Yearly :
        Screen("yearly", Icons.Filled.Info, { it.t("عملکرد سالانه", "Yearly report") })

    data object Settings :
        Screen("settings", Icons.Filled.Settings, { it.navSettings })

    companion object {
        // get() لازم است: با مقداردهی مستقیم، موقع لود کلاس Screen هنوز objectها null هستند
        val drawerItems: List<Screen>
            get() = listOf(
            Dashboard,
            Review,
            Incomes,
            Expenses,
            Loans,
            Debts,
            Receivables,
            Budgets,
            Goals,
            Assets,
            Cashflow,
            Yearly,
            Settings
        )
    }
}
