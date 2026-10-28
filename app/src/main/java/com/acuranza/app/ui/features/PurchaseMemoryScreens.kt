package com.acuranza.app.ui.features

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.acuranza.app.data.PurchaseRecordEntity
import com.acuranza.app.designsystem.components.AmountText
import com.acuranza.app.designsystem.components.EmptyState
import com.acuranza.app.designsystem.components.SumvaloCard
import com.acuranza.app.designsystem.theme.AmountStyles
import com.acuranza.app.designsystem.theme.SumvaloTheme
import com.acuranza.app.repo.PurchaseRepository
import com.acuranza.app.ui.common.Fmt
import com.acuranza.app.ui.common.LocalContainer
import com.acuranza.app.ui.common.SumvaloScreen
import com.acuranza.app.ui.common.rememberHaptic
import com.acuranza.app.ui.nav.popBackSafely
import com.acuranza.app.ui.nav.Routes
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Purchase Memory overview — list of purchase records with search. */
@Composable
fun PurchasesScreen(nav: NavController) {
    val haptic = rememberHaptic()
    val c = LocalContainer.current
    val scope = rememberCoroutineScope()
    var adding by remember { mutableStateOf<Boolean>(false) }
    var searchQuery by remember { mutableStateOf<String>("") }
    val records by c.purchases.observeAll().collectAsState(initial = emptyList())

    val filtered = records.filter {
        searchQuery.isEmpty() ||
            it.itemName.lowercase().contains(searchQuery.lowercase()) ||
            (it.merchant != null && it.merchant.lowercase().contains(searchQuery.lowercase())) ||
            (it.notes != null && it.notes.lowercase().contains(searchQuery.lowercase()))
    }

    SumvaloScreen(
        title = "Purchase Memory",
        onBack = { nav.popBackSafely() },
        floating = {
            if (!adding) {
                ExtendedFloatingActionButton(
                    onClick = { haptic.tick(); adding = true },
                    icon = { Icon(Icons.Filled.Add, null) },
                    text = { Text("Purchase") },
                )
            }
        },
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            com.acuranza.app.ui.common.SumvaloTextField(
                searchQuery,
                { searchQuery = it },
                "Search by item, merchant, notes, invoice no.",
            )

            if (filtered.isEmpty()) {
                EmptyState(
                    "No purchase records",
                    "Add purchases to track warranties, return deadlines, and receipts.",
                    onAction = { adding = true },
                    actionLabel = "Add purchase",
                )
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(filtered, key = { it.id }) { p ->
                        SumvaloCard(
                            onClick = { nav.navigate(Routes.purchase(p.id)) },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                        ) {
                            Row(Modifier.fillMaxWidth()) {
                                Column(Modifier.weight(1f)) {
                                    Text(p.itemName, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        (p.merchant ?: "No merchant") + " · " + Fmt.date(LocalDate.ofEpochDay(p.purchaseDate)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SumvaloTheme.colors.subtleText,
                                    )
                                }
                                p.amount?.let {
                                    AmountText(
                                        it,
                                        style = AmountStyles.medium,
                                        hidden = false,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (adding) {
            PurchaseAddScreen(
                nav = nav,
                onDismiss = { adding = false },
                onSave = { adding = false },
            )
        }
    }
}

/** Purchase detail — shows all fields and attachments. */
@Composable
fun PurchaseDetailScreen(nav: NavController, purchaseId: Long) {
    val haptic = rememberHaptic()
    val c = LocalContainer.current
    val scope = rememberCoroutineScope()

    val records by c.purchases.observeAll().collectAsState(initial = emptyList())
    val currentRecord = records.firstOrNull { it.id == purchaseId }
    val attachments by c.purchases.observeAttachments(purchaseId).collectAsState(initial = emptyList())

    if (currentRecord == null) {
        SumvaloScreen(
            title = "Purchase",
            onBack = { nav.popBackSafely() },
        ) {
            EmptyState("Purchase not found", "The record may have been deleted.")
        }
        return@PurchaseDetailScreen
    }

    SumvaloScreen(
        title = currentRecord.itemName,
        onBack = { nav.popBackSafely() },
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SumvaloCard {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Text(currentRecord.itemName, style = MaterialTheme.typography.headlineSmall)
                    currentRecord.merchant?.let {
                        Text("Merchant: $it", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        "Purchased: ${Fmt.date(LocalDate.ofEpochDay(currentRecord.purchaseDate))}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    currentRecord.amount?.let {
                        Text(
                            "Amount: ${com.acuranza.app.model.Money.format(it)}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    currentRecord.category?.let {
                        Text("Category: $it", style = MaterialTheme.typography.bodyMedium)
                    }
                    currentRecord.notes?.let {
                        Text("Notes: $it", style = MaterialTheme.typography.bodyMedium)
                    }
                    currentRecord.invoiceNo?.let {
                        Text("Invoice: $it", style = MaterialTheme.typography.bodyMedium)
                    }
                    currentRecord.serialNo?.let {
                        Text("Serial: $it", style = MaterialTheme.typography.bodyMedium)
                    }
                    currentRecord.warrantyUntil?.let {
                        Text(
                            "Warranty until: ${Fmt.date(LocalDate.ofEpochDay(it))}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (LocalDate.ofEpochDay(it).isBefore(LocalDate.now())) SumvaloTheme.colors.alert else SumvaloTheme.colors.subtleText,
                        )
                    }
                    currentRecord.returnUntil?.let {
                        Text(
                            "Return by: ${Fmt.date(LocalDate.ofEpochDay(it))}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (LocalDate.ofEpochDay(it).isBefore(LocalDate.now())) SumvaloTheme.colors.alert else SumvaloTheme.colors.subtleText,
                        )
                    }
                    currentRecord.linkedTransactionId?.let {
                        Text("Linked to transaction #$it", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (attachments.isNotEmpty()) {
                androidx.compose.material3.Text(
                    "Attachments",
                    style = MaterialTheme.typography.titleMedium,
                )
                LazyColumn(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(attachments, key = { it.id }) { a ->
                        SumvaloCard(
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Row(Modifier.fillMaxWidth()) {
                                Column(Modifier.weight(1f)) {
                                    Text(a.label ?: a.fileName, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "${a.mimeType} · ${a.sizeBytes / 1024} KB",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SumvaloTheme.colors.subtleText,
                                    )
                                }
                                androidx.compose.material3.IconButton(
                                    onClick = {
                                        scope.launch {
                                            val bytes = c.purchases.attachmentBytes(a)
                                            // Save to temp and open — simplified: just show that file exists
                                        }
                                    },
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null)
                                }
                                androidx.compose.material3.IconButton(
                                    onClick = {
                                        scope.launch {
                                            c.purchases.deleteAttachment(a.id)
                                        }
                                    },
                                ) {
                                    Icon(Icons.Filled.Delete, null)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Add a purchase record with optional attachment. */
@Composable
fun PurchaseAddScreen(
    nav: NavController,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val haptic = rememberHaptic()
    val c = LocalContainer.current
    val scope = rememberCoroutineScope()

    var itemName by remember { mutableStateOf<String>("") }
    var merchant by remember { mutableStateOf<String>("") }
    var purchaseDate by remember { mutableStateOf<LocalDate>(LocalDate.now()) }
    var amount by remember { mutableStateOf<Long?>(null) }
    var category by remember { mutableStateOf<String>("") }
    var notes by remember { mutableStateOf<String>("") }
    var warrantyUntil by remember { mutableStateOf<LocalDate?>(null) }
    var returnUntil by remember { mutableStateOf<LocalDate?>(null) }
    var invoiceNo by remember { mutableStateOf<String>("") }
    var serialNo by remember { mutableStateOf<String>("") }
    var saving by remember { mutableStateOf<Boolean>(false) }

    val canSave = itemName.isNotBlank()

    androidx.compose.material3.AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Add purchase") },
        text = {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.fillMaxWidth().height(400.dp),
            ) {
                com.acuranza.app.ui.common.SumvaloTextField(itemName, { itemName = it }, "Item or product name *")
                com.acuranza.app.ui.common.SumvaloTextField(merchant, { merchant = it }, "Merchant or seller")
                com.acuranza.app.ui.common.DateField("Purchase date", purchaseDate, { purchaseDate = it })
                com.acuranza.app.ui.common.AmountField("Amount", amount, { amount = it }, allowZero = true)
                com.acuranza.app.ui.common.SumvaloTextField(category, { category = it }, "Category")
                com.acuranza.app.ui.common.SumvaloTextField(notes, { notes = it }, "Notes")
                com.acuranza.app.ui.common.DateField("Warranty until", warrantyUntil ?: LocalDate.now(), { warrantyUntil = it })
                com.acuranza.app.ui.common.DateField("Return by", returnUntil ?: LocalDate.now(), { returnUntil = it })
                com.acuranza.app.ui.common.SumvaloTextField(invoiceNo, { invoiceNo = it }, "Invoice / order no.")
                com.acuranza.app.ui.common.SumvaloTextField(serialNo, { serialNo = it }, "Serial no.")
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                enabled = canSave && !saving,
                onClick = {
                    if (!canSave) return@TextButton
                    haptic.tick()
                    saving = true
                    scope.launch {
                        c.purchases.save(
                            com.acuranza.app.data.PurchaseRecordEntity(
                                itemName = itemName.trim(),
                                merchant = merchant.takeIf { it.isNotBlank() },
                                purchaseDate = purchaseDate.toEpochDay(),
                                amount = amount,
                                category = category.takeIf { it.isNotBlank() },
                                notes = notes.takeIf { it.isNotBlank() },
                                warrantyUntil = warrantyUntil?.toEpochDay(),
                                returnUntil = returnUntil?.toEpochDay(),
                                invoiceNo = invoiceNo.takeIf { it.isNotBlank() },
                                serialNo = serialNo.takeIf { it.isNotBlank() },
                            )
                        )
                        saving = false
                        onSave()
                    }
                },
            ) { Text("Save") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = { onDismiss() }) { Text("Cancel") }
        },
    )
}
