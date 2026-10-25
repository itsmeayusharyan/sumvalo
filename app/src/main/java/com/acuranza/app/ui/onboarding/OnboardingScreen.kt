package com.acuranza.app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.acuranza.app.designsystem.components.AcuranzaLogo
import com.acuranza.app.designsystem.components.PrimaryButton
import com.acuranza.app.designsystem.theme.AcuranzaTheme
import com.acuranza.app.model.Bank
import com.acuranza.app.ui.common.LocalContainer
import com.acuranza.app.ui.common.label
import com.acuranza.app.designsystem.theme.AcuranzaText
import kotlinx.coroutines.launch

private const val STEPS = 4

/** The one step that cannot be skipped: the notification permission is required before the app opens. */
private const val NOTIFICATIONS_STEP = 1

@Composable
fun OnboardingScreen() {
    val slideSpec = com.acuranza.app.designsystem.theme.motionTween<androidx.compose.ui.unit.IntOffset>(com.acuranza.app.designsystem.theme.Motion.EMPHASIZED)
    val fadeSpec = com.acuranza.app.designsystem.theme.motionTween<Float>(com.acuranza.app.designsystem.theme.Motion.STANDARD)
    val fadeSpecOut = com.acuranza.app.designsystem.theme.motionTween<Float>(com.acuranza.app.designsystem.theme.Motion.FAST)
    val c = LocalContainer.current
    var step by rememberSaveable { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val finish: () -> Unit = { scope.launch { c.settings.setOnboardingDone(true) } }
    BackHandler(enabled = step > 0) { step-- }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            StepDots(step, STEPS, Modifier.weight(1f))
            // The notification step is the gate: it is the only one without a Skip.
            if (step in 1 until STEPS - 1 && step != NOTIFICATIONS_STEP) TextButton(onClick = { step++ }) { Text("Skip") }
        }
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally(slideSpec) { if (forward) it / 3 else -it / 3 } + fadeIn(fadeSpec)) togetherWith
                    (slideOutHorizontally(slideSpec) { if (forward) -it / 3 else it / 3 } + fadeOut(fadeSpecOut))
            },
            label = "onboarding",
            modifier = Modifier.weight(1f),
        ) { s ->
            when (s) {
                0 -> Welcome { step = 1 }
                1 -> NotificationsStep { step = 2 }
                2 -> ImportStep { step = 3 }
                else -> DoneStep(finish)
            }
        }
    }
}

@Composable
private fun StepDots(step: Int, total: Int, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            val w by animateDpAsState(if (i == step) 22.dp else 8.dp, label = "dot")
            val color by animateColorAsState(
                if (i <= step) MaterialTheme.colorScheme.primary else AcuranzaTheme.colors.divider,
                label = "dotColor",
            )
            Box(Modifier.height(8.dp).width(w).clip(CircleShape).background(color))
        }
    }
}

@Composable
private fun StepPage(
    title: String,
    subtitle: String,
    primary: String,
    onPrimary: () -> Unit,
    primaryEnabled: Boolean = true,
    secondary: String? = null,
    onSecondary: (() -> Unit)? = null,
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if (icon != null) {
                Box(
                    Modifier
                        .padding(top = 8.dp)
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(AcuranzaTheme.colors.ink),
                    contentAlignment = Alignment.Center,
                ) { Icon(icon, contentDescription = null, tint = AcuranzaTheme.colors.onInk) }
                Spacer(Modifier.height(16.dp))
            }
            Text(title, style = AcuranzaText.screenTitle)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = AcuranzaTheme.colors.subtleText)
            Spacer(Modifier.height(20.dp))
            content()
            Spacer(Modifier.height(16.dp))
        }
        PrimaryButton(primary, onClick = onPrimary, enabled = primaryEnabled, modifier = Modifier.fillMaxWidth())
        if (secondary != null && onSecondary != null) {
            TextButton(onClick = onSecondary, modifier = Modifier.fillMaxWidth()) { Text(secondary) }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
internal fun Welcome(onNext: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(0.6f))
        AcuranzaLogo(size = 108.dp)
        Spacer(Modifier.height(24.dp))
        Text("Welcome to Sumvalo", style = AcuranzaText.screenTitle, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "Track income, expenses, budgets and goals — privately, on your phone.",
            style = MaterialTheme.typography.bodyLarge, color = AcuranzaTheme.colors.subtleText, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        ValueProp(Icons.Outlined.Assessment, "Track everything", "Income, expenses and card dues in one place, entered in seconds.")
        ValueProp(Icons.Outlined.AccountBalanceWallet, "Plan with confidence", "Budgets, savings goals and safe-to-spend pace, computed on your phone.")
        ValueProp(Icons.Outlined.Lock, "Private by design", "No account, no cloud, no tracking. Your money never leaves your device.")
        Spacer(Modifier.weight(1f))
        PrimaryButton("Get started", onClick = onNext, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ValueProp(icon: ImageVector, title: String, text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(text, style = MaterialTheme.typography.bodySmall, color = AcuranzaTheme.colors.subtleText)
        }
    }
}

/**
 * The one mandatory step: notifications. The primary button asks for the permission until it is
 * granted; a denial shows the settings path instead of letting the user into the app.
 */
@Composable
private fun NotificationsStep(onNext: () -> Unit) {
    val granted by rememberNotificationPermission()
    val request = rememberNotificationRequest()

    StepPage(
        title = "Turn on notifications",
        subtitle = "Sumvalo is offline, so notifications are how it reaches you: bill due dates, unusual spending, " +
            "warranties ending and payments you scheduled — all composed on your phone.",
        primary = if (granted) "Continue" else "Allow notifications",
        onPrimary = { if (granted) onNext() else request() },
        icon = Icons.Outlined.NotificationsActive,
    ) {
        NotificationReasons()
        if (granted) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = AcuranzaTheme.colors.good)
                Spacer(Modifier.width(8.dp))
                Text("Notifications are on", fontWeight = FontWeight.Medium)
            }
        } else {
            Spacer(Modifier.height(12.dp))
            NotificationDeniedHelp(Modifier.fillMaxWidth())
        }
    }
}


@Composable
private fun ImportStep(onNext: () -> Unit) {
    StepPage(
        title = "Your history",
        subtitle = "Track transactions your way. Enable SMS transaction tracking to help detect bank " +
            "transactions automatically, or import a bank statement CSV or add transactions manually. " +
            "SMS access is optional and controlled by you.",
        primary = "Continue",
        onPrimary = onNext,
        icon = Icons.Outlined.AutoAwesome,
    ) {
        ValueProp(Icons.Outlined.Edit, "Quick manual entry", "A calculator keypad with tags, splits and receipts attached.")
        ValueProp(Icons.Outlined.Assessment, "Bank statement import", "CSV files from HDFC, ICICI, SBI, Axis, Kotak and more — from More › Import statement.")
        ValueProp(Icons.Outlined.Autorenew, "Recurring on autopilot", "Rent, SIPs and subscriptions you schedule add themselves on their dates.")
    }
}

@Composable
private fun DoneStep(onFinish: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(0.7f))
        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = AcuranzaTheme.colors.good, modifier = Modifier.size(84.dp))
        Spacer(Modifier.height(16.dp))
        Text("You're all set", style = AcuranzaText.screenTitle)
        Spacer(Modifier.height(8.dp))
        Text(
            "Add bank accounts and cards any time from Accounts, or jump straight in and add transactions as you go.",
            style = MaterialTheme.typography.bodyLarge,
            color = AcuranzaTheme.colors.subtleText,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        PrimaryButton("Open Sumvalo", onClick = onFinish, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
    }
}
