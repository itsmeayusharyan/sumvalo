package com.acuranza.app.ui.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.acuranza.app.model.ChangeCategory
import com.acuranza.app.designsystem.components.SumvaloCard
import com.acuranza.app.designsystem.theme.SumvaloTheme
import com.acuranza.app.ui.common.SumvaloScreen
import com.acuranza.app.ui.nav.popBackSafely

/** Dedicated changelog screen: lists locally-maintained releases newest-first, grouped by
 * change category. Works completely offline — no network, account, or API key needed.
 *
 * Reached from Settings → About → Changelog.
 */
@Composable
fun ChangelogScreen(
    nav: androidx.navigation.NavController,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
) {
    val context = LocalContext.current

    SumvaloScreen(
        title = "Changelog",
        subtitle = "What's new in each release",
        onBack = { nav.popBackSafely() },
        snackbar = snackbar,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            val releases = LocalChangelog

            if (releases.isEmpty()) {
                renderEmpty()
            } else {
                releases.forEachIndexed { index, release ->
                    SumvaloCard(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(vertical = if (index == 0) 0.dp else 12.dp),
                    ) {
                        Column(
                            Modifier
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        release.version,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SumvaloTheme.colors.ink,
                                        modifier = Modifier.padding(bottom = 2.dp),
                                    )
                                    Text(
                                        "v${release.tag} · ${release.publishedDate}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SumvaloTheme.colors.subtleText,
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = SumvaloTheme.colors.tertiaryText,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            release.changes.groupBy { it.category }.forEach { (category, items) ->
                                renderCategory(category, items, SumvaloTheme.colors)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun renderCategory(
    category: ChangeCategory?,
    items: List<com.acuranza.app.model.ChangeItem>,
    colors: com.acuranza.app.designsystem.theme.SumvaloColors,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            categoryDisplayName(category),
            style = MaterialTheme.typography.labelSmall,
            color = colors.subtleText,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        items.forEach { item ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = categoryIcon(category),
                    contentDescription = null,
                    tint = categoryColor(category, colors),
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    item.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.ink,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun renderEmpty() {
    val colors = SumvaloTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = colors.tertiaryText,
            modifier = Modifier.size(32.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "No releases yet",
            style = MaterialTheme.typography.titleMedium,
            color = colors.ink,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Check back later for release notes.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.subtleText,
        )
    }
}

private fun categoryDisplayName(category: ChangeCategory?): String {
    return when (category) {
        ChangeCategory.ADDED -> "Added"
        ChangeCategory.IMPROVED -> "Improved"
        ChangeCategory.FIXED -> "Fixed"
        ChangeCategory.SECURITY -> "Security"
        ChangeCategory.OTHER, null -> "Changes"
    }
}

private fun categoryIcon(category: ChangeCategory?): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category) {
        ChangeCategory.ADDED -> Icons.Outlined.Add
        ChangeCategory.IMPROVED -> Icons.Outlined.AutoAwesome
        ChangeCategory.FIXED -> Icons.Outlined.CheckCircle
        ChangeCategory.SECURITY -> Icons.Outlined.Shield
        ChangeCategory.OTHER, null -> Icons.Outlined.Info
    }
}

private fun categoryColor(category: ChangeCategory?, colors: com.acuranza.app.designsystem.theme.SumvaloColors): Color {
    return when (category) {
        ChangeCategory.ADDED -> colors.good
        ChangeCategory.IMPROVED -> colors.tip
        ChangeCategory.FIXED -> colors.gold
        ChangeCategory.SECURITY -> colors.alert
        ChangeCategory.OTHER, null -> colors.tertiaryText
    }
}
