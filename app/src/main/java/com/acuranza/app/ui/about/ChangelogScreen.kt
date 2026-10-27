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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.acuranza.app.model.ChangeCategory
import com.acuranza.app.designsystem.theme.SumvaloText
import com.acuranza.app.designsystem.theme.SumvaloTheme
import com.acuranza.app.ui.common.SumvaloScreen
import com.acuranza.app.ui.nav.popBackSafely
import com.acuranza.app.ui.settings.SettingsDivider
import com.acuranza.app.ui.settings.SettingsRow
import com.acuranza.app.ui.settings.SettingsSection

/** Dedicated changelog screen: lists locally-maintained releases newest-first as expandable
 * cards using the same card styling as the About section (SettingsSection / SettingsRow).
 * Works completely offline — no network, account, or API key needed.
 */
@Composable
fun ChangelogScreen(
    nav: androidx.navigation.NavController,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
) {
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
                    val expanded = remember(release.tag) { mutableStateOf(false) }

                    // Each version is its own card — same SumvaloCard styling as About entries
                    // (cardSurface background, hairline border, cardRadius corners).
                    SettingsSection(title = null) {
                        // Header row — matches RowSkeleton styling exactly:
                        // leading icon in the same IconBubble container as About cards,
                        // ChevronRight shown automatically by SettingsRow when onClick is set.
                        SettingsRow(
                            title = release.version,
                            subtitle = "v${release.tag} · ${release.displayDate}",
                            icon = Icons.Outlined.AutoAwesome,
                            onClick = { expanded.value = !expanded.value },
                            divider = false,
                        )

                        if (expanded.value) {
                            // Hairline divider between header and changelog content,
                            // matching About row dividers (16.dp start inset, no icon tile).
                            SettingsDivider(16.dp)

                            release.changes.groupBy { it.category }.forEach { (category, items) ->
                                Text(
                                    categoryDisplayName(category),
                                    style = SumvaloText.sectionLabel,
                                    color = SumvaloTheme.colors.subtleText,
                                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 4.dp),
                                )
                                items.forEach { item ->
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = categoryIcon(category),
                                            contentDescription = null,
                                            tint = categoryColor(category, SumvaloTheme.colors),
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            item.text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = SumvaloTheme.colors.ink,
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
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
            .padding(horizontal = 20.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = colors.tertiaryText,
            modifier = Modifier.size(36.dp),
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
