package com.acuranza.app.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.acuranza.app.data.AccountEntity
import com.acuranza.app.data.CategoryEntity
import com.acuranza.app.data.TransactionEntity
import com.acuranza.app.data.Tags
import com.acuranza.app.designsystem.components.AmountInputDisplay
import com.acuranza.app.designsystem.components.BankBadge
import com.acuranza.app.designsystem.components.CategoryIcon
import com.acuranza.app.designsystem.components.KeypadExpression
import com.acuranza.app.designsystem.components.NumberKeypad
import com.acuranza.app.designsystem.components.PrimaryButton
import com.acuranza.app.ui.common.Pill
import com.acuranza.app.designsystem.components.SegmentedTabs
import com.acuranza.app.designsystem.theme.SumvaloTheme
import com.acuranza.app.di.AppContainer
import com.acuranza.app.domain.SmsMapping
import com.acuranza.app.model.TxnSource
import com.acuranza.app.model.TxnType
import com.acuranza.app.ui.common.ChoiceField
import com.acuranza.app.ui.common.ConfirmDialog
import com.acuranza.app.ui.common.DateField
import com.acuranza.app.ui.common.FieldBox
import com.acuranza.app.ui.common.SumvaloScreen
import com.acuranza.app.ui.common.SumvaloTextField
import com.acuranza.app.ui.common.bankEnum
import com.acuranza.app.ui.common.color
import com.acuranza.app.data.tombstones
import com.acuranza.app.ui.common.acuranzaViewModel
import com.acuranza.app.ui.common.launchGuarded
import com.acuranza.app.ui.nav.popPast
import com.acuranza.app.ui.common.label
import com.acuranza.app.ui.common.rememberHaptic
import com.acuranza.app.ui.common.toRupeeInput
import com.acuranza.app.util.Time
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import com.acuranza.app.ui.nav.popBackSafely
import androidx.compose.ui.text.style.TextOverflow

data class EditorRefs(val accounts: List<AccountEntity> = emptyList(), val categories: List<CategoryEntity> = emptyList(), val tags: List<String> = emptyList())

class AddTransactionViewModel(private val c: AppContainer) : ViewModel() {
    val refs: StateFlow<EditorRefs> = combine(c.accounts.observeAccounts(), c.categories.observeAll(), c.transactions.observeAllTags()) { a, cats, tags ->
        EditorRefs(a.map { it.account }, cats, tags)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EditorRefs())

    suspend fun load(id: Long): TransactionEntity? = c.transactions.byId(id)

    private val guard = com.acuranza.app.ui.common.ActionGuard()

    fun save(existing: TransactionEntity?, draft: TransactionEntity, onDone: () -> Unit) = launchGuarded(guard) {
        if (draft.type == TxnType.TRANSFER) {
            val to = draft.toAccountId ?: return@launchGuarded
            if (existing == null) {
                c.transactions.addTransfer(draft.accountId, to, draft.amount, draft.timestamp, draft.note)
            } else {
                // Keep the imported row's fingerprint, reference and channel, so the original SMS is still
                // recognised as already imported and isn't added again as a new expense.
                c.transactions.update(
                    existing.copy(
                        accountId = draft.accountId, toAccountId = to, type = TxnType.TRANSFER, amount = draft.amount,
                        categoryKey = SmsMapping.TRANSFER_CATEGORY, merchant = draft.merchant, note = draft.note,
                        timestamp = draft.timestamp, excludeFromStats = true, reviewed = true,
                    ),
                )
            }
        } else if (existing == null) {
            c.transactions.add(draft)
        } else {
            c.transactions.update(
                existing.copy(
                    accountId = draft.accountId, toAccountId = null, type = draft.type, amount = draft.amount,
                    categoryKey = draft.categoryKey, merchant = draft.merchant, note = draft.note,
                    timestamp = draft.timestamp, reviewed = true,
                    excludeFromStats = draft.categoryKey == SmsMapping.TRANSFER_CATEGORY,
                ),
            )
        }
        onDone()
    }

    /** Deletes the transaction; an imported one is remembered so a later inbox scan doesn't bring it back. */
    fun delete(id: Long, onDone: () -> Unit) = launchGuarded(guard) {
        c.tombstones.deleteTransaction(id)
        onDone()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(nav: NavController, editId: Long?, initialType: TxnType, initialAccountId: Long?) {
    val vm = acuranzaViewModel(key = "add-$editId") { AddTransactionViewModel(it) }
    val refs by vm.refs.collectAsStateWithLifecycle()
    AddTransactionContent(
        refs = refs,
        editId = editId,
        initialType = initialType,
        initialAccountId = initialAccountId,
        load = { vm.load(it) },
        onSave = { existing, draft -> vm.save(existing, draft) { nav.popBackSafely() } },
        // After deleting, also leave the detail screen the editor was opened from: it would show "not found".
        onDelete = { id -> vm.delete(id) { nav.popPast(com.acuranza.app.ui.nav.Routes.TXN_DETAIL) } },
        onBack = { nav.popBackSafely() },
    )
}

/**
 * Stateless (data-in, events-out) transaction editor; the form state lives here.
 * [initialAmount] pre-fills the keypad, e.g. for screenshot tests.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionContent(
    refs: EditorRefs,
    editId: Long?,
    initialType: TxnType,
    initialAccountId: Long?,
    load: suspend (Long) -> TransactionEntity? = { null },
    onSave: (TransactionEntity?, TransactionEntity) -> Unit = { _, _ -> },
    onDelete: (Long) -> Unit = {},
    onBack: () -> Unit = {},
    initialAmount: String = "",
    initialCategoryKey: String? = null,
    initialMerchant: String = "",
) {
    val haptic = rememberHaptic()

    var existing by remember { mutableStateOf<TransactionEntity?>(null) }
    var type by remember { mutableStateOf(initialType) }
    val expr = remember { KeypadExpression().apply { initialAmount.forEach { press(it.toString()) } } }
    var exprText by remember { mutableStateOf(expr.text) }
    var categoryKey by remember { mutableStateOf(initialCategoryKey) }
    var accountId by remember { mutableStateOf(initialAccountId) }
    var toAccountId by remember { mutableStateOf<Long?>(null) }
    var date by remember { mutableStateOf(Time.today()) }
    var timeOfDay by remember { mutableStateOf(java.time.LocalTime.now()) }
    var merchant by remember { mutableStateOf(initialMerchant) }
    var note by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf(emptySet<String>()) }
    var pickCategory by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(editId) {
        if (editId != null) {
            val t = load(editId) ?: return@LaunchedEffect
            existing = t
            type = t.type
            exprText = expr.apply { press("C"); t.amount.toRupeeInput().forEach { ch -> press(ch.toString()) } }.text
            categoryKey = t.categoryKey
            accountId = t.accountId
            toAccountId = t.toAccountId
            val dt = Instant.ofEpochMilli(t.timestamp).atZone(ZoneId.systemDefault())
            date = dt.toLocalDate()
            timeOfDay = dt.toLocalTime()
            merchant = t.merchant.orEmpty()
            note = t.note.orEmpty()
            tags = com.acuranza.app.data.Tags.decode(t.tags).toMutableSet()
        }
    }
    // Default account: the first bank account.
    LaunchedEffect(refs.accounts) {
        if (accountId == null) accountId = refs.accounts.firstOrNull { it.type != com.acuranza.app.model.AccountType.CREDIT_CARD }?.id
            ?: refs.accounts.firstOrNull()?.id
    }

    val catsForType = refs.categories.filter { it.type == (if (type == TxnType.INCOME) TxnType.INCOME else TxnType.EXPENSE) }
    val category = refs.categories.firstOrNull { it.key == categoryKey }
    val account = refs.accounts.firstOrNull { it.id == accountId }
    val toAccount = refs.accounts.firstOrNull { it.id == toAccountId }
    val value = remember(exprText) { expr.valuePaise() }
    val canSave = value != null && value > 0 && account != null &&
        (if (type == TxnType.TRANSFER) toAccount != null && toAccount.id != account.id else category != null)

    SumvaloScreen(
        title = if (existing == null) "New transaction" else "Edit transaction",
        onBack = onBack,
        actions = {
            if (existing != null) {
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete") }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .navigationBarsPadding(),
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SegmentedTabs(
                    options = listOf("Expense", "Income", "Transfer"),
                    selectedIndex = type.ordinal,
                    onSelect = {
                        val newType = TxnType.entries[it]
                        if (newType != type) {
                            type = newType
                            if (category?.type != newType) categoryKey = null
                        }
                    },
                )
                AmountInputDisplay(expression = exprText, valuePaise = value, type = type, showResult = expr.hasOperator)
                if (type == TxnType.TRANSFER) {
                    AccountChoice("From", refs.accounts, account) { accountId = it.id }
                    AccountChoice("To", refs.accounts.filter { it.id != accountId }, toAccount) { toAccountId = it.id }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FieldBox(label = "Category", modifier = Modifier.weight(1f), onClick = { pickCategory = true }) {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                if (category != null) {
                                    CategoryIcon(category.icon, category.color(), size = 24.dp)
                                    Spacer(Modifier.padding(start = 6.dp))
                                }
                                Text(category?.name ?: "Choose", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        AccountChoice("Account", refs.accounts, account, Modifier.weight(1f)) { accountId = it.id }
                    }
                    SumvaloTextField(merchant, { merchant = it }, if (type == TxnType.INCOME) "From (payer)" else "Paid to (merchant)")
                }
                DateField("Date", date, { date = it })
                SumvaloTextField(note, { note = it }, "Note (optional)")
                if (refs.tags.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text("Tags", style = MaterialTheme.typography.labelSmall, color = SumvaloTheme.colors.subtleText)
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        refs.tags.sorted().forEach { tag ->
                            val selected = tags.contains(tag)
                            Pill(
                                if (selected) "✓ $tag" else tag,
                                if (selected) SumvaloTheme.colors.accent else SumvaloTheme.colors.fill,
                                modifier = Modifier.clickable { if (selected) tags -= tag else tags += tag },
                            )
                        }
                    }
                }
                if (existing != null) {
                    Text(
                        existing!!.source.label(),
                        style = MaterialTheme.typography.labelSmall,
                        color = SumvaloTheme.colors.subtleText,
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
            NumberKeypad(onKey = { exprText = expr.press(it) }, modifier = Modifier.padding(horizontal = 12.dp))
            PrimaryButton(
                text = if (existing == null) "Save" else "Update",
                icon = Icons.Outlined.Check,
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                onClick = {
                    haptic()
                    val ts = date.atTime(timeOfDay).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val tagList = Tags.encode(tags)
                    val draft = TransactionEntity(
                        accountId = account!!.id,
                        toAccountId = if (type == TxnType.TRANSFER) toAccountId else null,
                        type = type,
                        amount = value!!,
                        categoryKey = if (type == TxnType.TRANSFER) SmsMapping.TRANSFER_CATEGORY else category!!.key,
                        merchant = merchant.trim().ifEmpty { null },
                        note = note.trim().ifEmpty { null },
                        timestamp = ts,
                        source = existing?.source ?: TxnSource.MANUAL,
                        tags = tagList,
                    )
                    onSave(existing, draft)
                },
            )
        }
    }

    if (pickCategory) {
        ModalBottomSheet(onDismissRequest = { pickCategory = false }) {
            Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding()) {
                Text("Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                CategoryGrid(catsForType, categoryKey, onSelect = {
                    categoryKey = it.key
                    pickCategory = false
                })
                Spacer(Modifier.height(16.dp))
            }
        }
    }
    if (confirmDelete && existing != null) {
        ConfirmDialog(
            title = "Delete transaction?",
            message = "This can't be undone.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                confirmDelete = false
                onDelete(existing!!.id)
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
fun AccountChoice(
    label: String,
    accounts: List<AccountEntity>,
    selected: AccountEntity?,
    modifier: Modifier = Modifier,
    onSelect: (AccountEntity) -> Unit,
) {
    ChoiceField(
        label = label,
        options = accounts,
        selected = selected,
        optionLabel = { it.label() },
        onSelect = onSelect,
        modifier = modifier,
        placeholder = if (accounts.isEmpty()) "Add an account first" else "Choose",
        leading = { BankBadge(it.bankEnum(), size = 28.dp) },
    )
}

