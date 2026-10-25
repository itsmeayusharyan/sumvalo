package com.acuranza.app.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.acuranza.app.designsystem.components.EmptyState
import com.acuranza.app.designsystem.theme.AcuranzaTheme
import com.acuranza.app.repo.NotificationItem
import com.acuranza.app.ui.common.AcuranzaScreen
import com.acuranza.app.ui.common.LocalContainer
import com.acuranza.app.ui.common.LocalHideAmounts
import com.acuranza.app.ui.common.LocalSettings
import com.acuranza.app.ui.nav.Routes
import com.acuranza.app.ui.nav.navigateToTab
import com.acuranza.app.ui.nav.popBackSafely
import kotlinx.coroutines.launch

/**
 * The in-app notification center: card-bill dues and reminders, important insights and items
 * waiting for review, all derived from local data. Tapping an item marks it read and opens it.
 */
@Composable
fun NotificationsScreen(nav: NavController) {
    val c = LocalContainer.current
    val hidden = LocalHideAmounts.current
    val scope = rememberCoroutineScope()
    val items by c.notificationCenter.observeNotifications().collectAsStateWithLifecycle(initialValue = null)
    NotificationsContent(
        items = items,
        hidden = hidden,
        onOpen = { item ->
            scope.launch { c.notificationCenter.markRead(listOf(item.id)) }
            when (item.route) {
                Routes.INSIGHTS -> nav.navigateToTab(Routes.INSIGHTS)
                null -> Unit
                else -> nav.navigate(item.route)
            }
        },
        onMarkAllRead = { scope.launch { c.notificationCenter.markRead(items.orEmpty().map { it.id }) } },
        onBack = { nav.popBackSafely() },
    )
}

/** Stateless notification center, rendered from [items]; used by the app and by screenshot tests. */
@Composable
fun NotificationsContent(
    items: List<NotificationItem>?,
    hidden: Boolean,
    onOpen: (NotificationItem) -> Unit = {},
    onMarkAllRead: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val list = items.orEmpty()
    val unread = list.count { it.unread }
    AcuranzaScreen(
        title = "Notifications",
        subtitle = if (unread > 0) "$unread unread" else null,
        onBack = onBack,
        actions = {
            if (items != null && unread > 0) {
                TextButton(onClick = onMarkAllRead) { Text("Mark all read") }
            }
        },
    ) { padding ->
        if (items == null) return@AcuranzaScreen
        if (list.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
                EmptyState(
                    "You're all caught up",
                    "Bill reminders, important insights and items to review will appear here.",
                    icon = Icons.Outlined.Inbox,
                )
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 40.dp),
            ) {
                items(list.size, key = { list[it].id }) { i ->
                    val item = list[i]
                    NotificationRow(item, hidden) { onOpen(item) }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(item: NotificationItem, hidden: Boolean, onClick: () -> Unit) {
    val colors = AcuranzaTheme.colors
    val (tile, icon, tint) = when (item.kind) {
        NotificationItem.Kind.BILL -> Triple(colors.accentTint, Icons.Outlined.CreditCard, colors.accent)
        NotificationItem.Kind.INSIGHT -> Triple(colors.warningTint, Icons.Outlined.Bolt, colors.warning)
        NotificationItem.Kind.REVIEW -> Triple(colors.fill, Icons.Outlined.Inbox, colors.ink)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tile),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (item.unread) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!hidden) {
                Text(
                    item.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.subtleText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(item.whenLabel, style = MaterialTheme.typography.labelSmall, color = colors.tertiaryText)
            Spacer(Modifier.size(6.dp))
            // Unread dot; an invisible spacer of the same size keeps rows aligned when read.
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (item.unread) colors.accent else androidx.compose.ui.graphics.Color.Transparent),
            )
        }
    }
}
