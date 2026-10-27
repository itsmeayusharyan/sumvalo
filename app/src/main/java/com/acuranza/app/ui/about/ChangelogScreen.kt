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
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Publish
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.acuranza.app.data.local.ReleaseCache
import com.acuranza.app.model.ChangeCategory
import com.acuranza.app.model.GitHubReleaseClient
import com.acuranza.app.model.ReleaseEntry
import com.acuranza.app.designsystem.components.SumvaloCard
import com.acuranza.app.designsystem.theme.SumvaloTheme
import com.acuranza.app.ui.common.SumvaloScreen
import com.acuranza.app.ui.common.acuranzaViewModel
import com.acuranza.app.ui.nav.popBackSafely
import kotlinx.coroutines.launch

/** Dedicated changelog screen: lists GitHub releases newest-first, grouped by change category.
 * Reached from Settings → About → Changelog.
 */
@Composable
fun ChangelogScreen(
    nav: androidx.navigation.NavController,
    cache: ReleaseCache,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
) {
    val viewModel = acuranzaViewModel { ChangelogViewModel(it, GitHubReleaseClient) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
            // Header with refresh
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Releases",
                    style = MaterialTheme.typography.titleMedium,
                    color = SumvaloTheme.colors.ink,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = {
                        viewModel.refresh()
                        scope.launch { snackbar.showSnackbar("Checking for updates…") }
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Refresh",
                        tint = SumvaloTheme.colors.tertiaryText,
                    )
                }
            }

            when (val state = viewModel.uiState.value) {
                is ChangelogUiState.Loading -> {
                    if (state.releases.isEmpty()) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CircularProgressIndicator(Modifier.size(20.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Loading releases…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SumvaloTheme.colors.subtleText,
                            )
                        }
                    } else {
                        renderReleases(state.releases)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Checking for updates…",
                            style = MaterialTheme.typography.bodySmall,
                            color = SumvaloTheme.colors.subtleText,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                }
                is ChangelogUiState.Success -> {
                    if (state.releases.isEmpty()) {
                        renderEmpty()
                    } else {
                        renderReleases(state.releases)
                        if (state.fromCache) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Showing cached releases — check for updates to see the latest.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SumvaloTheme.colors.subtleText,
                                modifier = Modifier.padding(horizontal = 20.dp),
                            )
                        }
                    }
                }
                is ChangelogUiState.Error -> {
                    renderReleases(state.releases)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = SumvaloTheme.colors.alert,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "Unable to check for updates",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SumvaloTheme.colors.ink,
                            )
                            Text(
                                state.exception.message ?: "Network error",
                                style = MaterialTheme.typography.bodySmall,
                                color = SumvaloTheme.colors.subtleText,
                                maxLines = 2,
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        TextButton(
                            onClick = { viewModel.refresh() },
                        ) {
                            Text("Retry")
                        }
                    }
                }
                is ChangelogUiState.Empty -> {
                    renderEmpty()
                }
            }
        }
    }
}

@Composable
private fun renderReleases(releases: List<ReleaseEntry>) {
    val colors = SumvaloTheme.colors
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
                            color = colors.ink,
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                        Text(
                            "v${release.tag} · ${release.publishedDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.subtleText,
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.Publish,
                        contentDescription = null,
                        tint = colors.tertiaryText,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                release.changes.groupBy { it.category }.forEach { (category, items) ->
                    renderCategory(category, items, colors)
                }
                release.body?.let { body ->
                    if (release.changes.isEmpty() && body.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            body.take(500),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.subtleText,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
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
