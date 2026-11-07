package com.acuranza.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.acuranza.app.data.CategoryEntity
import com.acuranza.app.designsystem.components.CategoryIcon
import com.acuranza.app.designsystem.components.CategoryIcons
import com.acuranza.app.designsystem.components.PrimaryButton
import com.acuranza.app.designsystem.components.SumvaloCard
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

/** Chakra-flavoured text style, matching the home/activity voice. */
private fun chakraText(weight: FontWeight, size: Int, lineHeight: Int, letterSpacingSp: Float = 0f): TextStyle =
    TextStyle(fontFamily = com.acuranza.app.ui.common.Chakra, fontWeight = weight, fontSize = size.sp, lineHeight = lineHeight.sp, letterSpacing = letterSpacingSp.sp)

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
    val colors = SumvaloTheme.colors
    var query by rememberSaveable { mutableStateOf("") }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val type = if (tab == 0) TxnType.EXPENSE else TxnType.INCOME
    val title = if (tab == 0) "Spending" else "Income"
    val rows = cats.filter { it.type == type && it.name.contains(query.trim(), ignoreCase = true) }

    SumvaloScreen(
        title = "Categories",
        subtitle = "Your spending and income categories",
        onBack = onBack,
        snackbar = snackbar,
    ) { padding ->
        Column(
            Modifier
                .fillMaxWidth()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 40.dp),
        ) {
            CategorySearchField(query, onQueryChange = { query = it })
            Spacer(Modifier.height(10.dp))
            TypeToggle(
                spendingCount = cats.count { it.type == TxnType.EXPENSE },
                incomeCount = cats.count { it.type == TxnType.INCOME },
                selected = tab,
                onSelect = { tab = it },
            )
            Spacer(Modifier.height(20.dp))
            SectionHeader(
                title = title,
                count = rows.size,
                onAdd = { onAdd(type) },
            )
            Spacer(Modifier.height(10.dp))
            if (rows.isEmpty()) {
                Text(
                    if (query.isBlank()) "No categories here yet" else "No categories match “${query.trim()}”",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.subtleText,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
                )
            } else {
                // One bordered card per section, rows hairline-separated inside it.
                SumvaloCard(contentPadding = PaddingValues(0.dp)) {
                    rows.forEachIndexed { i, cat ->
                        CategoryRowFlat(cat) { onEdit(cat) }
                        if (i < rows.lastIndex) {
                            Box(
                                Modifier
                                    .padding(start = 60.dp, end = 12.dp)
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(colors.divider),
                            )
                        }
                    }
                }
            }
        }
        editSheet()
        deleteFlow()
    }
}

/** The reference's search field: 44dp, card face, hairline border, focus ring. */
@Composable
private fun CategorySearchField(query: String, onQueryChange: (String) -> Unit) {
    val colors = SumvaloTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(shape)
            .background(colors.cardSurface)
            .border(1.dp, if (focused) colors.tertiaryText else colors.hairline, shape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = colors.tertiaryText, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = chakraText(FontWeight.Normal, 15, 20).copy(color = colors.ink),
            cursorBrush = SolidColor(colors.ink),
            interactionSource = interaction,
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            "Search categories",
                            style = chakraText(FontWeight.Normal, 15, 20),
                            color = colors.tertiaryText,
                            maxLines = 1,
                        )
                    }
                    inner()
                }
            },
        )
    }
}

/** Spending / Income segmented control with count badges, active side on the ink pill. */
@Composable
private fun TypeToggle(spendingCount: Int, incomeCount: Int, selected: Int, onSelect: (Int) -> Unit) {
    val colors = SumvaloTheme.colors
    val options = remember { listOf("Spending", "Income") }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, colors.hairline, RoundedCornerShape(12.dp))
            .background(colors.cardSurface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val active = i == selected
            val count = if (i == 0) spendingCount else incomeCount
            Row(
                Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (active) colors.ink else Color.Transparent)
                    .clickable(role = Role.Button) { if (!active) onSelect(i) },
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    label,
                    style = chakraText(if (active) FontWeight.Bold else FontWeight.Medium, 14, 18),
                    color = if (active) colors.onInk else colors.tertiaryText,
                )
                Text(
                    "$count",
                    style = chakraText(FontWeight.SemiBold, 11, 14),
                    color = colors.subtleText,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(colors.fill)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

/** Section header: tracking-caps label + count pill on the left, "+ Add" on the right. */
@Composable
private fun SectionHeader(title: String, count: Int, onAdd: () -> Unit) {
    val colors = SumvaloTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                title.uppercase(),
                style = chakraText(FontWeight.Bold, 12, 16, 1.2f),
                color = colors.subtleText,
            )
            Text(
                "$count",
                style = chakraText(FontWeight.SemiBold, 11, 14),
                color = colors.subtleText,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(colors.fill)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        Row(
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(role = Role.Button, onClick = onAdd)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, tint = colors.ink, modifier = Modifier.size(14.dp))
            Text("Add", style = chakraText(FontWeight.Bold, 14, 18), color = colors.ink)
        }
    }
}

/** One category row inside its section card: icon tile, name, chevron. */
@Composable
private fun CategoryRowFlat(cat: CategoryEntity, onClick: () -> Unit) {
    val colors = SumvaloTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryIcon(cat.icon, cat.color(), size = 36.dp)
        Row(
            Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                cat.name,
                style = chakraText(FontWeight.SemiBold, 15, 20),
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Edit ${cat.name}",
                tint = colors.tertiaryText,
                modifier = Modifier.size(14.dp),
            )
        }
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
