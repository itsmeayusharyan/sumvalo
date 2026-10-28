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
import androidx.compose.material3.FloatingActionButton
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
import com.acuranza.app.data.SplitExpenseEntity
import com.acuranza.app.data.SplitGroupEntity
import com.acuranza.app.data.SplitGroupParticipantEntity
import com.acuranza.app.data.SplitShareEntity
import com.acuranza.app.designsystem.components.AmountText
import com.acuranza.app.designsystem.components.EmptyState
import com.acuranza.app.designsystem.components.SumvaloCard
import com.acuranza.app.designsystem.theme.AmountStyles
import com.acuranza.app.designsystem.theme.SumvaloTheme
import com.acuranza.app.repo.SplitCalculator
import com.acuranza.app.repo.SplitExpenseRepository
import com.acuranza.app.repo.SplitGroupRepository
import com.acuranza.app.ui.common.Fmt
import com.acuranza.app.ui.common.LocalContainer
import com.acuranza.app.ui.common.SumvaloScreen
import com.acuranza.app.ui.common.rememberHaptic
import com.acuranza.app.ui.nav.popBackSafely
import com.acuranza.app.ui.nav.Routes
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Add a shared expense to a group. Equal split among selected participants. */
@Composable
fun SplitAddExpenseScreen(nav: NavController, groupId: Long) {
    val haptic = rememberHaptic()
    val c = LocalContainer.current
    val scope = rememberCoroutineScope()

    val participants by c.splitGroups.observeParticipants(groupId).collectAsState(initial = emptyList())

    var description by remember { mutableStateOf<String>("") }
    var amount by remember { mutableStateOf<Long?>(null) }
    var date by remember { mutableStateOf<LocalDate>(LocalDate.now()) }
    var payerId by remember { mutableStateOf<Long?>(null) }
    var note by remember { mutableStateOf<String>("") }
    var categoryKey by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf<Boolean>(false) }

    val canSave = description.isNotBlank() && amount != null && amount!! > 0 && payerId != null

    SumvaloScreen(
        title = "Add shared expense",
        onBack = { nav.popBackSafely() },
        floating = {
            if (!saving) {
                FloatingActionButton(
                    onClick = {
                        if (!canSave) return@FloatingActionButton
                        haptic.tick()
                        saving = true
                        val split = SplitCalculator.equalSplit(amount!!, participants.size)
                        val shares = participants.indices.map { i ->
                            com.acuranza.app.data.SplitShareEntity(
                                expenseId = 0,
                                participantId = participants[i].id,
                                share = split[i],
                            )
                        }
                        scope.launch {
                            c.splitExpenses.saveWithShares(
                                com.acuranza.app.data.SplitExpenseEntity(
                                    groupId = groupId,
                                    description = description.trim(),
                                    amount = amount!!,
                                    date = date.toEpochDay(),
                                    payerId = payerId!!,
                                    categoryKey = categoryKey,
                                    note = note.takeIf { it.isNotBlank() },
                                ),
                                shares,
                            )
                            saving = false
                            nav.popBackSafely()
                        }
                    },
                ) {
                    Icon(Icons.Filled.Add, null)
                }
            }
        },
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SumvaloCard {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    com.acuranza.app.ui.common.SumvaloTextField(description, { description = it }, "Description")
                    com.acuranza.app.ui.common.AmountField("Amount", amount, { amount = it }, allowZero = false)
                    com.acuranza.app.ui.common.DateField("Date", date, { date = it })

                    androidx.compose.material3.Text(
                        "Payer",
                        style = MaterialTheme.typography.labelSmall,
                        color = SumvaloTheme.colors.subtleText,
                    )
                    androidx.compose.material3.TextField(
                        value = participants.firstOrNull { it.id == payerId }?.name ?: "Select payer",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Select payer") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    com.acuranza.app.ui.common.SumvaloTextField(categoryKey ?: "", { categoryKey = it.takeIf { it.isNotBlank() } }, "Category (optional)")
                    com.acuranza.app.ui.common.SumvaloTextField(note, { note = it }, "Note (optional)")

                    androidx.compose.material3.Text(
                        "Equal split: each of ${participants.size} participant(s) pays ${com.acuranza.app.model.Money.format((amount ?: 0L) / participants.size.coerceAtLeast(1).toLong())}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SumvaloTheme.colors.subtleText,
                    )
                }
            }
        }
    }
}
