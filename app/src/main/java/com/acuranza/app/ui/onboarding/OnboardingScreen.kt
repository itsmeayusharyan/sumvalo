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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.acuranza.app.R
import com.acuranza.app.designsystem.components.SumvaloLogo
import com.acuranza.app.designsystem.components.PrimaryButton
import com.acuranza.app.designsystem.theme.SumvaloTheme
import com.acuranza.app.model.Bank
import com.acuranza.app.ui.common.LocalContainer
import com.acuranza.app.ui.common.label
import com.acuranza.app.designsystem.theme.SumvaloText
import kotlinx.coroutines.launch

private const val STEPS = 5

/** The one step that cannot be skipped: the notification permission is required before the app opens. */
private const val NOTIFICATIONS_STEP = 3

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
                1 -> FeaturesStep(onBack = { step = 0 }, onNext = { step = 2 })
                2 -> HistoryStep(onBack = { step = 1 }, onNext = { step = 3 })
                3 -> NotificationsStep(onBack = { step = 2 }, onNext = { step = 4 })
                else -> DoneStep(finish)
            }
        }
    }
}



/** Chakra Petch — bundled under res/font (SIL OFL). */
private val Chakra = FontFamily(
    Font(R.font.chakrapetch_medium, FontWeight.Medium),
    Font(R.font.chakrapetch_semibold, FontWeight.SemiBold),
    Font(R.font.chakrapetch_bold, FontWeight.Bold),
)

private fun chakraText(weight: FontWeight, size: Int, lineHeight: Int = size + 6, letterSpacingSp: Float = 0f): TextStyle =
    TextStyle(fontFamily = Chakra, fontWeight = weight, fontSize = size.sp, lineHeight = lineHeight.sp, letterSpacing = letterSpacingSp.sp)

/** Welcome screen (step 0 of 5): matches the Subframe reference design exactly. */
@Composable
internal fun Welcome(onNext: () -> Unit) {
    val colors = SumvaloTheme.colors

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.cardSurface),
    ) {
        // Header: logo + SUMVALO + Skip
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.cardSurface)
                    .border(1.dp, colors.hairline, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) { SumvaloLogo(size = 28.dp) }
            Spacer(Modifier.width(12.dp))
            Text(
                "SUMVALO",
                style = chakraText(FontWeight.Bold, 17, 22),
                color = colors.ink,
            )
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onNext)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    "Skip",
                    style = chakraText(FontWeight.Bold, 15, 20),
                    color = colors.ink,
                )
            }
        }

        // Hero card
        Box(
            Modifier
                .fillMaxWidth()
                .height(430.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFF1F1F1F))
                .border(1.dp, colors.hairline, RoundedCornerShape(32.dp)),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "9:41",
                    style = chakraText(FontWeight.Bold, 14, 18),
                    color = colors.ink,
                )
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusIcon(Icons.Outlined.KeyboardArrowLeft)
                    StatusIcon(Icons.Outlined.KeyboardArrowRight)
                    StatusIcon(Icons.Outlined.KeyboardArrowUp)
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.cardSurface),
                    contentAlignment = Alignment.Center,
                ) { SumvaloLogo(size = 28.dp) }
                Spacer(Modifier.width(12.dp))
                Text(
                    "SUMVALO",
                    style = chakraText(FontWeight.Bold, 15, 20),
                    color = colors.ink,
                )
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .clickable(onClick = onNext)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        "Skip",
                        style = chakraText(FontWeight.Bold, 15, 20),
                        color = colors.ink,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Box(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                ) {
                    Box(
                        Modifier
                            .size(260.dp, 260.dp)
                            .clip(RoundedCornerShape(9999.dp))
                            .background(Color(0xFF2A2A2A))
                            .padding(end = 40.dp),
                    )
                    Box(
                        Modifier
                            .size(10.dp, 20.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0E0E0E))
                            .padding(top = 22.dp),
                    )
                    Box(
                        Modifier
                            .size(5.dp, 5.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(top = 78.dp, end = 40.dp),
                    )
                    Box(
                    Modifier
                        .size(184.dp, 184.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .size(148.dp, 148.dp)
                                .clip(CircleShape)
                                .border(6.dp, Color(0xFF0E0E0E), CircleShape),
                        ) {
                            Text(
                                "₹",
                                style = chakraText(FontWeight.Bold, 92, 92),
                                color = Color(0xFF0E0E0E),
                            )
                        }
                    }
                }

                Box(
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                    Box(
                        Modifier
                            .size(12.dp, 20.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF3A3A3A)),
                    )
                    Box(
                        Modifier
                            .size(12.dp, 32.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0E0E0E)),
                    )
                    Box(
                        Modifier
                            .size(12.dp, 44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White),
                    )
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(colors.cardSurface)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("▲", style = chakraText(FontWeight.Bold, 14, 18), color = Color(0xFF6FBF8B))
                            Text("2.4% this month", style = chakraText(FontWeight.Bold, 14, 18), color = Color(0xFF6FBF8B))
                        }
                    }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Progress dots + page label
        ProgressRow(0)

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Take control of your money.",
                style = chakraText(FontWeight.Bold, 44, 42, -0.02f),
                color = Color.White,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Track income, expenses, budgets and goals — privately, on your phone.",
                style = chakraText(FontWeight.Normal, 16, 23),
                color = Color(0xFFA3A3A3),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))

            // Feature cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FeatureCard(Icons.Outlined.Lock, "On-device")
                FeatureCard(Icons.Outlined.Lock, "No account")
                FeatureCard(Icons.Outlined.Lock, "No ads")
            }

            Spacer(Modifier.height(24.dp))

            // Get started button
            Box(
                Modifier.fillMaxWidth().clickable(onClick = onNext),
            ) {
                Box(
                    Modifier
                        .height(56.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Get started",
                            style = chakraText(FontWeight.Bold, 18, 22),
                            color = Color(0xFF0E0E0E),
                        )
                        Spacer(Modifier.width(12.dp))
                        Icon(
                            Icons.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color(0xFF0E0E0E),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "Takes less than a minute",
                style = chakraText(FontWeight.Medium, 13, 16),
                color = Color(0xFF6B6B6B),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun FeatureCard(icon: ImageVector, label: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1A1A1A))
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(44.dp, 44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF2A2A2A)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp)) }
            Spacer(Modifier.width(12.dp))
            Text(
                label,
                style = chakraText(FontWeight.Bold, 14, 18),
                color = Color.White,
            )
        }
    }
}

@Composable
private fun StatusIcon(icon: ImageVector, tint: Color = Color(0xFFA3A3A3)) {
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
}

/**
 * The permission step. The primary button asks for POST_NOTIFICATIONS until granted; Skip and
 * "Maybe later" let the user proceed without it (notifications can be enabled later in Settings).
 */
@Composable
private fun NotificationsStep(onBack: () -> Unit, onNext: () -> Unit) {
    val granted by rememberNotificationPermission()
    val request = rememberNotificationRequest()
    var requested by rememberSaveable { mutableStateOf(false) }
    val ask: () -> Unit = {
        requested = true
        request()
    }

    var cardBills by rememberSaveable { mutableStateOf(true) }
    var spendingAlerts by rememberSaveable { mutableStateOf(true) }
    var planned by rememberSaveable { mutableStateOf(true) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0E0E0E)),
    ) {
        // Hero card
        Box(
            Modifier
                .fillMaxWidth()
                .height(270.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFF1F1F1F))
                .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(32.dp)),
        ) {
            // Status bar row
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "9:41",
                    style = chakraText(FontWeight.Bold, 14, 18),
                    color = Color.White,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusIcon(Icons.Outlined.KeyboardArrowLeft, tint = Color.White)
                    StatusIcon(Icons.Outlined.KeyboardArrowRight, tint = Color.White)
                    StatusIcon(Icons.Outlined.KeyboardArrowUp, tint = Color.White)
                }
            }

            // Header row: back button + Skip
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Default.KeyboardArrowLeft,
                        contentDescription = null,
                        tint = Color(0xFF0E0E0E),
                        modifier = Modifier.size(22.dp),
                    )
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .clickable(onClick = onNext)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        "Skip",
                        style = chakraText(FontWeight.Bold, 15, 20),
                        color = Color.White,
                    )
                }
            }

            // Hero decorative content
            Box(Modifier.fillMaxSize()) {
                // Grey circle backdrop
                Box(
                    Modifier
                        .size(200.dp)
                        .offset(x = 150.dp, y = 10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2A2A2A)),
                )
                // White dots
                Box(
                    Modifier
                        .size(20.dp)
                        .offset(x = 40.dp, y = 28.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                )
                Box(
                    Modifier
                        .size(20.dp)
                        .offset(x = 226.dp, y = 0.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                )
                // Rotated dark square
                Box(
                    Modifier
                        .size(32.dp)
                        .offset(x = 100.dp, y = 40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0E0E0E))
                        .graphicsLayer { rotationZ = 18f },
                )
                // Rotated dark diamond
                Box(
                    Modifier
                        .size(20.dp)
                        .offset(x = 322.dp, y = 30.dp)
                        .graphicsLayer { rotationZ = 45f }
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF0E0E0E)),
                )
                // Bell body (white pill)
                Box(
                    Modifier
                        .size(112.dp, 104.dp)
                        .offset(x = 180.dp, y = 12.dp)
                        .clip(RoundedCornerShape(56.dp))
                        .background(Color.White),
                )
                // Bell rim hard shadow + bar
                Box(
                    Modifier
                        .size(156.dp, 24.dp)
                        .offset(x = 158.dp, y = 112.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF0E0E0E)),
                )
                Box(
                    Modifier
                        .size(156.dp, 24.dp)
                        .offset(x = 158.dp, y = 104.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White),
                )
                // Clapper
                Box(
                    Modifier
                        .size(32.dp)
                        .offset(x = 220.dp, y = 134.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0E0E0E)),
                )
                // Red badge with count
                Box(
                    Modifier
                        .size(48.dp)
                        .offset(x = 262.dp, y = (-4).dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD9776F))
                        .border(4.dp, Color(0xFF1F1F1F), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "3",
                        style = chakraText(FontWeight.Bold, 20, 22),
                        color = Color.White,
                    )
                }
                // Bill-due chip (rotated -7deg, hard shadow)
                Box(
                    Modifier
                        .offset(x = 18.dp, y = 118.dp)
                        .graphicsLayer { rotationZ = -7f },
                ) {
                    Box(
                        Modifier
                            .offset(y = 6.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF3A3A3A)),
                    ) {
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF0E0E0E))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFD9776F)))
                            Text(
                                "Bill due in 3 days",
                                style = chakraText(FontWeight.Bold, 15, 18),
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Progress dots + page label
        ProgressRow(3)

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) {
            Text(
                "Turn on notifications.",
                style = chakraText(FontWeight.Bold, 40, 40, -0.02f),
                color = Color.White,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Timely reminders for bills, unusual spending, warranties and scheduled payments.",
                style = chakraText(FontWeight.Normal, 15, 21),
                color = Color(0xFFA3A3A3),
            )
        }

        Spacer(Modifier.height(20.dp))

        // Notification preference toggles
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NotifRow(
                icon = Icons.Outlined.CalendarToday,
                title = "Card bills",
                text = "A reminder before each due date.",
                on = cardBills,
                onToggle = { cardBills = !cardBills },
            )
            NotifRow(
                icon = Icons.Outlined.NotificationsNone,
                title = "Spending alerts",
                text = "Unusual spending or a budget running hot.",
                on = spendingAlerts,
                onToggle = { spendingAlerts = !spendingAlerts },
            )
            NotifRow(
                icon = Icons.Outlined.EventAvailable,
                title = "Things you planned",
                text = "Subscriptions and scheduled payments.",
                on = planned,
                onToggle = { planned = !planned },
            )
        }

        if (requested && !granted) {
            Spacer(Modifier.height(12.dp))
            NotificationDeniedHelp(Modifier.fillMaxWidth().padding(horizontal = 20.dp))
        }

        Spacer(Modifier.weight(1f))

        // Primary button + Maybe later
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.fillMaxWidth().clickable(onClick = { if (granted) onNext() else ask() }),
            ) {
                Box(
                    Modifier
                        .height(64.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 28.dp, end = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (granted) "Continue" else "Allow notifications",
                            style = chakraText(FontWeight.Bold, 18, 22),
                            color = Color(0xFF0E0E0E),
                        )
                        Box(
                            Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0E0E0E)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (granted) Icons.Outlined.KeyboardArrowRight else Icons.Outlined.NotificationsNone,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(9999.dp))
                    .clickable(onClick = onNext)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    "Maybe later",
                    style = chakraText(FontWeight.Bold, 15, 20),
                    color = Color(0xFFA3A3A3),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Toggle switch matching the reference: light track, dark knob. */
@Composable
private fun NotifToggle(on: Boolean, onToggle: () -> Unit) {
    Box(
        Modifier
            .size(52.dp, 30.dp)
            .clip(RoundedCornerShape(50))
            .background(if (on) Color(0xFFF5F5F5) else Color(0xFF3A3A3A))
            .clickable(onClick = onToggle)
            .padding(horizontal = 3.dp),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (on) Color(0xFF0E0E0E) else Color(0xFF8A8A8A)),
        )
    }
}

/** Notification preference row: icon + title + description + toggle. */
@Composable
private fun NotifRow(icon: ImageVector, title: String, text: String, on: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1A1A1A))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF262626)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = chakraText(FontWeight.Bold, 16, 20), color = Color.White)
            Spacer(Modifier.height(2.dp))
            Text(text, style = chakraText(FontWeight.Normal, 13, 18), color = Color(0xFFA3A3A3))
        }
        Spacer(Modifier.width(12.dp))
        NotifToggle(on, onToggle)
    }
}


/** One selectable entry-method row on the History step. */
@Composable
private fun HistoryOptionRow(
    icon: ImageVector,
    title: String,
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Color.White else Color(0xFF1A1A1A))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) Color(0xFF0E0E0E) else Color(0xFF262626)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = chakraText(FontWeight.Bold, 16, 20),
                color = if (selected) Color(0xFF0E0E0E) else Color.White,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text,
                style = chakraText(if (selected) FontWeight.Medium else FontWeight.Normal, 13, 18),
                color = if (selected) Color(0xFF0E0E0E) else Color(0xFFA3A3A3),
            )
        }
        Spacer(Modifier.width(12.dp))
        if (selected) {
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF0E0E0E)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        } else {
            Box(Modifier.size(32.dp).clip(CircleShape).border(3.dp, Color(0xFF3A3A3A), CircleShape))
        }
    }
}

/** One text bar inside the hero receipt card. */
@Composable
private fun ReceiptBar(width: Dp?, height: Dp) {
    val modifier = if (width == null) Modifier.fillMaxWidth() else Modifier.size(width, height)
    Box(modifier.clip(RoundedCornerShape(50)).background(Color(0xFF0E0E0E)))
}

/** History screen (step 2 of 5): matches the Subframe reference design exactly. */
@Composable
internal fun HistoryStep(onBack: () -> Unit, onNext: () -> Unit) {
    var selected by rememberSaveable { mutableIntStateOf(0) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0E0E0E)),
    ) {
        // Hero card
        Box(
            Modifier
                .fillMaxWidth()
                .height(256.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFF1F1F1F))
                .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(32.dp)),
        ) {
            // Status bar row
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "9:41",
                    style = chakraText(FontWeight.Bold, 14, 18),
                    color = Color.White,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusIcon(Icons.Outlined.KeyboardArrowLeft, tint = Color.White)
                    StatusIcon(Icons.Outlined.KeyboardArrowRight, tint = Color.White)
                    StatusIcon(Icons.Outlined.KeyboardArrowUp, tint = Color.White)
                }
            }

            // Header row: back button + Skip
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Default.KeyboardArrowLeft,
                        contentDescription = null,
                        tint = Color(0xFF0E0E0E),
                        modifier = Modifier.size(22.dp),
                    )
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .clickable(onClick = onNext)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        "Skip",
                        style = chakraText(FontWeight.Bold, 15, 20),
                        color = Color.White,
                    )
                }
            }

            // Hero decorative content
            Box(Modifier.fillMaxSize()) {
                // Big grey circle backdrop (right)
                Box(
                    Modifier
                        .size(190.dp)
                        .offset(x = 196.dp, y = 34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2A2A2A)),
                )
                // White dot (left)
                Box(
                    Modifier
                        .size(20.dp)
                        .offset(x = 30.dp, y = 118.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                )
                // Rotated dark square (right edge)
                Box(
                    Modifier
                        .size(32.dp)
                        .offset(x = 322.dp, y = 104.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0E0E0E))
                        .graphicsLayer { rotationZ = 18f },
                )
                // Receipt card (rotated -6deg, hard shadow)
                Box(
                    Modifier
                        .offset(x = 132.dp, y = 12.dp)
                        .size(112.dp, 150.dp)
                        .graphicsLayer { rotationZ = -6f },
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .offset(y = 10.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF0E0E0E)),
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White)
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ReceiptBar(width = 56.dp, height = 12.dp)
                            ReceiptBar(width = null, height = 10.dp)
                            ReceiptBar(width = 64.dp, height = 10.dp)
                            ReceiptBar(width = null, height = 10.dp)
                            ReceiptBar(width = 48.dp, height = 10.dp)
                        }
                    }
                }
                // ₹ speech-bubble tile (rotated 8deg)
                Box(
                    Modifier
                        .offset(x = 40.dp, y = 22.dp)
                        .size(88.dp, 66.dp)
                        .graphicsLayer { rotationZ = 8f }
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF0E0E0E)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "₹",
                        style = chakraText(FontWeight.Bold, 38, 40),
                        color = Color.White,
                    )
                }
                // Speech-bubble tail (diamond)
                Box(
                    Modifier
                        .size(20.dp)
                        .offset(x = 58.dp, y = 78.dp)
                        .graphicsLayer { rotationZ = 45f }
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF0E0E0E)),
                )
                // Repeat-circle hard shadow
                Box(
                    Modifier
                        .size(78.dp)
                        .offset(x = 232.dp, y = 74.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0E0E0E)),
                )
                // Repeat circle (white with icon)
                Box(
                    Modifier
                        .size(78.dp)
                        .offset(x = 232.dp, y = 66.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Repeat,
                        contentDescription = null,
                        tint = Color(0xFF0E0E0E),
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Progress dots + page label
        ProgressRow(2)

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) {
            Text(
                "Your history, your way.",
                style = chakraText(FontWeight.Bold, 40, 40, -0.02f),
                color = Color.White,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Track transactions, enable SMS tracking, import a bank statement or add entries manually — it's up to you.",
                style = chakraText(FontWeight.Normal, 15, 21),
                color = Color(0xFFA3A3A3),
            )
        }

        Spacer(Modifier.height(20.dp))

        // Entry-method options
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HistoryOptionRow(
                icon = Icons.Outlined.Edit,
                title = "Quick manual entry",
                text = "Tags, splits and receipts in seconds.",
                selected = selected == 0,
                onClick = { selected = 0 },
            )
            HistoryOptionRow(
                icon = Icons.Outlined.Sms,
                title = "SMS tracking",
                text = "Auto-logs bank SMS, on-device.",
                selected = selected == 1,
                onClick = { selected = 1 },
            )
            HistoryOptionRow(
                icon = Icons.Outlined.Description,
                title = "Bank statement import",
                text = "CSV from HDFC, ICICI, SBI and more.",
                selected = selected == 2,
                onClick = { selected = 2 },
            )
            HistoryOptionRow(
                icon = Icons.Outlined.Repeat,
                title = "Recurring on autopilot",
                text = "Rent, SIPs and subscriptions.",
                selected = selected == 3,
                onClick = { selected = 3 },
            )
        }

        Spacer(Modifier.weight(1f))

        // Continue button
        Box(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onNext)
                .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 32.dp),
        ) {
            Box(
                Modifier
                    .height(64.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Continue",
                        style = chakraText(FontWeight.Bold, 18, 22),
                        color = Color(0xFF0E0E0E),
                    )
                    Spacer(Modifier.width(12.dp))
                    Icon(
                        Icons.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFF0E0E0E),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DoneStep(onFinish: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(0.7f))
        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = SumvaloTheme.colors.good, modifier = Modifier.size(84.dp))
        Spacer(Modifier.height(16.dp))
        Text("You're all set", style = SumvaloText.screenTitle)
        Spacer(Modifier.height(8.dp))
        Text(
            "Add bank accounts and cards any time from Accounts, or jump straight in and add transactions as you go.",
            style = MaterialTheme.typography.bodyLarge,
            color = SumvaloTheme.colors.subtleText,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        PrimaryButton("Open Sumvalo", onClick = onFinish, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
    }
}

/** Progress dots + page label, shared across onboarding steps. */
@Composable
private fun ProgressRow(activeIndex: Int) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(5) { i ->
                Box(
                    Modifier
                        .size(if (i == activeIndex) 36.dp else 12.dp, 12.dp)
                        .clip(if (i == activeIndex) RoundedCornerShape(50) else CircleShape)
                        .background(if (i == activeIndex) Color.White else Color(0xFF2A2A2A)),
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            "${activeIndex + 1} / 05",
            style = chakraText(FontWeight.Bold, 12, 16, 1.44f),
            color = Color(0xFF6B6B6B),
        )
    }
}

/** One bar in the hero bar chart: white top segment over a colored bottom segment. */
@Composable
private fun BarColumn(topColor: Color, topHeight: Dp, bottomColor: Color, bottomHeight: Dp) {
    Box(Modifier.size(44.dp)) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
        ) {
            Box(Modifier.width(44.dp).height(topHeight).clip(RoundedCornerShape(10.dp)).background(topColor))
            Spacer(Modifier.height(4.dp))
            Box(Modifier.width(44.dp).height(bottomHeight).clip(RoundedCornerShape(12.dp)).background(bottomColor))
        }
    }
}

/** Feature row: icon circle + title + description. */
@Composable
private fun FeatureRow(icon: ImageVector, iconBg: Color, iconTint: Color, title: String, text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1A1A1A)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = chakraText(FontWeight.Bold, 16, 20), color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(text, style = chakraText(FontWeight.Normal, 14, 19), color = Color(0xFFA3A3A3))
        }
    }
}

/** Features screen (step 1 of 5): matches the Subframe reference design exactly. */
@Composable
internal fun FeaturesStep(onBack: () -> Unit, onNext: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0E0E0E)),
    ) {
        // Hero card
        Box(
            Modifier
                .fillMaxWidth()
                .height(320.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFF1F1F1F))
                .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(32.dp)),
        ) {
            // Status bar row
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "9:41",
                    style = chakraText(FontWeight.Bold, 14, 18),
                    color = Color.White,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusIcon(Icons.Outlined.KeyboardArrowLeft)
                    StatusIcon(Icons.Outlined.KeyboardArrowRight)
                    StatusIcon(Icons.Outlined.KeyboardArrowUp)
                }
            }

            // Header row: back button + Skip
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Default.KeyboardArrowLeft,
                        contentDescription = null,
                        tint = Color(0xFF0E0E0E),
                        modifier = Modifier.size(22.dp),
                    )
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .clickable(onClick = onNext)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        "Skip",
                        style = chakraText(FontWeight.Bold, 15, 20),
                        color = Color.White,
                    )
                }
            }

            // Hero decorative content
            Box(Modifier.fillMaxSize()) {
                // Big grey circle backdrop (top-right)
                Box(
                    Modifier
                        .size(230.dp)
                        .offset(x = 170.dp, y = (-20).dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2A2A2A)),
                )
                // Rotated dark square (top-left)
                Box(
                    Modifier
                        .size(36.dp)
                        .offset(x = 34.dp, y = 12.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0E0E0E))
                        .graphicsLayer { rotationZ = 18f },
                )
                // White dot
                Box(
                    Modifier
                        .size(20.dp)
                        .offset(x = 104.dp, y = 30.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                )
                // Hard offset shadow under target
                Box(
                    Modifier
                        .size(164.dp)
                        .offset(x = 186.dp, y = 22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0E0E0E)),
                )
                // Target concentric circles
                Box(
                    Modifier
                        .size(164.dp)
                        .offset(x = 186.dp, y = 8.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.size(128.dp).clip(CircleShape).background(Color(0xFF0E0E0E)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier.size(92.dp).clip(CircleShape).background(Color.White),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier.size(58.dp).clip(CircleShape).background(Color(0xFF0E0E0E)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    Modifier.size(22.dp).clip(CircleShape).background(Color.White)
                                )
                            }
                        }
                    }
                }
                // Bar chart (bottom-left)
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 22.dp, bottom = 22.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        BarColumn(topColor = Color.White, topHeight = 24.dp, bottomColor = Color(0xFF3A3A3A), bottomHeight = 40.dp)
                        BarColumn(topColor = Color.White, topHeight = 32.dp, bottomColor = Color(0xFF0E0E0E), bottomHeight = 64.dp)
                        BarColumn(topColor = Color(0xFF0E0E0E), topHeight = 40.dp, bottomColor = Color.White, bottomHeight = 96.dp)
                    }
                }
                // Shield with check mark (rotated -6deg)
                Box(
                    Modifier.offset(x = 148.dp, y = 96.dp).size(96.dp).graphicsLayer { rotationZ = -6f },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.VerifiedUser,
                        contentDescription = null,
                        tint = Color(0xFF0E0E0E),
                        modifier = Modifier.size(96.dp),
                    )
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(52.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Progress dots + page label
        ProgressRow(1)

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Track, plan and stay in control.",
                style = chakraText(FontWeight.Bold, 40, 40, -0.02f),
                color = Color.White,
            )
            Spacer(Modifier.height(24.dp))

            // Feature rows
            Column(Modifier.fillMaxWidth()) {
                FeatureRow(
                    icon = Icons.Outlined.BarChart,
                    iconBg = Color(0xFF2A2A2A),
                    iconTint = Color.White,
                    title = "Track everything",
                    text = "Income, expenses and card dues in one place.",
                )
                Spacer(Modifier.height(8.dp))
                FeatureRow(
                    icon = Icons.Outlined.Adjust,
                    iconBg = Color.White,
                    iconTint = Color(0xFF0E0E0E),
                    title = "Plan with confidence",
                    text = "Budgets, savings goals and safe-to-spend pace.",
                )
                Spacer(Modifier.height(8.dp))
                FeatureRow(
                    icon = Icons.Outlined.VerifiedUser,
                    iconBg = Color(0xFF2A2A2A),
                    iconTint = Color.White,
                    title = "Private by design",
                    text = "No account, no cloud, no tracking. Your data stays on your device.",
                )
            }

            Spacer(Modifier.weight(1f))

            // Continue button
            Box(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNext)
                    .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 32.dp),
            ) {
                Box(
                    Modifier
                        .height(64.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Continue",
                            style = chakraText(FontWeight.Bold, 18, 22),
                            color = Color(0xFF0E0E0E),
                        )
                        Spacer(Modifier.width(12.dp))
                        Icon(
                            Icons.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color(0xFF0E0E0E),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}

