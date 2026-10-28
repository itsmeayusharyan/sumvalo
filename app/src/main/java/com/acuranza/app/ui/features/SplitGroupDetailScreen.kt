package com.acuranza.app.ui.features

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.acuranza.app.data.SplitExpenseEntity
import com.acuranza.app.data.SplitGroupEntity
import com.acuranza.app.data.SplitGroupParticipantEntity
import com.acuranza.app.designsystem.components.AmountText
import com.acuranza.app.designsystem.components.EmptyState
import com.acuranza.app.designsystem.components.SectionHeader
import com.acuranza.app.designsystem.components.SumvaloCard
import com.acuranza.app.designsystem.theme.AmountStyles
import com.acuranza.app.designsystem.theme.SumvaloTheme
import com.acuranza.app.repo.SplitCalculator
import com.acuranza.app.repo.ParticipantBalance
import com.acuranza.app.ui.common.Fmt
import com.acuranza.app.ui.common.LocalContainer
import com.acuranza.app.ui.common.SumvaloScreen
import com.acuranza.app.ui.common.rememberHaptic
import com.acuranza.app.ui.nav.popBackSafely
import com.acuranza.app.ui.nav.Routes
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Group detail: participants, balances, expenses, and settlements. */
@Composable
fun SplitGroupDetailScreen(nav: NavController, groupId: Long) {
    val haptic = rememberHaptic()
    val c = LocalContainer.current
    val scope = rememberCoroutineScope()

    val groups by c.splitGroups.observeActive().collectAsState(initial = emptyList())
    val currentGroup = groups.firstOrNull { it.id == groupId }

    if (currentGroup == null) {
        SumvaloScreen(
            title = "Group",
            onBack = { nav.popBackSafely() },
        ) {
            EmptyState("Group not found", "The group may have been archived or deleted.")
        }
        return@SplitGroupDetailScreen
    }

    val participants by c.splitGroups.observeParticipants(groupId).collectAsState(initial = emptyList())
    val expenses by c.splitExpenses.observeForGroup(groupId).collectAsState(initial = emptyList())
    val settlements by c.splitSettlements.observeForGroup(groupId).collectAsState(initial = emptyList())

    val allShares by c.splitExpenses.observeAllSharesForGroup(groupId).collectAsState(initial = emptyList())
    val sharesByExpense = allShares.groupBy { it.expenseId }

    val balances = SplitCalculator.balances(participants, expenses, sharesByExpense, settlements)

    SumvaloScreen(
        title = currentGroup.name,
        onBack = { nav.popBackSafely() },
        floating = {
            ExtendedFloatingActionButton(
                onClick = { haptic.tick(); nav.navigate(Routes.splitAddExpense(groupId)) },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Expense") },
            )
        },
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Balances summary
            SectionHeader("Balances")
            LazyColumn(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(balances, key = { it.participantId }) { b ->
                    SumvaloCard(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Row(Modifier.fillMaxWidth()) {
                            Column(Modifier.weight(1f)) {
                                Text(b.participantName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                val netLabel = when {
                                    b.netBalance > 0 -> "Owed to you"
                                    b.netBalance < 0 -> "You owe"
                                    else -> "Settled"
                                }
                                Text(
                                    netLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SumvaloTheme.colors.subtleText,
                                )
                            }
                            AmountText(
                                b.netBalance,
                                style = AmountStyles.large,
                                color = if (b.netBalance > 0) SumvaloTheme.colors.good else if (b.netBalance < 0) SumvaloTheme.colors.alert else SumvaloTheme.colors.subtleText,
                                hidden = false,
                            )
                        }
                    }
                }
            }

            // Recent expenses
            if (expenses.isNotEmpty()) {
                SectionHeader("Recent expenses")
                LazyColumn(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(expenses, key = { it.id }) { e ->
                        SumvaloCard(
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        ) {
                            Row(Modifier.fillMaxWidth()) {
                                Column(Modifier.weight(1f)) {
                                    Text(e.description, style = MaterialTheme.typography.bodyMedium)
                                    val payer = participants.firstOrNull { it.id == e.payerId }?.name ?: "Unknown"
                                    Text(
                                        "Paid by $payer · ${Fmt.date(LocalDate.ofEpochDay(e.date))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SumvaloTheme.colors.subtleText,
                                    )
                                }
                                AmountText(
                                    e.amount,
                                    style = AmountStyles.medium,
                                    hidden = false,
                                )
                            }
                        }
                    }
                }
            }

            // Recent settlements
            if (settlements.isNotEmpty()) {
                SectionHeader("Recent settlements")
                LazyColumn(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(settlements, key = { it.id }) { s ->
                        SumvaloCard(
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        ) {
                            Row(Modifier.fillMaxWidth()) {
                                val from = participants.firstOrNull { it.id == s.fromId }?.name ?: "Unknown"
                                val to = participants.firstOrNull { it.id == s.toId }?.name ?: "Unknown"
                                Column(Modifier.weight(1f)) {
                                    Text("$from → $to", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        Fmt.date(LocalDate.ofEpochDay(s.date)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SumvaloTheme.colors.subtleText,
                                    )
                                }
                                AmountText(
                                    s.amount,
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
}
