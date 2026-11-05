package com.acuranza.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.acuranza.app.designsystem.components.AmountText
import com.acuranza.app.designsystem.components.EmptyState
import com.acuranza.app.designsystem.components.SectionHeader
import com.acuranza.app.designsystem.components.SumvaloCard
import com.acuranza.app.designsystem.theme.AmountStyles
import com.acuranza.app.designsystem.theme.SumvaloText
import com.acuranza.app.designsystem.theme.SumvaloTheme
import com.acuranza.app.ui.common.color

/**
 * Stateless Home screen, rendered from [HomeState]; used by the app and by screenshot tests.
 * The redesigned dashboard: new header/date strip/net-worth hero and the reference's section
 * treatments, while every [HomeSection] stays user-reorderable and hideable as before.
 */
@Composable
fun HomeContent(
    state: HomeState,
    hidden: Boolean,
    onProfile: () -> Unit = {},
    loadingDemo: Boolean = false,
    onToggleHidden: () -> Unit = {},
    onInsights: () -> Unit = {},
    onReview: () -> Unit = {},
    onBills: () -> Unit = {},
    onNotifications: () -> Unit = {},
    onActivity: () -> Unit = {},
    onCategory: (String) -> Unit = {},
    onTransaction: (Long) -> Unit = {},
    onAccounts: () -> Unit = {},
    onAccount: (Long) -> Unit = {},
    onAddAccount: () -> Unit = {},
    onLoadDemo: () -> Unit = {},
    onPendingAccounts: () -> Unit = {},
    onIgnoredSms: () -> Unit = {},
    onDismissIgnoredSms: () -> Unit = {},
    onUncategorised: () -> Unit = {},
    onSafeToSpend: () -> Unit = {},
    onAddExpense: () -> Unit = {},
    onAddIncome: () -> Unit = {},
    onTransfer: () -> Unit = {},
    onReports: () -> Unit = {},
    onPlanning: () -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(bottom = 8.dp),
    ) {
        item(key = "header") {
            HomeHeader(state, onNotifications, onProfile, onSearch = onActivity)
        }

        item(key = "hero") {
            NetWorthHero(state, hidden, onToggleHidden)
        }

        // The reference's "Last 7 days" card sits directly under the balance card.
        if (state.charts.paceCurrent.size >= 2) {
            item(key = "weekly") {
                Box(Modifier.animateItem()) {
                    Last7DaysCard(state, hidden)
                }
            }
        }

        // Action banners and the first-run card sit directly under the balance card: they ask the
        // user for something, so they never compete with charts for attention.
        if (state.ignoredSms > 0) {
            item(key = "ignored-sms") {
                com.acuranza.app.ui.accounts.IgnoredSmsBanner(
                    state.ignoredSms, onIgnoredSms, onDismissIgnoredSms,
                    Modifier.padding(horizontal = 16.dp).padding(top = 16.dp),
                )
            }
        }
        if (state.pendingAccounts > 0) {
            item(key = "pending-accounts") {
                com.acuranza.app.ui.accounts.PendingAccountsBanner(
                    state.pendingAccounts, onPendingAccounts,
                    Modifier.padding(horizontal = 16.dp).padding(top = 16.dp),
                )
            }
        }
        if (state.unreviewed > 0) {
            item(key = "review") {
                ReviewBanner(
                    state.unreviewed, Modifier.padding(horizontal = 16.dp).padding(top = 16.dp),
                    onReview, state.reviewFromSms, state.reviewFromEmail,
                )
            }
        }
        if (state.uncategorised > UNCATEGORISED_BANNER_MIN) {
            item(key = "uncategorised") {
                com.acuranza.app.ui.transactions.UncategorisedBanner(
                    state.uncategorised, onUncategorised,
                    Modifier.padding(horizontal = 16.dp).padding(top = 12.dp),
                )
            }
        }
        if (state.isEmpty) {
            item(key = "empty") {
                Column(Modifier.padding(horizontal = 16.dp).padding(top = 24.dp)) {
                    SumvaloCard {
                        EmptyState(
                            title = "Set up Sumvalo",
                            message = "Add your bank accounts, or scan your SMS inbox to pull in transactions automatically.",
                            icon = Icons.Outlined.AccountBalance,
                            actionLabel = "Add an account",
                            onAction = onAddAccount,
                        )
                        Text(
                            if (loadingDemo) "Loading sample data…" else "Explore with sample data",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = !loadingDemo, onClick = onLoadDemo)
                                .padding(12.dp),
                            color = SumvaloTheme.colors.accent,
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        // The dashboard below is the user's own layout (Settings → Appearance → Edit home):
        // every section keeps its data conditions, order and visibility come from the saved layout,
        // and animateItem() makes reordering and show/hide feel fluid instead of jumpy.
        state.sections.forEach { section ->
            when (section) {
                HomeSection.QUICK -> item(key = "quick") {
                    Box(Modifier.animateItem()) {
                        QuickActionsGrid(state, onActivity, onBills, onPlanning, onAccounts)
                    }
                }
                HomeSection.SAFE -> state.safeToSpend?.let { safe -> item(key = "safe") {
                    Column(Modifier.animateItem().padding(horizontal = 16.dp).padding(top = 20.dp)) {
                        SafeToSpendCard(safe, hidden, onSafeToSpend)
                    }
                } }
                HomeSection.STATS -> item(key = "stats") {
                    Box(Modifier.animateItem()) {
                        MonthlySpendingCard(state, hidden, onSafeToSpend)
                    }
                }
                HomeSection.PACE -> if (state.charts.hasPace) item(key = "pace") {
                    Box(Modifier.animateItem()) {
                        PaceSectionV2(state, hidden, onActivity)
                    }
                }
                HomeSection.BILLS -> if (state.bills.isNotEmpty()) item(key = "bills") {
                    Box(Modifier.animateItem()) {
                        BillsSectionV2(state, hidden, onBills)
                    }
                }
                HomeSection.BUDGETS -> if (state.budgets.any { !it.isOverall }) item(key = "budgets") {
                    Box(Modifier.animateItem()) {
                        BudgetsSectionV2(state, hidden, onPlanning)
                    }
                }
            }
        }
    }
}
