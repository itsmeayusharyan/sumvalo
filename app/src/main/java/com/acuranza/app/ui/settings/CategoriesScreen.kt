package com.acuranza.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.acuranza.app.data.CategoryEntity
import com.acuranza.app.designsystem.components.CategoryIcon
import com.acuranza.app.designsystem.components.CategoryIcons
import com.acuranza.app.designsystem.components.PrimaryButton
import com.acuranza.app.designsystem.theme.SumvaloText
import com.acuranza.app.designsystem.theme.SumvaloTheme
import com.acuranza.app.model.TxnType
import com.acuranza.app.repo.CategoryRules
import com.acuranza.app.ui.common.SumvaloScreen
import com.acuranza.app.ui.common.LocalContainer
import com.acuranza.app.ui.common.color
import com.acuranza.app.ui.nav.popBackSafely
import kotlinx.coroutines.launch

/** The palette a category's colour is picked from — the same hues the built-ins use. */
private val CategorySwatches = listOf(
    0xFFFF7A59, 0xFF34C38F, 0xFFE056FD, 0xFF4DABF7, 0xFFFFA94D, 0xFF748FFC,
    0xFF9775FA, 0xFFF06595, 0xFFFF6B6B, 0xFF20C997, 0xFF3BC9DB, 0xFFCC5DE8,
    0xFF51CF66, 0xFFF59F00, 0xFF2F9E44, 0xFFADB5BD,
)

/**
 * Categories management: add, rename, restyle and delete income and expense categories. Deleting a
 * category that still holds records requires a replacement, and every record — transactions,
 * splits, recurring rules, learned merchant rules — moves there atomically before the delete.
 */
@Composable
fun CategoriesScreen(nav: NavController) {
    val c = LocalContainer.current
    val scope = rememberCoroutineScope()
    val cats by c.categories.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val snackbar = remember { SnackbarHostState() }

    var editing by remember { mutableStateOf<CategoryEntity?>(null) }
    var adding by remember { mutableStateOf<TxnType?>(null) }
    var deleting by remember { mutableStateOf<CategoryEntity?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    // Errors live only until the sheet closes; the snackbar is the fallback surface.
    LaunchedEffect(editing, adding) { if (editing == null && adding == null) error = null }

    fun save(initial: CategoryEntity?, type: TxnType, name: String, icon: String, color: Long) {
        scope.launch {
            val failure = if (initial == null) {
                if (c.categories.addCustom(name, type, icon, color) == null) "A category with this name already exists" else null
            } else {
                c.categories.update(initial, name, icon, color)
            }
            if (failure == null) {
                editing = null
                adding = null
                error = null
            } else {
                error = failure
            }
        }
    }

    fun requestDelete(cat: CategoryEntity) {
        scope.launch {
            if (c.categories.countTransactions(cat.key) == 0) {
                c.categories.deleteWithReassignment(cat.key, cat.key)
                editing = null
            } else {
                // Records exist: the replacement dialog takes over.
                editing = null
                deleting = cat
            }
        }
    }

    CategoriesContent(
        cats = cats,
        snackbar = snackbar,
        onAdd = { adding = it },
        onEdit = { editing = it },
        onBack = { nav.popBackSafely() },
        editSheet = {
            val editingCat = editing
            val addingType = adding
            if (editingCat != null || addingType != null) {
                CategoryEditSheet(
                    initial = editingCat,
                    type = editingCat?.type ?: addingType!!,
                    currentName = editingCat?.name ?: "",
                    currentIcon = editingCat?.icon ?: "category",
                    currentColor = editingCat?.colorArgb ?: CategorySwatches.first(),
                    error = error,
                    onSave = { name, icon, color -> save(editingCat, editingCat?.type ?: addingType!!, name, icon, color) },
                    onDelete = editingCat?.let { cat -> { requestDelete(cat) } },
                    onDismiss = {
                        editing = null
                        adding = null
                        error = null
                    },
                )
            }
        },
        deleteFlow = {
            deleting?.let { victim ->
                val options = CategoryRules.reassignmentOptions(cats, victim)
                var replacement by remember(victim.key) { mutableStateOf(CategoryRules.suggestedReplacement(cats, victim)?.key) }
                ReplaceCategoryDialog(
                    victim = victim,
                    options = options,
                    selectedKey = replacement,
                    onSelect = { replacement = it },
                    onConfirm = {
                        val to = replacement
                        deleting = null
                        if (to != null) scope.launch { c.categories.deleteWithReassignment(victim.key, to) }
                    },
                    onDismiss = { deleting = null },
                )
            }
        },
    )
}

/** Stateless list body, so screenshot tests can render it without the container. */
@Composable
fun CategoriesContent(
    cats: List<CategoryEntity>,
    snackbar: SnackbarHostState? = null,
    onAdd: (TxnType) -> Unit = {},
    onEdit: (CategoryEntity) -> Unit = {},
    onBack: () -> Unit = {},
    editSheet: @Composable () -> Unit = {},
    deleteFlow: @Composable () -> Unit = {},
) {
    SumvaloScreen(
        title = "Categories",
        subtitle = "Your spending and income categories",
        onBack = onBack,
        snackbar = snackbar,
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxWidth().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp),
        ) {
            listOf(TxnType.EXPENSE to "Spending", TxnType.INCOME to "Income").forEach { (type, title) ->
                val rows = cats.filter { it.type == type }
                item(key = "head-$title") {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(title.uppercase(), style = SumvaloText.sectionLabel, color = SumvaloTheme.colors.subtleText, modifier = Modifier.weight(1f))
                        Text(
                            "Add category",
                            style = MaterialTheme.typography.labelMedium,
                            color = SumvaloTheme.colors.accent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onAdd(type) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                        )
                    }
                }
                items(rows.size, key = { rows[it].key }) { i ->
                    val cat = rows[i]
                    CategoryRow(cat) { onEdit(cat) }
                }
            }
        }
        editSheet()
        deleteFlow()
    }
}

@Composable
private fun CategoryRow(cat: CategoryEntity, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryIcon(cat.icon, cat.color(), size = 38.dp)
        Spacer(Modifier.width(14.dp))
        Text(cat.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (!CategoryRules.isDeletable(cat)) {
            Text("Built-in", style = MaterialTheme.typography.labelSmall, color = SumvaloTheme.colors.tertiaryText)
            Spacer(Modifier.width(12.dp))
        }
        Icon(Icons.Outlined.Edit, contentDescription = "Edit ${cat.name}", tint = SumvaloTheme.colors.tertiaryText, modifier = Modifier.size(16.dp))
    }
}

/**
 * The add/edit sheet: name, icon and colour. Saving runs through the repository, which rejects
 * duplicates; the message comes back onto the sheet instead of losing the user's input.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditSheet(
    initial: CategoryEntity?,
    type: TxnType,
    currentName: String,
    currentIcon: String,
    currentColor: Long,
    error: String?,
    onSave: (String, String, Long) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(currentName) }
    var icon by remember { mutableStateOf(currentIcon) }
    var color by remember { mutableLongStateOf(currentColor) }
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val nameOk = name.isNotBlank()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                if (initial == null) "New ${if (type == TxnType.EXPENSE) "spending" else "income"} category" else "Edit category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, isError = !nameOk && error == null, modifier = Modifier.fillMaxWidth())
            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = SumvaloTheme.colors.alert)
            }
            Spacer(Modifier.height(16.dp))
            Text("Icon", style = SumvaloText.sectionLabel, color = SumvaloTheme.colors.subtleText)
            Spacer(Modifier.height(8.dp))
            IconPicker(selected = icon, onSelect = { icon = it })
            Spacer(Modifier.height(16.dp))
            Text("Colour", style = SumvaloText.sectionLabel, color = SumvaloTheme.colors.subtleText)
            Spacer(Modifier.height(8.dp))
            ColorSwatches(selected = color, onSelect = { color = it })
            Spacer(Modifier.height(20.dp))
            PrimaryButton(text = "Save", enabled = nameOk, onClick = { onSave(name.trim(), icon, color) }, modifier = Modifier.fillMaxWidth())
            if (initial != null && CategoryRules.isDeletable(initial)) {
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = { onDelete?.invoke() })
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Outlined.DeleteForever, contentDescription = null, tint = SumvaloTheme.colors.alert, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Delete category", style = MaterialTheme.typography.labelLarge, color = SumvaloTheme.colors.alert)
                }
            }
        }
    }
}

@Composable
private fun IconPicker(selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CategoryIcons.names.chunked(6).forEach { rowNames ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowNames.forEach { n ->
                    val picked = n == selected
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (picked) SumvaloTheme.colors.accentTint else SumvaloTheme.colors.fill)
                            .clickable { onSelect(n) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            CategoryIcons.forName(n),
                            contentDescription = null,
                            tint = if (picked) SumvaloTheme.colors.accent else SumvaloTheme.colors.ink,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSwatches(selected: Long, onSelect: (Long) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CategorySwatches.chunked(8).forEach { rowColors ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowColors.forEach { c ->
                    val picked = c == selected
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(c))
                            .then(if (picked) Modifier.border(2.dp, SumvaloTheme.colors.ink, CircleShape) else Modifier)
                            .clickable { onSelect(c) },
                    )
                }
            }
        }
    }
}

/**
 * Deleting a category that still has records: the user picks where they go, then the move and the
 * delete happen together in one database transaction.
 */
@Composable
fun ReplaceCategoryDialog(
    victim: CategoryEntity,
    options: List<CategoryEntity>,
    selectedKey: String?,
    onSelect: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete “${victim.name}”?", fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                Text(
                    "Its transactions, splits and rules move to the category you pick. A budget for it, if any, is removed.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(12.dp))
                options.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (option.key == selectedKey) SumvaloTheme.colors.accentTint else Color.Transparent)
                            .clickable { onSelect(option.key) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CategoryIcon(option.icon, option.color(), size = 30.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(option.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        if (option.key == selectedKey) {
                            Icon(Icons.Outlined.Check, contentDescription = null, tint = SumvaloTheme.colors.accent, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = selectedKey != null, onClick = onConfirm) { Text("Move & delete") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
