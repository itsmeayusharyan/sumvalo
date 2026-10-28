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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.acuranza.app.data.SplitGroupEntity
import com.acuranza.app.designsystem.components.AmountText
import com.acuranza.app.designsystem.components.EmptyState
import com.acuranza.app.designsystem.components.SectionHeader
import com.acuranza.app.designsystem.components.SumvaloCard
import com.acuranza.app.designsystem.components.SumvaloCard
import com.acuranza.app.designsystem.theme.AmountStyles
import com.acuranza.app.designsystem.theme.SumvaloTheme
import com.acuranza.app.repo.SplitGroupRepository
import com.acuranza.app.ui.common.Fmt
import com.acuranza.app.ui.common.SumvaloScreen
import com.acuranza.app.ui.common.SumvaloTextField
import com.acuranza.app.ui.common.rememberHaptic
import com.acuranza.app.ui.nav.popBackSafely
import com.acuranza.app.ui.nav.Routes
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Groups overview for Split Any Expense. */
@Composable
fun SplitGroupsScreen(nav: NavController) {
    val haptic = rememberHaptic()
    val c = com.acuranza.app.ui.common.LocalContainer.current
    val scope = rememberCoroutineScope()
    var adding by remember { mutableStateOf(false) }
    val groups by c.splitGroups.observeActive().collectAsState(initial = emptyList())

    SumvaloScreen(
        title = "Shared expenses",
        onBack = { nav.popBackSafely() },
        floating = {
            if (!adding) {
                ExtendedFloatingActionButton(
                    onClick = { haptic.tick(); adding = true },
                    icon = { Icon(Icons.Filled.Add, null) },
                    text = { Text("Group") },
                )
            }
        },
    ) { pad ->
        if (groups.isEmpty()) {
            EmptyState(
                "No shared expense groups",
                "Create a group to split bills with friends, family or roommates. Everyone stays local — no accounts or internet needed.",
                onAction = { adding = true },
                actionLabel = "Create group",
            )
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(pad),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(groups, key = { it.id }) { g ->
                    SumvaloCard(
                        onClick = { nav.navigate(Routes.splitGroup(g.id)) },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        Row(Modifier.fillMaxWidth()) {
                            Column(Modifier.weight(1f)) {
                                Text(g.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                                Text(
                                    "Created ${Fmt.date(LocalDate.parse(g.createdAt.toString(), DateTimeFormatter.ofPattern("yyyyMMdd")))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SumvaloTheme.colors.subtleText,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (adding) {
            SplitGroupDialog(
                onDismiss = { adding = false },
                onSave = { name ->
                    scope.launch {
                        c.splitGroups.save(com.acuranza.app.data.SplitGroupEntity(name = name))
                        adding = false
                    }
                },
            )
        }
    }
}

@Composable
private fun SplitGroupDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New group") },
        text = {
            androidx.compose.foundation.layout.Column {
                SumvaloTextField(name, { name = it }, "Group name (e.g. Roommates, Trip to Manali)")
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                enabled = name.isNotBlank(),
                onClick = { onSave(name.trim()) },
            ) { Text("Create") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
