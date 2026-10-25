package com.acuranza.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.SouthWest
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.acuranza.app.data.SmsImportBatchEntity
import com.acuranza.app.data.SmsImportDao
import com.acuranza.app.designsystem.components.SumvaloCard
import com.acuranza.app.designsystem.components.SecondaryButton
import com.acuranza.app.designsystem.theme.SumvaloText
import com.acuranza.app.designsystem.theme.SumvaloTheme
import com.acuranza.app.model.Money
import com.acuranza.app.model.TxnType
import com.acuranza.app.ui.common.SumvaloScreen
import com.acuranza.app.ui.common.Fmt
import com.acuranza.app.ui.common.LocalContainer
import com.acuranza.app.ui.nav.popBackSafely
import kotlinx.coroutines.launch
import androidx.compose.ui.text.style.TextOverflow

/**
 * Full management list for transactions imported from bank messages — relocated here from the
 * SMS tracking screen so that screen stays about permissions, scanning and tracking status.
 * Purely a view over existing rows: no imports, no deduplication, nothing is reprocessed.
 */
@Composable
fun SmsImportsScreen(nav: NavController) {
    val c = LocalContainer.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { androidx.compose.material3.SnackbarHostState() }
    val backStack = nav.currentBackStackEntryAsState()

    var rows by remember { mutableStateOf<List<SmsImportDao.ManagementRow>>(emptyList()) }
    var batches by remember { mutableStateOf<List<com.acuranza.app.data.SmsImportBatchEntity>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf<SmsImportDao.ManagementRow?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            message = null
        }
    }
    // Reload whenever this screen (re)enters the foreground navigation, so rows and the count
    // stay true after confirming, undoing or deleting elsewhere.
    fun reload() {
        scope.launch {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                rows = c.smsImports.managementRows(Int.MAX_VALUE).filter { it.transactionId != null }
                batches = c.smsImports.latestBatches().filterNot { it.label == "Pending review" }
            }
        }
    }
    LaunchedEffect(backStack) { reload() }
    SumvaloScreen(
        title = "Imported transactions",
        subtitle = "From bank messages — confirm, fix or delete them here",
        onBack = { nav.popBackSafely() },
        snackbar = snackbar,
    ) { padding ->
        deleting?.let { row ->
            AlertDialog(
                onDismissRequest = { deleting = null },
                title = { Text("Delete this import?") },
                text = {
                    Text(
                        "Removes \"${row.merchant ?: row.bank}\" (${Money.format(row.amount)}) and its transaction. " +
                            "Balances and every other record recompute from what remains.",
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val target = row
                        deleting = null
                        scope.launch {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                c.smsImports.deleteImport(target.importId)
                            }
                            message = "Import deleted"
                            reload()
                        }
                    }) { Text("Delete", color = SumvaloTheme.colors.alert) }
                },
                dismissButton = { TextButton(onClick = { deleting = null }) { Text("Keep") } },
            )
        }

        val q = query.trim()
        val visible = if (q.isEmpty()) rows else rows.filter {
            (it.merchant?.contains(q, ignoreCase = true) == true) ||
                it.bank.contains(q, ignoreCase = true) ||
                it.sender.contains(q, ignoreCase = true)
        }

        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search merchant, bank or sender") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            )
            if (rows.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("Nothing imported yet", style = SumvaloText.screenTitle)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Transactions confirmed from bank messages appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SumvaloTheme.colors.subtleText,
                    )
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Text(
                            "${visible.size} of ${rows.size} imported",
                            style = SumvaloText.sectionLabel,
                            color = SumvaloTheme.colors.subtleText,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                        )
                    }
                    items(visible, key = { it.importId }) { row ->
                        SumvaloCard {
                            Row(Modifier.padding(vertical = 12.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (row.type == TxnType.INCOME) Icons.Outlined.NorthEast else Icons.Outlined.SouthWest,
                                    contentDescription = if (row.type == TxnType.INCOME) "Money received" else "Money spent",
                                    tint = if (row.type == TxnType.INCOME) SumvaloTheme.colors.income else SumvaloTheme.colors.expense,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(row.merchant ?: row.bank, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    val flagged = if (row.suspicious == true && row.reviewed == false) " · flagged for review" else ""
                                    Text(
                                        "${row.bank} · ${Fmt.time(row.timestamp)}$flagged",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SumvaloTheme.colors.tertiaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        (if (row.type == TxnType.INCOME) "+" else "−") + Money.format(row.amount),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (row.type == TxnType.INCOME) SumvaloTheme.colors.income else SumvaloTheme.colors.expense,
                                    )
                                }
                                IconButton(onClick = { deleting = row }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete import", tint = SumvaloTheme.colors.tertiaryText)
                                }
                            }
                        }
                    }
                    batches.firstOrNull()?.let { last ->
                        item {
                            SecondaryButton(
                                "Undo last import batch (${last.txnCount})",
                                onClick = {
                                    scope.launch {
                                        val removed = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            c.smsImports.undoBatch(last.id)
                                        }
                                        message = "Removed $removed imported transactions"
                                        reload()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}
