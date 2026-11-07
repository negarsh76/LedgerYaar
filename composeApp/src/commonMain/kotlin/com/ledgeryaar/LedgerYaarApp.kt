@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ledgeryaar

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ledgeryaar.data.repository.AppSettings
import com.ledgeryaar.data.repository.SettingsRepository
import com.ledgeryaar.i18n.AppLanguage
import com.ledgeryaar.i18n.LocalStrings
import com.ledgeryaar.i18n.stringsFor
import com.ledgeryaar.ui.nav.Screen
import com.ledgeryaar.ui.screens.assets.AssetsScreen
import com.ledgeryaar.ui.screens.budgets.BudgetsScreen
import com.ledgeryaar.ui.screens.cashflow.CashflowScreen
import com.ledgeryaar.ui.screens.dashboard.DashboardScreen
import com.ledgeryaar.ui.screens.goals.GoalsScreen
import com.ledgeryaar.ui.screens.debts.PeopleScreen
import com.ledgeryaar.ui.screens.incomes.TransactionsScreen
import com.ledgeryaar.ui.screens.yearly.YearlyScreen
import com.ledgeryaar.domain.model.PersonType
import com.ledgeryaar.domain.model.TxType
import com.ledgeryaar.domain.period.PeriodHolder
import com.ledgeryaar.ui.LocalCustomCategories
import com.ledgeryaar.ui.LocalInputUnit
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import com.ledgeryaar.ui.screens.loans.LoansScreen
import com.ledgeryaar.ui.screens.settings.SettingsScreen
import com.ledgeryaar.ui.screens.splash.SplashScreen
import com.ledgeryaar.ui.screens.review.ReviewScreen
import com.ledgeryaar.ui.nav.NavRequests
import com.ledgeryaar.ui.components.AppLogo
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import com.ledgeryaar.ui.theme.LedgerTheme
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun LedgerYaarApp() {
    val settingsRepository: SettingsRepository = koinInject()
    val settings by settingsRepository.settings.collectAsState(initial = AppSettings())
    val baseStrings = stringsFor(settings.language)
    // واحد پول انتخابی کاربر جای «تومان» در همهٔ صفحه‌ها می‌نشیند
    val strings = baseStrings.copy(
        currency = if (settings.language == AppLanguage.FA) settings.currency.fa else settings.currency.en
    )
    val direction = if (settings.language == AppLanguage.FA) LayoutDirection.Rtl else LayoutDirection.Ltr
    var showSplash by remember { mutableStateOf(true) }

    // رفتن خودکار به ماه جدید وقتی ماه واقعی عوض می‌شود (داده‌ی ماه قبل در تاریخچه می‌ماند)
    val periodHolder: PeriodHolder = koinInject()
    LaunchedEffect(Unit) {
        while (true) {
            periodHolder.refreshIfNewMonth()
            delay(60_000)
        }
    }

    CompositionLocalProvider(
        LocalStrings provides strings,
        LocalLayoutDirection provides direction,
        LocalInputUnit provides settings.inputUnit,
        LocalCustomCategories provides settings.customCategories
    ) {
        LedgerTheme(theme = settings.theme) {
            Surface {
                if (showSplash) {
                    SplashScreen(strings = strings, onFinished = { showSplash = false })
                } else {
                    LedgerNavGraph()
                }
            }
        }
    }
}

@Composable
private fun LedgerNavGraph() {
    val strings = LocalStrings.current
    val navController: NavHostController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // لمس نوتیف «تراکنش جدید» ← مستقیم صفحهٔ بررسی
    val requested by NavRequests.pendingRoute.collectAsState()
    LaunchedEffect(requested) {
        requested?.let { route ->
            navController.navigate(route) {
                popUpTo(Screen.Dashboard.route)
                launchSingleTop = true
            }
            NavRequests.pendingRoute.value = null
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Screen.Dashboard.route
    val currentTitle = Screen.drawerItems.find { it.route == currentRoute }?.title?.invoke(strings) ?: strings.appName

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(Modifier.verticalScroll(rememberScrollState())) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppLogo(size = 52.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(strings.appName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Text(strings.dashboardSubtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Screen.drawerItems.forEach { screen ->
                    NavigationDrawerItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.title(strings)) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(screen.route) {
                                popUpTo(Screen.Dashboard.route)
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(currentTitle) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = null)
                        }
                    }
                )
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.padding(padding).fillMaxWidth()
            ) {
                composable(Screen.Dashboard.route) {
                    DashboardScreen(onOpenReview = {
                        navController.navigate(Screen.Review.route) { launchSingleTop = true }
                    })
                }
                composable(Screen.Review.route) { ReviewScreen() }
                composable(Screen.Incomes.route) { TransactionsScreen(TxType.INCOME) }
                composable(Screen.Expenses.route) { TransactionsScreen(TxType.EXPENSE) }
                composable(Screen.Loans.route) { LoansScreen() }
                composable(Screen.Debts.route) { PeopleScreen(PersonType.DEBT) }
                composable(Screen.Receivables.route) { PeopleScreen(PersonType.RECEIVABLE) }
                composable(Screen.Budgets.route) { BudgetsScreen() }
                composable(Screen.Goals.route) { GoalsScreen() }
                composable(Screen.Assets.route) { AssetsScreen() }
                composable(Screen.Cashflow.route) { CashflowScreen() }
                composable(Screen.Yearly.route) { YearlyScreen() }
                composable(Screen.Settings.route) { SettingsScreen() }
            }
        }
    }
}
