package com.acuranza.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acuranza.app.designsystem.components.*
import com.acuranza.app.designsystem.theme.*
import com.acuranza.app.model.Bank
import com.acuranza.app.model.Paise
import com.acuranza.app.model.TxnType
import com.acuranza.app.ui.common.Fmt
import com.acuranza.app.ui.common.Pill
import com.acuranza.app.ui.common.bankEnum
import com.acuranza.app.ui.common.label
import com.acuranza.app.ui.nav.Routes
import androidx.navigation.NavController

@Composable
fun HomeScreenDesloped(
    nav: NavController,
    state: HomeState,
    hidden: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onProfile: () -> Unit = {},
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
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(bottom = 8.dp),
    ) {
        item(key = "header") {
            Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    com.acuranza.app.ui.settings.ProfileAvatar(
                        path = state.profileAvatar,
                        name = state.profileName,
                        size = 40.dp,
                        modifier = Modifier
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onProfile),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(Fmt.todayLong(), style = MaterialTheme.typography.bodySmall, color = SumvaloTheme.colors.subtleText)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            Fmt.greeting() + (state.profileName?.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""),
                            style = SumvaloText.screenTitle,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        item(key = "safe-to-spend") {
            if (state.sections.none { it == HomeSection.SAFE }) {
                val safe = state.safeToSpend
                if (safe != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SumvaloTheme.colors.fill)
                            .clickable(onClick = onSafeToSpend),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Safe to spend", style = MaterialTheme.typography.labelSmall, color = SumvaloTheme.colors.subtleText)
                        Spacer(Modifier.weight(1f))
                        Text(
                            if (hidden) "••••" else "₹${String.format("%d.%02d", safe.perDay / 100, safe.perDay % 100)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                            color = SumvaloTheme.colors.ink,
                        )
                    }
                }
            }
        }
        item(key = "networth") {
            Column(Modifier.padding(horizontal = 20.dp).padding(top = SumvaloTheme.spacing.section)) {
                if (state.netWorth == null) {
                    SectionHeader("Net worth")
                    Spacer(Modifier.height(12.dp))
                    Text("No net worth data yet", style = MaterialTheme.typography.bodyMedium, color = SumvaloTheme.colors.subtleText)
                } else {
                    val nw = state.netWorth!!
                    SectionHeader("Net worth")
                    Spacer(Modifier.height(12.dp))
                    GradientCard(
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Total", style = MaterialTheme.typography.bodyMedium, color = SumvaloTheme.colors.subtleText)
                                Spacer(Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(4.dp))
                            AmountText(
                                paise = nw.total,
                                style = AmountStyles.hero,
                                hidden = hidden,
                                color = SumvaloTheme.colors.ink,
                            )
                            Spacer(Modifier.height(8.dp))
                            AmountText(
                                paise = nw.total,
                                style = AmountStyles.medium,
                                hidden = hidden,
                                color = SumvaloTheme.colors.subtleText,
                            )
                        }
                    }
                }
            }
        }
        item(key = "stats") {
            Column(Modifier.padding(horizontal = 20.dp).padding(top = SumvaloTheme.spacing.section)) {
                if (state.summary == null) {
                    SectionHeader("This month")
                    Spacer(Modifier.height(12.dp))
                    Text("No summary data yet", style = MaterialTheme.typography.bodyMedium, color = SumvaloTheme.colors.subtleText)
                } else {
                    val s = state.summary!!
                    SectionHeader("This month")
                    Spacer(Modifier.height(12.dp))
                    SumvaloCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
                        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                            StatChip("Income", s.income, Modifier.weight(1f), type = TxnType.INCOME, hidden = hidden)
                            VerticalHairline()
                            StatChip("Spent", s.expense, Modifier.weight(1f).padding(start = 14.dp), type = TxnType.EXPENSE, hidden = hidden)
                            VerticalHairline()
                            StatChip("Saved", s.savings, Modifier.weight(1f).padding(start = 14.dp), hidden = hidden)
                        }
                    }
                }
            }
        }
        item(key = "bills") {
            Column(Modifier.padding(horizontal = 20.dp).padding(top = SumvaloTheme.spacing.section)) {
                if (state.bills.isEmpty()) {
                    SectionHeader("Upcoming bills")
                    Spacer(Modifier.height(12.dp))
                    Text("No upcoming bills", style = MaterialTheme.typography.bodyMedium, color = SumvaloTheme.colors.subtleText)
                } else {
                    SectionHeader("Upcoming bills", actionLabel = "See all", onAction = onBills)
                    Spacer(Modifier.height(12.dp))
                    SumvaloCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                        val bills = state.bills.take(3)
                        for (i in bills.indices) {
                            val b = bills[i]
                            if (i > 0) Hairline()
                            BillRow(b, hidden, onBills)
                        }
                    }
                }
            }
        }
        item(key = "recent") {
            Column(Modifier.padding(horizontal = 20.dp).padding(top = SumvaloTheme.spacing.section)) {
                if (state.recent.isEmpty()) {
                    SectionHeader("Recent")
                    Spacer(Modifier.height(12.dp))
                    Text("No recent transactions", style = MaterialTheme.typography.bodyMedium, color = SumvaloTheme.colors.subtleText)
                } else {
                    SectionHeader("Recent", actionLabel = "See all", onAction = onActivity)
                    Spacer(Modifier.height(12.dp))
                    SumvaloCard(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)) {
                        for (i in state.recent.indices) {
                            val t = state.recent[i]
                            if (i > 0) Hairline(start = 56.dp)
                            TxnRow(t, hidden) { onTransaction(t.txn.id) }
                        }
                    }
                }
            }
        }
        item(key = "accounts") {
            Column(Modifier.padding(horizontal = 20.dp).padding(top = SumvaloTheme.spacing.section)) {
                if (state.accounts.isEmpty()) {
                    SectionHeader("Accounts")
                    Spacer(Modifier.height(12.dp))
                    Text("No accounts", style = MaterialTheme.typography.bodyMedium, color = SumvaloTheme.colors.subtleText)
                } else {
                    SectionHeader("Accounts", actionLabel = "Manage", onAction = onAccounts)
                    Spacer(Modifier.height(12.dp))
                    SumvaloCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                        for (i in state.accounts.indices) {
                            val a = state.accounts[i]
                            if (i > 0) Hairline(start = 48.dp)
                            AccountRow(a, hidden) { onAccount(a.id) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Hairline(start: androidx.compose.ui.unit.Dp = 0.dp) {
    Box(Modifier.fillMaxWidth().padding(start = start).height(1.dp).background(SumvaloTheme.colors.divider))
}

@Composable
private fun VerticalHairline() {
    Box(Modifier.fillMaxHeight().width(1.dp).background(SumvaloTheme.colors.divider))
}

@Composable
private fun BillRow(b: com.acuranza.app.repo.BillItem, hidden: Boolean, onClick: () -> Unit) {
    val days = b.daysLeft
    val urgent = days != null && days <= 3
    val dueColor = when {
        days != null && days < 0 -> SumvaloTheme.colors.alert
        urgent -> SumvaloTheme.colors.warning
        else -> SumvaloTheme.colors.subtleText
    }
    val bank = b.card?.bankEnum() ?: Bank.OTHER
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BankBadge(bank, size = 32.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(b.card?.label() ?: "Card", style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                if (urgent || (days != null && days < 0)) {
                    Spacer(Modifier.width(6.dp))
                    Pill(
                        if (days != null && days < 0) "Overdue" else if (days == 0L) "Due today" else Fmt.relativeDays(days ?: 0),
                        dueColor,
                    )
                }
            }
            Text(if (days != null) Fmt.relativeDays(days) else "No due date", style = MaterialTheme.typography.bodySmall, color = dueColor)
        }
        AmountText(b.bill.totalDue, style = AmountStyles.medium, hidden = hidden)
    }
}

@Composable
private fun AccountRow(a: com.acuranza.app.repo.AccountWithBalance, hidden: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BankBadge(a.bank, size = 32.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(a.account.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(
                if (a.isCard) "Outstanding" else a.account.type.label(),
                style = MaterialTheme.typography.bodySmall,
                color = SumvaloTheme.colors.subtleText,
            )
        }
        AmountText(if (a.isCard) a.outstanding else a.balance, style = AmountStyles.medium, hidden = hidden)
    }
}
