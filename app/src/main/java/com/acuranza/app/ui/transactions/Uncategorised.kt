package com.acuranza.app.ui.transactions

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.acuranza.app.data.CategoryEntity
import com.acuranza.app.data.MerchantRuleEntity
import com.acuranza.app.data.TransactionEntity
import com.acuranza.app.designsystem.components.AmountText
import com.acuranza.app.designsystem.components.EmptyState
import com.acuranza.app.designsystem.components.AcuranzaCard
import com.acuranza.app.designsystem.components.SecondaryButton
import com.acuranza.app.designsystem.theme.AmountStyles
import com.acuranza.app.designsystem.theme.AcuranzaTheme
import com.acuranza.app.di.AppContainer
import com.acuranza.app.domain.SmsMapping
import com.acuranza.app.model.Paise
import com.acuranza.app.model.TxnSource
import com.acuranza.app.model.TxnType
import com.acuranza.app.ui.common.Fmt
import com.acuranza.app.ui.common.AcuranzaScreen
import com.acuranza.app.ui.common.LocalHideAmounts
import com.acuranza.app.ui.common.acuranzaViewModel
import com.acuranza.app.util.Time
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.acuranza.app.ui.nav.popBackSafely

/** Uncategorised transactions to the same payee, fixed together with one tap. */
data class PayeeGroup(
    val key: String,
    val label: String,
    val merchant: String?,
    val transactions: List<TransactionEntity>,
) {
    val total: Paise get() = transactions.sumOf { it.amount }
    val count: Int get() = transactions.size
    val latest: Long get() = transactions.maxOf { it.timestamp }
}

data class UncategorisedState(
    val groups: List<PayeeGroup> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val loaded: Boolean = false,
)

/** Groups "Other" expenses by payee: same merchant spelled differently shares a group; no merchant goes last. */
internal fun groupByPayee(rows: List<TransactionEntity>): List<PayeeGroup> =
    rows.groupBy { t -> t.merchant?.let { SmsMapping.merchantKey(it) }?.takeIf { it.isNotEmpty() } ?: UNKNOWN_KEY }
        .map { (key, txns) ->
            val merchant = txns.firstNotNullOfOrNull { it.merchant }
            PayeeGroup(key, if (key == UNKNOWN_KEY) "Unknown payee" else merchant ?: key, if (key == UNKNOWN_KEY) null else merchant, txns)
        }
        .sortedWith(compareBy<PayeeGroup> { it.key == UNKNOWN_KEY }.thenByDescending { it.total })

private const val UNKNOWN_KEY = "\u0001unknown"

class UncategorisedViewModel(private val c: AppContainer) : ViewModel() {
    val state: StateFlow<UncategorisedState> = combine(
        c.db.transactions().observeUncategorised(),
        c.categories.observeAll(),
    ) { rows, cats ->
        UncategorisedState(groupByPayee(rows), cats.filter { it.type == TxnType.EXPENSE }, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UncategorisedState())

    /** Assigns [categoryKey] to every transaction in the group and, for a named payee, remembers it for future imports. */
    fun assign(group: PayeeGroup, categoryKey: String) = viewModelScope.launch {
        val dao = c.db.transactions()
        group.merchant?.let { c.db.merchantRules().upsert(MerchantRuleEntity(merchant = it, categoryKey = categoryKey)) }
        group.transactions.forEach { t ->
            dao.update(t.copy(categoryKey = categoryKey, reviewed = true, excludeFromStats = categoryKey == SmsMapping.TRANSFER_CATEGORY))
        }
    }
}

/** "N transactions are uncategorised · Fix": shown on Home and Activity. */
@Composable
fun UncategorisedBanner(count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    if (count <= 0) return
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = AcuranzaTheme.colors.cardSurfaceHigh,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Category, contentDescription = null, tint = AcuranzaTheme.colors.subtleText)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("$count transactions in \"Other\" this month", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("Fix them by payee in one tap", style = MaterialTheme.typography.bodySmall, color = AcuranzaTheme.colors.subtleText)
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Fix uncategorised", tint = AcuranzaTheme.colors.subtleText)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UncategorisedScreen(nav: NavController) {
    val vm = acuranzaViewModel { UncategorisedViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val hidden = LocalHideAmounts.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var picking by remember { mutableStateOf<PayeeGroup?>(null) }

    AcuranzaScreen(
        title = "Fix uncategorised",
        subtitle = "Pick a category once per payee",
        onBack = { nav.popBackSafely() },
        snackbar = snackbar,
    ) { padding ->
        if (state.loaded && state.groups.isEmpty()) {
            EmptyState(
                "Nothing in Other",
                "Every imported transaction has a category.",
                Modifier.padding(padding).padding(top = 60.dp),
            )
            return@AcuranzaScreen
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp, 4.dp, 20.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "intro") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Sumvalo couldn't tell what these were for. Choose a category for a payee and every transaction to them " +
                            "moves; future messages from them will be categorised automatically.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AcuranzaTheme.colors.subtleText,
                    )
                }
            }
            items(state.groups, key = { it.key }) { g ->
                AcuranzaCard(onClick = { picking = g }, contentPadding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(g.label, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${g.count} transaction${if (g.count == 1) "" else "s"} · last ${Fmt.date(Time.toDate(g.latest))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AcuranzaTheme.colors.subtleText,
                            )
                        }
                        AmountText(g.total, style = AmountStyles.small, hidden = hidden)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Choose category", tint = AcuranzaTheme.colors.subtleText)
                    }
                }
            }
        }
    }

    picking?.let { group ->
        ModalBottomSheet(onDismissRequest = { picking = null }) {
            Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Category for ${group.label}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    if (group.merchant != null) "Applies to all ${group.count} transactions and future ones from this payee."
                    else "Applies to these ${group.count} transactions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AcuranzaTheme.colors.subtleText,
                )
                CategoryGrid(categories = state.categories.filter { it.key != "other" }, selectedKey = null, onSelect = {
                    vm.assign(group, it.key)
                    picking = null
                    scope.launch { snackbar.showSnackbar("Moved ${group.count} to ${it.name}") }
                })
                Spacer(Modifier.width(1.dp))
            }
        }
    }
}
