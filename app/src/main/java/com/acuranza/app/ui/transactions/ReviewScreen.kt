package com.acuranza.app.ui.transactions

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.acuranza.app.data.CategoryEntity
import com.acuranza.app.data.tombstones
import com.acuranza.app.designsystem.components.EmptyState
import com.acuranza.app.designsystem.components.SumvaloCard
import com.acuranza.app.designsystem.components.SourceBadge
import com.acuranza.app.designsystem.theme.SumvaloTheme
import com.acuranza.app.di.AppContainer
import com.acuranza.app.domain.SmsMapping
import com.acuranza.app.repo.TransactionItem
import com.acuranza.app.ui.common.Fmt
import com.acuranza.app.ui.common.SumvaloScreen
import com.acuranza.app.ui.common.LocalHideAmounts
import com.acuranza.app.ui.common.acuranzaViewModel
import com.acuranza.app.ui.common.rememberHaptic
import com.acuranza.app.ui.home.TxnRow
import com.acuranza.app.ui.nav.Routes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.acuranza.app.ui.nav.popBackSafely

data class ReviewState(val items: List<TransactionItem> = emptyList(), val categories: List<CategoryEntity> = emptyList(), val loaded: Boolean = false)

class ReviewViewModel(private val c: AppContainer) : ViewModel() {
    val state: StateFlow<ReviewState> = combine(c.transactions.observeUnreviewed(), c.categories.observeAll()) { items, cats ->
        ReviewState(items, cats, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReviewState())

    fun confirm(id: Long) = viewModelScope.launch { c.transactions.markReviewed(id) }

    /** Reports how many rows the single atomic UPDATE actually approved. */
    fun confirmAll(onResult: (Int) -> Unit) = viewModelScope.launch {
        onResult(c.transactions.markAllReviewed())
    }
    fun delete(id: Long) = viewModelScope.launch { c.tombstones.deleteTransaction(id) }
    fun recategorise(id: Long, key: String, all: Boolean) = viewModelScope.launch { c.transactions.recategorise(id, key, all) }
    fun markTransfer(id: Long, otherAccountId: Long?) = viewModelScope.launch { c.transactions.markSelfTransfer(id, otherAccountId) }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ReviewScreen(nav: NavController) {
    val vm = acuranzaViewModel { ReviewViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val hidden = LocalHideAmounts.current
    val haptic = rememberHaptic()
    val snackbar = remember { androidx.compose.material3.SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<TransactionItem?>(null) }
    var transferring by remember { mutableStateOf<TransactionItem?>(null) }

    SumvaloScreen(
        title = "Review",
        subtitle = if (state.items.isEmpty()) null else "${state.items.size} imported from SMS & email",
        onBack = { nav.popBackSafely() },
        snackbar = snackbar,
        actions = {
            if (state.items.isNotEmpty()) {
                TextButton(onClick = {
                    haptic()
                    vm.confirmAll { approved ->
                        scope.launch {
                            snackbar.showSnackbar(
                                if (approved > 0) "Confirmed $approved transaction" + (if (approved > 1) "s" else "")
                                else "Nothing left to confirm",
                            )
                        }
                    }
                }) {
                    Icon(Icons.Outlined.DoneAll, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text("Confirm all")
                }
            }
        },
    ) { padding ->
        if (state.loaded && state.items.isEmpty()) {
            EmptyState(
                title = "All caught up",
                message = "New transactions from your bank SMS and card emails will appear here for a quick check.",
                icon = Icons.Outlined.TaskAlt,
                modifier = Modifier.padding(padding).padding(top = 60.dp),
                actionLabel = "Back to home",
                onAction = { nav.popBackSafely() },
            )
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.items, key = { it.txn.id }) { t ->
                    SumvaloCard(Modifier.animateContentSize(), contentPadding = PaddingValues(12.dp)) {
                        if (t.txn.suspicious) {
                            Text(
                                "This amount looks unusually large, maybe a reference number read as an amount. It isn't " +
                                    "counted in balances or totals until you confirm it — \"Looks right\" or \"Confirm all\" clears the flag.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SumvaloTheme.colors.warning,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SumvaloTheme.colors.warning.copy(alpha = 0.10f))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                            )
                        }
                        TxnRow(t, hidden) { nav.navigate(Routes.txn(t.txn.id)) }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SourceBadge(t.txn.source)
                            Spacer(Modifier.size(8.dp))
                            Text(
                                "${Fmt.dayLabel(t.date)} · ${t.category?.name ?: t.txn.categoryKey}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SumvaloTheme.colors.subtleText,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        FlowRow(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp),
                        ) {
                            ReviewAction(if (t.txn.suspicious) "Looks right" else "Confirm", Icons.Outlined.Check, SumvaloTheme.colors.good) {
                                haptic()
                                vm.confirm(t.txn.id)
                            }
                            ReviewAction("Category", Icons.Outlined.Category, MaterialTheme.colorScheme.primary) { editing = t }
                            ReviewAction("Transfer", Icons.Outlined.SwapHoriz, SumvaloTheme.colors.transfer) { transferring = t }
                            ReviewAction("Delete", Icons.Outlined.DeleteOutline, SumvaloTheme.colors.alert) { vm.delete(t.txn.id) }
                        }
                    }
                }
            }
        }
    }

    transferring?.let { t ->
        SelfTransferSheet(
            currentAccountId = t.txn.accountId,
            isIncome = t.txn.type == com.acuranza.app.model.TxnType.INCOME,
            onDismiss = { transferring = null },
            onPick = { other ->
                vm.markTransfer(t.txn.id, other)
                transferring = null
            },
        )
    }

    editing?.let { t ->
        RecategoriseSheet(
            categories = state.categories.filter { it.type == t.txn.type || t.txn.type == com.acuranza.app.model.TxnType.TRANSFER },
            currentKey = t.txn.categoryKey,
            merchant = t.txn.merchant,
            onDismiss = { editing = null },
            onPick = { key, all ->
                vm.recategorise(t.txn.id, key, all)
                editing = null
            },
        )
    }
}

@Composable
private fun ReviewAction(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(4.dp))
        Text(
            label,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}

