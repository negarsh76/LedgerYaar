package com.ledgeryaar.i18n

import androidx.compose.runtime.compositionLocalOf
import com.ledgeryaar.domain.model.CategoryId

enum class AppLanguage { FA, EN }

data class AppStrings(
    val appName: String,
    // navigation
    val navDashboard: String,
    val navIncomes: String,
    val navExpenses: String,
    val navLoans: String,
    val navDebts: String,
    val navReceivables: String,
    val navBudgets: String,
    val navGoals: String,
    val navAssets: String,
    val navCashflow: String,
    val navSettings: String,
    // common actions
    val add: String,
    val save: String,
    val cancel: String,
    val delete: String,
    val settle: String,
    val reopen: String,
    val currency: String,
    // dashboard
    val dashboardSubtitle: String,
    val netWorth: String,
    val monthNet: String,
    val monthIncome: String,
    val monthExpense: String,
    val upcomingDues: String,
    val noUpcomingDues: String,
    val recentTransactions: String,
    val noTransactionsYet: String,
    // transactions
    val amountLabel: String,
    val dateLabel: String,
    val dateHint: String,
    val categoryLabel: String,
    val noteLabel: String,
    val addIncome: String,
    val addExpense: String,
    val noIncomesYet: String,
    val noExpensesYet: String,
    val invalidAmount: String,
    val invalidDate: String,
    // loans
    val addLoan: String,
    val loanTitleLabel: String,
    val totalAmountLabel: String,
    val installmentCountLabel: String,
    val firstDueDateLabel: String,
    val noLoansYet: String,
    val installmentsPaidOf: String,
    val remaining: String,
    val installmentLabel: String,
    // people (debts/receivables)
    val totalActiveDebt: String,
    val totalActiveReceivable: String,
    val personNameLabel: String,
    val dueDateOptionalLabel: String,
    val noItemsYet: String,
    val overdue: String,
    val settled: String,
    val addDebtParty: String,
    val addReceivableParty: String,
    val invalidName: String,
    // budgets
    val addBudget: String,
    val budgetLimitLabel: String,
    val noBudgetsYet: String,
    val overBudget: String,
    // goals
    val addGoal: String,
    val goalTitleLabel: String,
    val targetAmountLabel: String,
    val currentAmountLabel: String,
    val noGoalsYet: String,
    // assets
    val netWorthLabel: String,
    val bankAccounts: String,
    val otherAssets: String,
    val noBankAccountsYet: String,
    val noAssetsYet: String,
    val bankNameLabel: String,
    val accountTitleLabel: String,
    val balanceLabel: String,
    val assetTitleLabel: String,
    val assetValueLabel: String,
    // cashflow
    val cashflowShortageWarning: String,
    val cashflowHealthyMessage: String,
    val remainingPayments: String,
    val surplus: String,
    val shortage: String,
    val weekLabel: String,
    val cashNeeded: String,
    // settings
    val aboutApp: String,
    val aboutAppBody: String,
    val language: String,
    val darkMode: String,
    val calendarInfo: String,
    val calendarInfoBody: String,
    val categoryLabelFor: (CategoryId) -> String,
    /** true یعنی زبان فارسی؛ برای متن‌های جدید با helper `t` */
    val isFa: Boolean = true
)

/** انتخاب متن بر اساس زبان فعلی */
fun AppStrings.t(fa: String, en: String): String = if (isFa) fa else en

private val fa = AppStrings(
    appName = "دفتر یار",
    navDashboard = "داشبورد",
    navIncomes = "درآمدها",
    navExpenses = "هزینه‌ها",
    navLoans = "اقساط و وام‌ها",
    navDebts = "بدهی‌ها",
    navReceivables = "طلب‌ها",
    navBudgets = "بودجه‌بندی",
    navGoals = "اهداف مالی",
    navAssets = "دارایی و حساب‌ها",
    navCashflow = "جریان نقدی",
    navSettings = "تنظیمات",
    add = "افزودن",
    save = "ذخیره",
    cancel = "انصراف",
    delete = "حذف",
    settle = "تسویه",
    reopen = "بازگشایی",
    currency = "تومان",
    dashboardSubtitle = "نگاهی کلی به دفتر کل شما",
    netWorth = "دارایی خالص",
    monthNet = "مانده این ماه",
    monthIncome = "درآمد این ماه",
    monthExpense = "هزینه این ماه",
    upcomingDues = "سررسیدهای نزدیک",
    noUpcomingDues = "سررسید نزدیکی وجود ندارد ✓",
    recentTransactions = "آخرین تراکنش‌ها",
    noTransactionsYet = "هنوز تراکنشی ثبت نشده",
    amountLabel = "مبلغ (تومان)",
    dateLabel = "تاریخ",
    dateHint = "تاریخ شمسی، مثل 1403/03/15",
    categoryLabel = "دسته‌بندی",
    noteLabel = "توضیحات (اختیاری)",
    addIncome = "ثبت درآمد جدید",
    addExpense = "ثبت هزینه جدید",
    noIncomesYet = "هنوز درآمدی ثبت نشده",
    noExpensesYet = "هنوز هزینه‌ای ثبت نشده",
    invalidAmount = "مبلغ معتبر وارد کنید",
    invalidDate = "فرمت تاریخ صحیح نیست",
    addLoan = "ثبت وام جدید",
    loanTitleLabel = "عنوان وام",
    totalAmountLabel = "مبلغ کل (تومان)",
    installmentCountLabel = "تعداد اقساط",
    firstDueDateLabel = "سررسید قسط اول",
    noLoansYet = "هنوز وامی ثبت نشده",
    installmentsPaidOf = "قسط پرداخت‌شده از",
    remaining = "باقی‌مانده",
    installmentLabel = "قسط",
    totalActiveDebt = "مجموع بدهی فعال",
    totalActiveReceivable = "مجموع طلب فعال",
    personNameLabel = "نام شخص",
    dueDateOptionalLabel = "سررسید (اختیاری)",
    noItemsYet = "موردی ثبت نشده",
    overdue = "گذشته از سررسید",
    settled = "تسویه شده",
    addDebtParty = "ثبت طرف بدهی",
    addReceivableParty = "ثبت طرف طلب",
    invalidName = "نام را وارد کنید",
    addBudget = "افزودن سقف بودجه",
    budgetLimitLabel = "سقف بودجه (تومان)",
    noBudgetsYet = "هنوز سقف بودجه‌ای ثبت نشده",
    overBudget = "فراتر از سقف",
    addGoal = "هدف مالی جدید",
    goalTitleLabel = "عنوان هدف",
    targetAmountLabel = "مبلغ هدف",
    currentAmountLabel = "مبلغ فعلی",
    noGoalsYet = "هنوز هدفی ثبت نشده",
    netWorthLabel = "خالص ارزش دارایی",
    bankAccounts = "حساب‌های بانکی",
    otherAssets = "سایر دارایی‌ها",
    noBankAccountsYet = "حسابی ثبت نشده",
    noAssetsYet = "دارایی ثبت نشده",
    bankNameLabel = "نام بانک",
    accountTitleLabel = "عنوان حساب",
    balanceLabel = "موجودی فعلی (تومان)",
    assetTitleLabel = "عنوان دارایی",
    assetValueLabel = "ارزش تقریبی (تومان)",
    cashflowShortageWarning = "احتمال کسری بودجه تا پایان ماه",
    cashflowHealthyMessage = "وضعیت نقدینگی پایان ماه مناسب است",
    remainingPayments = "پرداخت باقی‌مانده",
    surplus = "مازاد",
    shortage = "کسری",
    weekLabel = "هفته",
    cashNeeded = "نیاز نقدینگی",
    aboutApp = "درباره برنامه",
    aboutAppBody = "دفتر یار — مدیریت مالی شخصی. تمام اطلاعات به‌صورت محلی روی گوشی شما ذخیره می‌شود.",
    language = "زبان",
    darkMode = "حالت تیره",
    calendarInfo = "تقویم",
    calendarInfoBody = "تمام تاریخ‌ها در برنامه بر اساس تقویم شمسی نمایش داده می‌شوند.",
    categoryLabelFor = { cat ->
        when (cat) {
            CategoryId.FOOD -> "خوراک"
            CategoryId.RESTAURANT -> "رستوران"
            CategoryId.TRANSPORT -> "حمل و نقل"
            CategoryId.BILLS -> "قبوض"
            CategoryId.ENTERTAINMENT -> "تفریح"
            CategoryId.SHOPPING -> "خرید شخصی"
            CategoryId.HEALTH -> "درمان"
            CategoryId.EDUCATION -> "آموزش"
            CategoryId.OTHER_EXPENSE -> "سایر"
            CategoryId.SALARY -> "حقوق"
            CategoryId.PROJECT -> "پروژه"
            CategoryId.BONUS -> "پاداش"
            CategoryId.OTHER_INCOME -> "سایر"
        }
    }
)

private val en = AppStrings(
    appName = "Ledger Yaar",
    navDashboard = "Dashboard",
    navIncomes = "Incomes",
    navExpenses = "Expenses",
    navLoans = "Loans & Installments",
    navDebts = "Debts",
    navReceivables = "Receivables",
    navBudgets = "Budgets",
    navGoals = "Goals",
    navAssets = "Assets & Accounts",
    navCashflow = "Cashflow",
    navSettings = "Settings",
    add = "Add",
    save = "Save",
    cancel = "Cancel",
    delete = "Delete",
    settle = "Settle",
    reopen = "Reopen",
    currency = "Toman",
    dashboardSubtitle = "An overview of your finances",
    netWorth = "Net Worth",
    monthNet = "This Month's Balance",
    monthIncome = "This Month's Income",
    monthExpense = "This Month's Expense",
    upcomingDues = "Upcoming Dues",
    noUpcomingDues = "No upcoming dues ✓",
    recentTransactions = "Recent Transactions",
    noTransactionsYet = "No transactions yet",
    amountLabel = "Amount (Toman)",
    dateLabel = "Date",
    dateHint = "Persian date, e.g. 1403/03/15",
    categoryLabel = "Category",
    noteLabel = "Note (optional)",
    addIncome = "Add Income",
    addExpense = "Add Expense",
    noIncomesYet = "No incomes recorded yet",
    noExpensesYet = "No expenses recorded yet",
    invalidAmount = "Enter a valid amount",
    invalidDate = "Date format is invalid",
    addLoan = "Add New Loan",
    loanTitleLabel = "Loan Title",
    totalAmountLabel = "Total Amount (Toman)",
    installmentCountLabel = "Number of Installments",
    firstDueDateLabel = "First Installment Due Date",
    noLoansYet = "No loans recorded yet",
    installmentsPaidOf = "installments paid of",
    remaining = "Remaining",
    installmentLabel = "Installment",
    totalActiveDebt = "Total Active Debt",
    totalActiveReceivable = "Total Active Receivable",
    personNameLabel = "Person's Name",
    dueDateOptionalLabel = "Due Date (optional)",
    noItemsYet = "Nothing recorded yet",
    overdue = "Overdue",
    settled = "Settled",
    addDebtParty = "Add Debt",
    addReceivableParty = "Add Receivable",
    invalidName = "Enter a name",
    addBudget = "Add Budget Limit",
    budgetLimitLabel = "Budget Limit (Toman)",
    noBudgetsYet = "No budget limits set yet",
    overBudget = "Over budget",
    addGoal = "New Financial Goal",
    goalTitleLabel = "Goal Title",
    targetAmountLabel = "Target Amount",
    currentAmountLabel = "Current Amount",
    noGoalsYet = "No goals set yet",
    netWorthLabel = "Net Asset Value",
    bankAccounts = "Bank Accounts",
    otherAssets = "Other Assets",
    noBankAccountsYet = "No accounts recorded",
    noAssetsYet = "No assets recorded",
    bankNameLabel = "Bank Name",
    accountTitleLabel = "Account Title",
    balanceLabel = "Current Balance (Toman)",
    assetTitleLabel = "Asset Title",
    assetValueLabel = "Approximate Value (Toman)",
    cashflowShortageWarning = "Possible cash shortage by month end",
    cashflowHealthyMessage = "Liquidity looks healthy for month end",
    remainingPayments = "Remaining Payments",
    surplus = "Surplus",
    shortage = "Shortage",
    weekLabel = "Week",
    cashNeeded = "Cash needed",
    aboutApp = "About the App",
    aboutAppBody = "Ledger Yaar — personal finance manager. All data is stored locally on your device.",
    language = "Language",
    darkMode = "Dark Mode",
    calendarInfo = "Calendar",
    calendarInfoBody = "All dates in the app are shown using the Persian (Jalali) calendar.",
    isFa = false,
    categoryLabelFor = { cat ->
        when (cat) {
            CategoryId.FOOD -> "Food"
            CategoryId.RESTAURANT -> "Restaurant"
            CategoryId.TRANSPORT -> "Transport"
            CategoryId.BILLS -> "Bills"
            CategoryId.ENTERTAINMENT -> "Entertainment"
            CategoryId.SHOPPING -> "Shopping"
            CategoryId.HEALTH -> "Health"
            CategoryId.EDUCATION -> "Education"
            CategoryId.OTHER_EXPENSE -> "Other"
            CategoryId.SALARY -> "Salary"
            CategoryId.PROJECT -> "Project"
            CategoryId.BONUS -> "Bonus"
            CategoryId.OTHER_INCOME -> "Other"
        }
    }
)

fun stringsFor(language: AppLanguage): AppStrings = when (language) {
    AppLanguage.FA -> fa
    AppLanguage.EN -> en
}

val LocalStrings = compositionLocalOf { fa }
