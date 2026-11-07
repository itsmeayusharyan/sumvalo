package com.acuranza.app.ui.accounts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.acuranza.app.data.AccountEntity
import com.acuranza.app.designsystem.components.AmountText
import com.acuranza.app.designsystem.components.BankBadge
import com.acuranza.app.designsystem.components.CreditCardVisual
import com.acuranza.app.designsystem.components.DateHeader
import com.acuranza.app.designsystem.components.EmptyState
import com.acuranza.app.designsystem.components.GradientCard
import com.acuranza.app.designsystem.components.SumvaloCard
import com.acuranza.app.designsystem.components.PrimaryButton
import com.acuranza.app.designsystem.components.SectionHeader
import com.acuranza.app.designsystem.components.SegmentedTabs
import com.acuranza.app.designsystem.components.ThinProgressBar
import com.acuranza.app.designsystem.theme.AmountStyles
import com.acuranza.app.designsystem.theme.SumvaloTheme
import com.acuranza.app.di.AppContainer
import com.acuranza.app.model.AccountType
import com.acuranza.app.model.Bank
import com.acuranza.app.repo.AccountWithBalance
import com.acuranza.app.repo.BillItem
import com.acuranza.app.repo.TransactionItem
import com.acuranza.app.repo.TxnFilter
import com.acuranza.app.ui.bills.BillRow
import com.acuranza.app.ui.bills.PayBillDialog
import com.acuranza.app.ui.common.AmountField
import com.acuranza.app.ui.common.launchGuarded
import com.acuranza.app.ui.common.AmountSheet
import com.acuranza.app.ui.common.ConfirmDialog
import com.acuranza.app.ui.common.Fmt
import com.acuranza.app.ui.common.FormColumn
import com.acuranza.app.ui.common.SumvaloScreen
import com.acuranza.app.ui.common.SumvaloTextField
import com.acuranza.app.ui.common.LocalHideAmounts
import com.acuranza.app.ui.common.acuranzaViewModel
import com.acuranza.app.ui.common.bankEnum
import com.acuranza.app.ui.common.label
import com.acuranza.app.ui.home.TxnRow
import com.acuranza.app.ui.nav.Routes
import com.acuranza.app.model.TxnType
import com.acuranza.app.util.Time
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.acuranza.app.ui.nav.popBackSafely

// ---------------------------------------------------------------- list

/** An account the user ignored, with how much of its history totals leave out. */
data class IgnoredAccount(val account: AccountEntity, val transactions: Int, val latest: Long?)

class AccountsViewModel(private val c: AppContainer) : ViewModel() {
    val accounts: StateFlow<List<AccountWithBalance>?> =
        c.accounts.observeAccounts().map<List<AccountWithBalance>, List<AccountWithBalance>?> { it }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val ignored: StateFlow<List<IgnoredAccount>> =
        combine(c.accounts.observeIgnored(), c.db.transactions().observeIgnoredActivity()) { list, activity ->
            val byId = activity.associateBy { it.accountId }
            list.map { IgnoredAccount(it, byId[it.id]?.count ?: 0, byId[it.id]?.latest) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun restore(id: Long) = viewModelScope.launch { c.accounts.restore(id) }
}

@Composable
fun AccountsScreen(nav: NavController) {
    val vm = acuranzaViewModel { AccountsViewModel(it) }
    val accounts by vm.accounts.collectAsStateWithLifecycle()
    val ignored by vm.ignored.collectAsStateWithLifecycle()
    val hidden = LocalHideAmounts.current
    SumvaloScreen(
        title = "Accounts",
        onBack = { nav.popBackSafely() },
        actions = { IconButton(onClick = { nav.navigate(Routes.editAccount()) }) { Icon(Icons.Outlined.Add, contentDescription = "Add account") } },
    ) { padding ->
        val list = accounts ?: return@SumvaloScreen
        if (list.isEmpty() && ignored.isEmpty()) {
            EmptyState(
                "No accounts yet",
                "Add your bank accounts and credit cards to track balances.",
                Modifier.padding(padding).padding(top = 60.dp),
                icon = Icons.Outlined.AccountBalance,
                actionLabel = "Add account",
                onAction = { nav.navigate(Routes.editAccount()) },
            )
            return@SumvaloScreen
        }
        AccountsContent(
            list = list,
            ignored = ignored,
            hidden = hidden,
            modifier = Modifier.padding(padding),
            onAccount = { nav.navigate(Routes.account(it)) },
            onAdd = { nav.navigate(Routes.editAccount()) },
            onPending = { nav.navigate(Routes.PENDING_ACCOUNTS) },
            onRestore = { vm.restore(it) },
        )
    }
}

/** Stateless Accounts list; used by the app and by screenshot tests. */
@Composable
fun AccountsContent(
    list: List<AccountWithBalance>,
    ignored: List<IgnoredAccount>,
    hidden: Boolean,
    modifier: Modifier = Modifier,
    onAccount: (Long) -> Unit = {},
    onAdd: () -> Unit = {},
    onPending: () -> Unit = {},
    onRestore: (Long) -> Unit = {},
) {
    val pendingCount = list.count { it.isPending }
    val groups = list.filter { it.counted }.groupBy {
        when (it.account.type) {
            AccountType.BANK -> "Bank accounts"
            AccountType.CREDIT_CARD -> "Credit cards"
            AccountType.CASH, AccountType.WALLET -> "Cash & wallets"
        }
    }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 40.dp)) {
        if (pendingCount > 0) {
            item(key = "pending") {
                PendingAccountsBanner(pendingCount, onPending, Modifier.padding(bottom = 8.dp))
            }
        }
        item(key = "net-balance") {
            NetBalanceCard(
                bank = groups["Bank accounts"].orEmpty().sumOf { it.balance },
                cash = groups["Cash & wallets"].orEmpty().sumOf { it.balance },
                cardsOutstanding = groups["Credit cards"].orEmpty().sumOf { it.outstanding },
                hidden = hidden,
            )
        }
        listOf("Bank accounts", "Credit cards", "Cash & wallets").forEach { title ->
            val items = groups[title].orEmpty()
            if (items.isNotEmpty()) {
                item(key = title) {
                    val total = if (title == "Credit cards") -items.sumOf { it.outstanding } else items.sumOf { it.balance }
                    DateHeader(
                        title,
                        netPaise = total,
                        hidden = hidden,
                        netColor = if (title == "Credit cards") SumvaloTheme.colors.alert else SumvaloTheme.colors.subtleText,
                    )
                }
                item(key = title + "-card") {
                    // One bordered card per section, rows hairline-separated inside it.
                    SumvaloCard(contentPadding = PaddingValues(0.dp)) {
                        items.forEachIndexed { i, a ->
                            AccountRowFlat(a, hidden) { onAccount(a.id) }
                            if (i < items.lastIndex) {
                                Box(Modifier.fillMaxWidth().height(1.dp).background(SumvaloTheme.colors.divider))
                            }
                        }
                    }
                }
            }
        }
        if (ignored.isNotEmpty()) {
            item(key = "ignored") {
                Column(Modifier.padding(top = 20.dp, bottom = 4.dp)) {
                    Text("Ignored · not counted", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Sumvalo still reads their SMS but leaves them out of spending, balances and net worth.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SumvaloTheme.colors.subtleText,
                    )
                }
            }
            items(ignored, key = { "ignored-${it.account.id}" }) { ig -> IgnoredAccountRow(ig, onRestore = { onRestore(ig.account.id) }) }
        }
        item(key = "add") {
            AddAccountRow(Modifier.padding(top = 16.dp), onClick = onAdd)
        }
    }
}

@Composable
private fun IgnoredAccountRow(ig: IgnoredAccount, onRestore: () -> Unit) {
    SumvaloCard(Modifier.padding(vertical = 5.dp), contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BankBadge(ig.account.bankEnum(), size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(ig.account.label(), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    buildString {
                        append(ig.account.type.label())
                        if (ig.transactions > 0) {
                            append(" · ${ig.transactions} transaction${if (ig.transactions == 1) "" else "s"} left out")
                            ig.latest?.let { append(", latest ${Fmt.date(com.acuranza.app.util.Time.toDate(it))}") }
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = SumvaloTheme.colors.subtleText,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        com.acuranza.app.designsystem.components.SecondaryButton(
            text = if (ig.account.autoCreated) "Count again and review" else "Count again",
            onClick = onRestore,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Chakra-flavoured text style, matching the home/activity voice. */
private fun chakraText(weight: FontWeight, size: Int, lineHeight: Int, letterSpacingSp: Float = 0f): TextStyle =
    TextStyle(fontFamily = com.acuranza.app.ui.common.Chakra, fontWeight = weight, fontSize = size.sp, lineHeight = lineHeight.sp, letterSpacing = letterSpacingSp.sp)

/**
 * The reference's net-balance hero: total across every counted account, a Bank/Cash/Cards split
 * bar, and the three totals beside it — card debt in the coral role, as in the design.
 */
@Composable
private fun NetBalanceCard(bank: Long, cash: Long, cardsOutstanding: Long, hidden: Boolean) {
    val colors = SumvaloTheme.colors
    val net = bank + cash - cardsOutstanding
    val total = bank + cash + cardsOutstanding
    SumvaloCard(contentPadding = PaddingValues(16.dp)) {
        Text("Net balance", style = chakraText(FontWeight.Medium, 13, 18), color = colors.subtleText)
        Spacer(Modifier.height(2.dp))
        AmountText(net, style = chakraText(FontWeight.Bold, 30, 36), hidden = hidden)
        Spacer(Modifier.height(14.dp))
        if (total > 0) {
            Row(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50))) {
                val weight = { v: Long -> ((v.toFloat() / total) * 100f).coerceAtLeast(1f) }
                Box(Modifier.weight(weight(bank)).fillMaxHeight().background(colors.ink))
                Box(Modifier.weight(weight(cash)).fillMaxHeight().background(colors.tertiaryText))
                Box(Modifier.weight(weight(cardsOutstanding)).fillMaxHeight().background(colors.alert))
            }
            Spacer(Modifier.height(14.dp))
        }
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            AccountSplitColumn("Bank", bank, colors.ink, Color.Unspecified, hidden, Modifier.weight(1f))
            Box(Modifier.width(1.dp).fillMaxHeight().background(colors.hairline))
            AccountSplitColumn("Cash", cash, colors.tertiaryText, Color.Unspecified, hidden, Modifier.weight(1f).padding(start = 12.dp))
            Box(Modifier.width(1.dp).fillMaxHeight().background(colors.hairline))
            AccountSplitColumn("Cards", -cardsOutstanding, colors.alert, colors.alert, hidden, Modifier.weight(1f).padding(start = 12.dp))
        }
    }
}

@Composable
private fun AccountSplitColumn(label: String, value: Long, dot: Color, valueColor: Color, hidden: Boolean, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
            Spacer(Modifier.width(6.dp))
            Text(label, style = chakraText(FontWeight.Medium, 12, 16), color = SumvaloTheme.colors.tertiaryText)
        }
        Spacer(Modifier.height(2.dp))
        AmountText(value, style = chakraText(FontWeight.SemiBold, 14, 20), hidden = hidden, color = valueColor)
    }
}

/**
 * One account row inside its section card: badge tile, name with type/last4, and the balance —
 * coral with an "Overdrawn" pill when a bank account went negative, or the outstanding figure
 * with its limit-usage bar for cards.
 */
@Composable
private fun AccountRowFlat(a: AccountWithBalance, hidden: Boolean, onClick: () -> Unit) {
    val colors = SumvaloTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BankBadge(a.bank, size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                a.account.name,
                style = chakraText(FontWeight.SemiBold, 15, 20),
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                buildString {
                    append(a.account.type.label())
                    a.account.last4?.let { append(" ··$it") }
                    if (a.account.autoCreated) append(" · from SMS")
                },
                style = chakraText(FontWeight.Normal, 13, 18),
                color = colors.subtleText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            if (a.isCard) {
                AmountText(a.outstanding, style = chakraText(FontWeight.Bold, 15, 22), hidden = hidden)
                Text("outstanding", style = chakraText(FontWeight.Normal, 12, 16), color = colors.tertiaryText)
            } else {
                val overdrawn = a.balance < 0
                AmountText(
                    a.balance,
                    style = chakraText(FontWeight.Bold, 15, 22),
                    hidden = hidden,
                    color = if (overdrawn) colors.alert else Color.Unspecified,
                )
                if (overdrawn && !hidden) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Overdrawn",
                        style = chakraText(FontWeight.SemiBold, 11, 16),
                        color = colors.alert,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(colors.alert.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp),
                    )
                }
            }
        }
    }
    a.utilisation?.let { util ->
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 52.dp, end = 16.dp)
                .padding(bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(50)).background(colors.fill)) {
                Box(
                    Modifier
                        .fillMaxWidth(util.toFloat().coerceIn(0.02f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(if (util > 0.3) colors.warning else colors.good),
                )
            }
            Text(
                "${(util * 100).roundToInt()}% of limit used",
                style = chakraText(FontWeight.Medium, 11, 14),
                color = colors.tertiaryText,
                maxLines = 1,
            )
        }
    }
}

/** The reference's dashed add-account row; the edit form's type picker covers card/cash/wallet. */
@Composable
private fun AddAccountRow(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = SumvaloTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = colors.hairline,
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())),
                    ),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                )
            }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.fill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, tint = colors.ink, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text("Add account", style = chakraText(FontWeight.SemiBold, 15, 20), color = colors.ink)
            Text("Bank, card, cash or wallet", style = chakraText(FontWeight.Normal, 13, 18), color = colors.tertiaryText)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.tertiaryText, modifier = Modifier.size(18.dp))
    }
}

// ---------------------------------------------------------------- detail

data class AccountDetailState(
    val account: AccountWithBalance? = null,
    val transactions: List<TransactionItem> = emptyList(),
    val bills: List<BillItem> = emptyList(),
    val banks: List<AccountEntity> = emptyList(),
    val loaded: Boolean = false,
)

class AccountDetailViewModel(private val c: AppContainer, private val id: Long) : ViewModel() {
    val state: StateFlow<AccountDetailState> = combine(
        c.accounts.observeAccounts(),
        c.transactions.observe(TxnFilter(accountId = id)),
        c.bills.observeForCard(id),
    ) { accounts, txns, bills ->
        AccountDetailState(
            account = accounts.firstOrNull { it.id == id },
            transactions = txns.take(100),
            bills = bills,
            banks = accounts.filter { !it.isCard }.map { it.account },
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountDetailState())

    fun setBalance(amount: Long) = viewModelScope.launch { c.accounts.setCurrentBalance(id, amount) }
    fun archive(onDone: () -> Unit) = viewModelScope.launch {
        c.accounts.archive(id)
        onDone()
    }
    private val payGuard = com.acuranza.app.ui.common.ActionGuard()
    fun payBill(billId: Long, from: Long?, amount: Long?) = launchGuarded(payGuard) { c.bills.markPaid(billId, from, amount) }
    fun unpay(billId: Long) = viewModelScope.launch { c.bills.markUnpaid(billId) }
}

@Composable
fun AccountDetailScreen(nav: NavController, id: Long) {
    val vm = acuranzaViewModel(key = "acc-$id") { AccountDetailViewModel(it, id) }
    val state by vm.state.collectAsStateWithLifecycle()
    val hidden = LocalHideAmounts.current
    var reconcile by remember { mutableStateOf(false) }
    var archive by remember { mutableStateOf(false) }
    var paying by remember { mutableStateOf<BillItem?>(null) }
    val a = state.account

    SumvaloScreen(
        title = a?.account?.name ?: "Account",
        subtitle = a?.account?.type?.label(),
        onBack = { nav.popBackSafely() },
        actions = {
            if (a != null) {
                IconButton(onClick = { nav.navigate(Routes.editAccount(id)) }) { Icon(Icons.Outlined.Edit, contentDescription = "Edit") }
                IconButton(onClick = { archive = true }) { Icon(Icons.Outlined.Archive, contentDescription = "Archive") }
            }
        },
    ) { padding ->
        if (a == null) {
            if (state.loaded) EmptyState("Account not found", "It may have been archived.", Modifier.padding(padding).padding(top = 60.dp))
            return@SumvaloScreen
        }
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 40.dp)) {
            if (a.account.autoCreated) {
                item(key = "auto") {
                    SumvaloCard(Modifier.padding(bottom = 12.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                        Text("Found in your SMS · not counted yet", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Sumvalo spotted ${a.bank.shortName} ••${a.account.last4 ?: "?"} in your messages. Tell Sumvalo whether it's one of " +
                                "your accounts, a new one (with today's balance), or not yours. Until then it isn't in net worth.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SumvaloTheme.colors.subtleText,
                        )
                        TextButton(onClick = { nav.navigate(Routes.PENDING_ACCOUNTS) }) { Text("Sort it out") }
                    }
                }
            }
            item(key = "hero") {
                if (a.isCard) {
                    CreditCardVisual(
                        bank = a.bank,
                        cardName = a.account.name,
                        last4 = a.account.last4,
                        outstanding = a.outstanding,
                        creditLimit = a.account.creditLimit,
                        hidden = hidden,
                    )
                } else {
                    GradientCard {
                        Text("Current balance", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge)
                        AmountText(a.balance, style = AmountStyles.hero, hidden = hidden, animate = true, color = androidx.compose.ui.graphics.Color.White)
                        a.account.reportedAt?.let {
                            Text("Last confirmed by bank SMS ${Fmt.ago(it)}", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            item(key = "actions") {
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { reconcile = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.Tune, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (a.isCard) "Set outstanding" else "Set balance")
                    }
                    OutlinedButton(onClick = { nav.navigate(Routes.addTxn(TxnType.EXPENSE, id)) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Transaction")
                    }
                }
            }
            if (a.isCard) {
                item(key = "card-info") {
                    SumvaloCard(Modifier.padding(bottom = 8.dp)) {
                        Row {
                            CardStat("Available", a.availableLimit, hidden, Modifier.weight(1f))
                            CardStat("Limit", a.account.creditLimit, hidden, Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            buildString {
                                a.account.statementDay?.let { append("Statement on ${ordinal(it)}") }
                                a.account.dueDay?.let { if (isNotEmpty()) append(" · "); append("Due on ${ordinal(it)}") }
                                a.utilisation?.let { if (isNotEmpty()) append(" · "); append("${Fmt.pct(it * 100, 0)} used") }
                            }.ifEmpty { "Add statement and due days in edit" },
                            style = MaterialTheme.typography.bodySmall,
                            color = SumvaloTheme.colors.subtleText,
                        )
                    }
                }
                if (state.bills.isNotEmpty()) {
                    item(key = "bills-h") { SectionHeader("Bills") }
                    items(state.bills, key = { "b" + it.bill.id }) { b ->
                        BillRow(b, hidden, onPay = { paying = b }, onUnpay = { vm.unpay(b.bill.id) })
                    }
                }
            }
            item(key = "txn-h") { SectionHeader("Transactions") }
            if (state.transactions.isEmpty()) {
                item(key = "none") {
                    Text("No transactions yet", color = SumvaloTheme.colors.subtleText, modifier = Modifier.padding(8.dp))
                }
            }
            items(state.transactions, key = { "t" + it.txn.id }) { t ->
                TxnRow(t, hidden) { nav.navigate(Routes.txn(t.txn.id)) }
            }
        }
    }

    if (reconcile && a != null) {
        AmountSheet(
            title = if (a.isCard) "Amount you owe today" else "Balance in your bank app today",
            initial = if (a.isCard) a.outstanding else a.balance,
            allowZero = true,
            onDismiss = { reconcile = false },
            onConfirm = {
                vm.setBalance(it)
                reconcile = false
            },
        )
    }
    if (archive) {
        ConfirmDialog(
            "Archive account?",
            "It will be hidden and left out of spending, balances and net worth, including its future bank SMS. " +
                "Its transactions stay in your history, and Accounts › Ignored can count it again.",
            "Archive",
            onConfirm = {
                archive = false
                vm.archive { nav.popBackSafely() }
            },
            onDismiss = { archive = false },
        )
    }
    paying?.let { b ->
        PayBillDialog(b, state.banks, onDismiss = { paying = null }) { from, amount ->
            vm.payBill(b.bill.id, from, amount)
            paying = null
        }
    }
}

@Composable
private fun CardStat(label: String, value: Long?, hidden: Boolean, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = SumvaloTheme.colors.subtleText)
        if (value != null) AmountText(value, style = AmountStyles.medium, hidden = hidden) else Text("—")
    }
}

fun ordinal(day: Int): String = day.toString() + when {
    day in 11..13 -> "th"
    day % 10 == 1 -> "st"
    day % 10 == 2 -> "nd"
    day % 10 == 3 -> "rd"
    else -> "th"
}

// ---------------------------------------------------------------- edit

class AccountEditViewModel(private val c: AppContainer) : ViewModel() {
    suspend fun load(id: Long): AccountWithBalance? = c.accounts.byId(id)?.let { c.accounts.withBalance(it) }

    private val guard = com.acuranza.app.ui.common.ActionGuard()

    fun save(existing: AccountEntity?, draft: AccountEntity, currentAmount: Long, onDone: (Long) -> Unit) = launchGuarded(guard) {
        if (existing == null) {
            // For cards the opening balance is the amount owed; for others, money held.
            val id = c.accounts.add(draft.copy(openingBalance = currentAmount, openingDate = System.currentTimeMillis()))
            onDone(id)
        } else {
            c.accounts.update(
                existing.copy(
                    name = draft.name, type = draft.type, bank = draft.bank, last4 = draft.last4,
                    // Renaming an account found in SMS doesn't confirm it: it stays pending (and out of net worth)
                    // until the user answers the "is this yours?" prompt with a balance.
                    creditLimit = draft.creditLimit, statementDay = draft.statementDay, dueDay = draft.dueDay,
                ),
            )
            onDone(existing.id)
        }
    }
}

private val TYPES = listOf(AccountType.BANK, AccountType.CREDIT_CARD, AccountType.CASH, AccountType.WALLET)

@Composable
fun AccountEditScreen(nav: NavController, editId: Long?, defaultCard: Boolean) {
    val vm = acuranzaViewModel(key = "acc-edit-$editId") { AccountEditViewModel(it) }
    var existing by remember { mutableStateOf<AccountEntity?>(null) }
    var type by remember { mutableStateOf(if (defaultCard) AccountType.CREDIT_CARD else AccountType.BANK) }
    var bank by remember { mutableStateOf(Bank.HDFC) }
    var name by remember { mutableStateOf("") }
    var last4 by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf<Long?>(null) }
    var limit by remember { mutableStateOf<Long?>(null) }
    var statementDay by remember { mutableStateOf("") }
    var dueDay by remember { mutableStateOf("") }

    LaunchedEffect(editId) {
        if (editId != null) {
            val a = vm.load(editId) ?: return@LaunchedEffect
            existing = a.account
            type = a.account.type
            bank = Bank.fromName(a.account.bank)
            name = a.account.name
            last4 = a.account.last4.orEmpty()
            amount = if (a.isCard) a.outstanding else a.balance
            limit = a.account.creditLimit
            statementDay = a.account.statementDay?.toString().orEmpty()
            dueDay = a.account.dueDay?.toString().orEmpty()
        }
    }

    val isCard = type == AccountType.CREDIT_CARD
    val needsBank = type == AccountType.BANK || isCard
    val valid = name.isNotBlank() && (last4.isEmpty() || last4.length == 4)

    SumvaloScreen(title = if (existing == null) (if (isCard) "Add credit card" else "Add account") else "Edit account", onBack = { nav.popBackSafely() }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
        ) {
            FormColumn {
                SegmentedTabs(listOf("Bank", "Card", "Cash", "Wallet"), TYPES.indexOf(type), onSelect = { type = TYPES[it] })
                if (needsBank) {
                    Text("Bank", style = MaterialTheme.typography.labelLarge, color = SumvaloTheme.colors.subtleText)
                    BankPicker(bank) {
                        bank = it
                        if (name.isBlank() || Bank.entries.any { b -> name.startsWith(b.shortName) }) {
                            name = "${it.shortName} ${if (isCard) "Credit Card" else "Savings"}"
                        }
                    }
                }
                SumvaloTextField(name, { name = it }, "Name", supporting = "e.g. HDFC Savings, ICICI Amazon Pay")
                if (needsBank) {
                    SumvaloTextField(
                        last4,
                        { v -> last4 = v.filter(Char::isDigit).take(4) },
                        if (isCard) "Last 4 digits of card" else "Last 4 digits of account",
                        keyboardType = KeyboardType.Number,
                        supporting = "Used to match bank SMS to this account",
                    )
                }
                if (existing == null) {
                    AmountField(if (isCard) "Amount you owe now" else "Current balance", amount, { amount = it })
                } else {
                    Text(
                        "To change the balance, use “Set balance” on the account screen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SumvaloTheme.colors.subtleText,
                    )
                }
                if (isCard) {
                    AmountField("Credit limit", limit, { limit = it })
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SumvaloTextField(statementDay, { v -> statementDay = v.filter(Char::isDigit).take(2) }, "Statement day",
                            Modifier.weight(1f), keyboardType = KeyboardType.Number)
                        SumvaloTextField(dueDay, { v -> dueDay = v.filter(Char::isDigit).take(2) }, "Due day",
                            Modifier.weight(1f), keyboardType = KeyboardType.Number)
                    }
                }
                Spacer(Modifier.height(8.dp))
                PrimaryButton(
                    text = if (existing == null) "Add" else "Save",
                    enabled = valid,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val draft = AccountEntity(
                            name = name.trim(),
                            type = type,
                            bank = if (needsBank) bank.name else Bank.OTHER.name,
                            last4 = last4.takeIf { it.length == 4 },
                            creditLimit = if (isCard) limit else null,
                            statementDay = if (isCard) statementDay.toIntOrNull()?.coerceIn(1, 31) else null,
                            dueDay = if (isCard) dueDay.toIntOrNull()?.coerceIn(1, 31) else null,
                        )
                        vm.save(existing, draft, amount ?: 0) { nav.popBackSafely() }
                    },
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/** Horizontal bank chooser with brand badges. */
@Composable
fun BankPicker(selected: Bank, onSelect: (Bank) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(Bank.entries.toList(), key = { it.name }) { b ->
            val isSel = b == selected
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else SumvaloTheme.colors.cardSurface,
                border = if (isSel) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, SumvaloTheme.colors.divider),
                modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { onSelect(b) },
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    BankBadge(b, size = 36.dp)
                    Spacer(Modifier.height(6.dp))
                    Text(b.shortName, style = MaterialTheme.typography.labelMedium, fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
    }
}

