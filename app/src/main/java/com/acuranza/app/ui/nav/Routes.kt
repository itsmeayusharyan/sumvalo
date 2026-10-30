package com.acuranza.app.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.ui.graphics.vector.ImageVector
import com.acuranza.app.model.TxnType

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val TRANSACTIONS = "transactions"
    const val PLANNING = "planning"
    const val SETTINGS = "settings"

    const val REVIEW = "review"
    const val ADD_TXN = "txn/add?type={type}&account={account}"
    const val EDIT_TXN = "txn/edit/{id}"
    const val TXN_DETAIL = "txn/{id}"
    const val ACCOUNTS = "accounts"
    const val ACCOUNT_DETAIL = "account/{id}"
    const val ACCOUNT_EDIT = "account/edit?id={id}&card={card}"
    const val BILLS = "bills"
    const val NOTIFICATIONS = "notifications"
    const val EDIT_HOME = "home/edit"
    const val SETTINGS_EDIT = "settings/edit"
    const val SETTINGS_HAPTICS = "settings/haptics"
    const val SETTINGS_NAV_TABS = "settings/nav-tabs"
    const val SETTINGS_MANAGEMENT = "settings/management"
    const val SETTINGS_FINANCIAL = "settings/financial-tools"
    const val SMS_TRACKING = "sms-tracking"
    const val SETTINGS_PRIVACY = "settings/privacy"
    const val SETTINGS_NOTIFICATIONS = "settings/notifications"
    const val SETTINGS_APPEARANCE = "settings/appearance"
    const val SETTINGS_DATA = "settings/data"
    const val SETTINGS_SMS_IMPORTS = "settings/sms-imports"
    const val PROFILE = "profile"
    const val INSIGHTS = "insights"
    const val CATEGORIES = "categories"
    const val PENDING_ACCOUNTS = "accounts/pending"
    const val UNCATEGORISED = "uncategorised"
    const val RECURRING = "recurring"
    const val RECURRING_EDIT = "recurring/edit?id={id}&series={series}"
    const val LOANS = "loans"
    const val LOAN_DETAIL = "loan/{id}"
    const val LOAN_EDIT = "loan/edit?id={id}"
    const val DEBTS = "debts"
    const val AUTO_BACKUP = "backup/auto"
    const val STATEMENT_IMPORT = "import/statement"
    const val REPORTS = "reports"
    const val YEAR_REVIEW = "reports/year/{year}"
    const val CALENDAR = "calendar"
    const val ABOUT = "about"
    const val ABOUT_PRIVACY = "about/privacy"
    const val ABOUT_TERMS = "about/terms"
    const val ABOUT_GPL = "about/gpl"
    const val CHANGELOG = "changelog"
    const val SAVINGS_GOAL = "savings-goal"
    const val WIDGETS_PREVIEW = "widgets"
    // Split Any Expense
    const val SPLIT_GROUPS = "split-groups"
    const val SPLIT_GROUP_DETAIL = "split-group/{id}"
    const val SPLIT_GROUP_EDIT = "split-group/edit?id={id}"
    const val SPLIT_ADD_EXPENSE = "split-expense/add?group={group}"
    const val SPLIT_EXPENSE_DETAIL = "split-expense/{id}"
    const val SPLIT_SETTLEMENTS = "split-settlements?group={group}"
    // Purchase Memory
    const val PURCHASES = "purchases"
    const val PURCHASE_DETAIL = "purchase/{id}"
    const val PURCHASE_ADD = "purchase/add"
    const val PURCHASE_EDIT = "purchase/edit?id={id}"

    fun addTxn(type: TxnType = TxnType.EXPENSE, accountId: Long? = null) = "txn/add?type=${type.name}&account=${accountId ?: -1}"
    fun editTxn(id: Long) = "txn/edit/$id"
    fun txn(id: Long) = "txn/$id"
    fun account(id: Long) = "account/$id"
    fun editAccount(id: Long? = null, card: Boolean = false) = "account/edit?id=${id ?: -1}&card=$card"
    fun editRecurring(id: Long? = null, fromSeries: String? = null) =
        "recurring/edit?id=${id ?: -1}" + (fromSeries?.let { "&series=" + android.net.Uri.encode(it) } ?: "")
    fun loan(id: Long) = "loan/$id"
    fun editLoan(id: Long? = null) = "loan/edit?id=${id ?: -1}"
    fun yearReview(year: Int) = "reports/year/$year"
    fun splitGroup(id: Long) = "split-group/$id"
    fun splitGroupEdit(id: Long? = null) = "split-group/edit?id=${id ?: -1}"
    fun splitAddExpense(groupId: Long) = "split-expense/add?group=$groupId"
    fun splitExpense(id: Long) = "split-expense/$id"
    fun splitSettlements(groupId: Long) = "split-settlements?group=$groupId"
    fun purchase(id: Long) = "purchase/$id"
    fun purchaseEdit(id: Long? = null) = "purchase/edit?id=${id ?: -1}"
}

data class Tab(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

val Tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Outlined.Home, Icons.Filled.Home),
    Tab(Routes.INSIGHTS, "Insights", Icons.Outlined.QueryStats, Icons.Filled.QueryStats),
    Tab(Routes.TRANSACTIONS, "Activity", Icons.AutoMirrored.Outlined.ReceiptLong, Icons.AutoMirrored.Filled.ReceiptLong),
    Tab(Routes.SETTINGS, "Settings", Icons.Outlined.Settings, Icons.Filled.Settings),
)

val TabRoutes = Tabs.map { it.route }.toSet()
