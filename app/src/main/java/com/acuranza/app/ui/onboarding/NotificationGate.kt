package com.acuranza.app.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.acuranza.app.designsystem.components.SumvaloCard
import com.acuranza.app.designsystem.components.SumvaloLogo
import com.acuranza.app.designsystem.components.PrimaryButton
import com.acuranza.app.designsystem.theme.SumvaloText
import com.acuranza.app.designsystem.theme.SumvaloTheme

/**
 * Whether the app may post notifications. Below Android 13 the permission does not exist and is
 * implicitly granted; on 13+ [Manifest.permission.POST_NOTIFICATIONS] must be allowed explicitly.
 */
fun notificationsGranted(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

/**
 * Tracks [notificationsGranted], re-checking whenever the app returns to the foreground — for example
 * after the user comes back from system settings.
 */
@Composable
fun rememberNotificationPermission(): MutableState<Boolean> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(notificationsGranted(context)) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) state.value = notificationsGranted(context)
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return state
}

/**
 * Starts the system notification-permission dialog. Never called automatically: only an explicit
 * button tap asks the user, so a denial never triggers a repeated prompt loop.
 */
@Composable
fun rememberNotificationRequest(): () -> Unit {
    val context = LocalContext.current
    val state = rememberNotificationPermission()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        state.value = notificationsGranted(context)
    }
    return {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            state.value = true
        }
    }
}

/** Opens this app's page in system notification settings. */
private fun openNotificationSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

/** What the notification permission is actually used for. */
@Composable
fun NotificationReasons() {
    Column(Modifier.fillMaxWidth()) {
        ReasonRow(Icons.Outlined.CreditCard, "Card bills", "A reminder before each due date, so interest and late fees never sneak up on you.")
        ReasonRow(Icons.Outlined.ReceiptLong, "Spending alerts", "A heads-up when an unusual spend is detected or a budget runs hot.")
        ReasonRow(Icons.Outlined.VerifiedUser, "Things you planned", "Subscriptions that got pricier, payments you scheduled, budgets that need attention.")
    }
}

@Composable
private fun ReasonRow(icon: ImageVector, title: String, text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(text, style = MaterialTheme.typography.bodySmall, color = SumvaloTheme.colors.subtleText)
        }
    }
}

/**
 * The help card shown after a denial: it explains the settings path without ever leaving the user
 * stuck, crashing or being asked over and over.
 */
@Composable
fun NotificationDeniedHelp(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    SumvaloCard(modifier) {
        Text("Notifications are off", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Sumvalo needs this permission to remind you. If Android didn't ask, the permission was " +
                "blocked earlier — turn it on for Sumvalo in system settings, then come back.",
            style = MaterialTheme.typography.bodySmall,
            color = SumvaloTheme.colors.subtleText,
        )
        TextButton(onClick = { openNotificationSettings(context) }) { Text("Open notification settings") }
    }
}

/**
 * The gate's visual content, with the permission state passed in. Kept separate from
 * [NotificationGateScreen] so it renders without an activity (screenshot tests, previews).
 */
@Composable
fun NotificationGateBody(granted: Boolean, onRequest: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(56.dp))
        SumvaloLogo(size = 72.dp)
        Spacer(Modifier.height(24.dp))
        Text("Turn on notifications", style = SumvaloText.screenTitle, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            "Sumvalo is fully offline, so notifications are the only way it can reach you. " +
                "Everything it sends is composed on your phone.",
            style = MaterialTheme.typography.bodyMedium,
            color = SumvaloTheme.colors.subtleText,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        NotificationReasons()
        Spacer(Modifier.height(24.dp))
        // When the permission arrives — from the dialog or from settings — the stage recomposes into
        // the app, so this button only ever asks; it never claims to be anything else.
        PrimaryButton("Allow notifications", onClick = onRequest, modifier = Modifier.fillMaxWidth())
        if (!granted) {
            Spacer(Modifier.height(12.dp))
            NotificationDeniedHelp(Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(32.dp))
    }
}

/**
 * The full-screen gate shown whenever the main app would open but the notification permission is
 * missing (first launch after onboarding, or a later revocation). The user stays here until the
 * permission is granted — no crash, no force-close, no other permissions requested.
 */
@Composable
fun NotificationGateScreen() {
    val granted by rememberNotificationPermission()
    val request = rememberNotificationRequest()
    NotificationGateBody(granted = granted, onRequest = request)
}
