package com.acuranza.app.ui.bills

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CreditScore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.acuranza.app.data.AccountEntity
import com.acuranza.app.designsystem.components.AmountText
import com.acuranza.app.designsystem.components.BankBadge
import com.acuranza.app.designsystem.components.EmptyState
import com.acuranza.app.designsystem.components.SumvaloCard
import com.acuranza.app.designsystem.components.PrimaryButton
import com.acuranza.app.designsystem.components.SourceBadge
import com.acuranza.app.designsystem.theme.AmountStyles
import com.acuranza.app.designsystem.theme.SumvaloTheme
import com.acuranza.app.di.AppContainer
import com.acuranza.app.model.AccountType
import com.acuranza.app.model.Bank
import com.acuranza.app.repo.BillItem
import com.acuranza.app.ui.common.AmountField
import com.acuranza.app.ui.common.ChoiceField
import com.acuranza.app.ui.common.DateField
import com.acuranza.app.ui.common.Fmt
import com.acuranza.app.ui.common.FormColumn
import com.acuranza.app.ui.common.SumvaloScreen
import com.acuranza.app.ui.common.LocalHideAmounts
import com.acuranza.app.ui.common.Pill
import com.acuranza.app.ui.common.bankEnum
import com.acuranza.app.ui.common.acuranzaViewModel
import com.acuranza.app.ui.common.launchGuarded
import com.acuranza.app.ui.common.label
import com.acuranza.app.ui.common.rememberHaptic
import com.acuranza.app.util.Time
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import com.acuranza.app.ui.nav.popBackSafely

data class BillsState(
    val bills: List<BillItem> = emptyList(),
    val cards: List<AccountEntity> = emptyList(),
    val banks: List<AccountEntity> = emptyList(),
    val loaded: Boolean = false,
)

class BillsViewModel(private val c: AppContainer) : ViewModel() {
    val state: StateFlow<BillsState> = combine(c.bills.observeUnpaid(), c.accounts.observeAccounts()) { bills, accounts ->
        BillsState(
            bills = bills,
            cards = accounts.filter { it.isCard }.map { it.account },
            banks = accounts.filter { it.account.type == AccountType.BANK || it.account.type == AccountType.WALLET }.map { it.account },
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BillsState())

    private val guard = com.acuranza.app.ui.common.ActionGuard()

    fun pay(billId: Long, from: Long?, amount: Long?) = launchGuarded(guard) { c.bills.markPaid(billId, from, amount) }
    fun add(cardId: Long, total: Long, min: Long?, due: LocalDate) = launchGuarded(guard) { c.bills.addManual(cardId, total, min, due) }
    fun delete(id: Long) = viewModelScope.launch { c.bills.delete(id) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillsScreen(nav: NavController) {
    val vm = acuranzaViewModel { BillsViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val hidden = LocalHideAmounts.current
    var paying by remember { mutableStateOf<BillItem?>(null) }
    var adding by remember { mutableStateOf(false) }

    BillsContent(
        state = state,
        hidden = hidden,
        onBack = { nav.popBackSafely() },
        onAdd = { adding = true },
        onPay = { paying = it },
        onDelete = { vm.delete(it.bill.id) },
    )

    paying?.let { b ->
        PayBillDialog(b, state.banks, onDismiss = { paying = null }) { from, amount ->
            vm.pay(b.bill.id, from, amount)
            paying = null
        }
    }
    if (adding) {
        var card by remember { mutableStateOf(state.cards.firstOrNull()) }
        var total by remember { mutableStateOf<Long?>(null) }
        var min by remember { mutableStateOf<Long?>(null) }
        var due by remember { mutableStateOf(Time.today().plusDays(15)) }
        ModalBottomSheet(onDismissRequest = { adding = false }) {
            Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding()) {
                Text("Add a bill", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                FormColumn {
                    ChoiceField("Card", state.cards, card, { it.label() }, { card = it }, leading = { BankBadge(it.bankEnum(), size = 28.dp) })
                    AmountField("Total due", total, { total = it })
                    AmountField("Minimum due (optional)", min, { min = it })
                    DateField("Due date", due, { due = it })
                    PrimaryButton("Add bill", enabled = card != null && (total ?: 0) > 0, modifier = Modifier.fillMaxWidth(), onClick = {
                        vm.add(card!!.id, total!!, min, due)
                        adding = false
                    })
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/** Stateless Card bills screen; used by the app and by screenshot tests. */
@Composable
fun BillsContent(
    state: BillsState,
    hidden: Boolean,
    onBack: () -> Unit = {},
    onAdd: () -> Unit = {},
    onPay: (BillItem) -> Unit = {},
    onDelete: (BillItem) -> Unit = {},
) {
    SumvaloScreen(
        title = "Card bills",
        subtitle = if (state.bills.isEmpty()) null else "Total due ${com.acuranza.app.model.Money.format(state.bills.sumOf { it.bill.totalDue }, showPaise = false)}"
            .takeUnless { hidden },
        onBack = onBack,
        actions = {
            if (state.cards.isNotEmpty()) IconButton(onClick = onAdd) { Icon(Icons.Outlined.Add, contentDescription = "Add bill") }
        },
    ) { padding ->
        if (state.loaded && state.bills.isEmpty()) {
            EmptyState(
                title = "No bills due",
                message = if (state.cards.isEmpty()) {
                    "Add a credit card to track its bills. Statements are read from your card SMS."
                } else {
                    "You're all paid up. New statements from your card SMS will show here with due dates."
                },
                icon = Icons.Outlined.CreditScore,
                modifier = Modifier.padding(padding).padding(top = 60.dp),
                actionLabel = if (state.cards.isEmpty()) null else "Add a bill manually",
                onAction = if (state.cards.isEmpty()) null else onAdd,
            )
            return@SumvaloScreen
        }
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp, 4.dp, 20.dp, 40.dp)) {
            items(state.bills, key = { it.bill.id }) { b -> BillRow(b, hidden, onPay = { onPay(b) }, onDelete = { onDelete(b) }) }
        }
    }
}

/** A bill row with urgency colour, due countdown and actions. */
@Composable
fun BillRow(b: BillItem, hidden: Boolean, onPay: () -> Unit, onUnpay: (() -> Unit)? = null, onDelete: (() -> Unit)? = null) {
    val days = b.daysLeft
    val urgency = when {
        b.bill.paid -> SumvaloTheme.colors.good
        days == null -> SumvaloTheme.colors.subtleText
        days < 0 -> SumvaloTheme.colors.alert
        days <= 3 -> SumvaloTheme.colors.warning
        else -> SumvaloTheme.colors.subtleText
    }
    SumvaloCard(Modifier.padding(vertical = 5.dp), contentPadding = PaddingValues(start = 14.dp, end = 6.dp, top = 14.dp, bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BankBadge(b.card?.bankEnum() ?: Bank.OTHER, size = 36.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(b.card?.label() ?: "Card", fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        when {
                            b.bill.paid -> "Paid${b.bill.paidAt?.let { " · " + Fmt.date(Time.toDate(it)) } ?: ""}"
                            else -> b.dueDate?.let { "${Fmt.relativeDays(days ?: 0)} · ${Fmt.date(it)}" } ?: "No due date"
                        },
                        color = urgency,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                AmountText(b.bill.totalDue, style = AmountStyles.medium, hidden = hidden)
                b.bill.minimumDue?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("min ", style = MaterialTheme.typography.labelSmall, color = SumvaloTheme.colors.subtleText)
                        AmountText(it, style = AmountStyles.small, hidden = hidden, color = SumvaloTheme.colors.subtleText)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SourceBadge(b.bill.source)
            if (b.isOverdue) {
                Spacer(Modifier.width(8.dp))
                Pill("Overdue", SumvaloTheme.colors.alert)
            }
            Spacer(Modifier.weight(1f))
            if (onDelete != null) TextButton(onClick = onDelete) { Text("Remove", color = SumvaloTheme.colors.subtleText) }
            if (b.bill.paid) {
                if (onUnpay != null) TextButton(onClick = onUnpay) { Text("Mark unpaid") }
            } else {
                TextButton(onClick = onPay) { Text("Mark paid", fontWeight = FontWeight.SemiBold, color = SumvaloTheme.colors.accent) }
            }
        }
    }
}

/** Asks which account paid the bill and how much; records a transfer so both balances update. */
@Composable
fun PayBillDialog(b: BillItem, banks: List<AccountEntity>, onDismiss: () -> Unit, onPay: (fromAccountId: Long?, amount: Long?) -> Unit) {
    val haptic = rememberHaptic()
    var from by remember { mutableStateOf(banks.firstOrNull()) }
    var amount by remember { mutableStateOf<Long?>(b.bill.totalDue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mark bill paid") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Choose the account you paid from to update both balances, or skip if it's already recorded.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SumvaloTheme.colors.subtleText,
                )
                if (banks.isNotEmpty()) {
                    ChoiceField("Paid from", banks, from, { it.label() }, { from = it }, leading = { BankBadge(it.bankEnum(), size = 28.dp) })
                    AmountField("Amount paid", amount, { amount = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                haptic()
                onPay(from?.id, amount)
            }) { Text(if (from != null) "Record payment" else "Mark paid") }
        },
        dismissButton = {
            Row {
                if (from != null) TextButton(onClick = { onPay(null, null) }) { Text("Just mark paid") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
