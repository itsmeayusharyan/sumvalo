package com.acuranza.app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acuranza.app.R
import com.acuranza.app.ui.common.Chakra
import com.acuranza.app.designsystem.components.SumvaloLogo
import com.acuranza.app.ui.common.LocalContainer
import kotlinx.coroutines.launch

private const val STEPS = 5

/** Chakra Petch — bundled under res/font (SIL OFL). */

private fun chakraText(weight: FontWeight, size: Int, lineHeight: Int = size + 6, letterSpacingSp: Float = 0f): TextStyle =
    TextStyle(fontFamily = Chakra, fontWeight = weight, fontSize = size.sp, lineHeight = lineHeight.sp, letterSpacing = letterSpacingSp.sp)

// Palette is fixed-dark by design: onboarding always renders on #0E0E0E regardless of app theme.
private val OnboardingBg = Color(0xFF0E0E0E)
private val HeroBg = Color(0xFF1F1F1F)
private val Hairline = Color(0xFF2A2A2A)
private val CardBg = Color(0xFF1A1A1A)
private val TileBg = Color(0xFF262626)
private val SubtleText = Color(0xFFA3A3A3)
private val DotInactive = Color(0xFF2A2A2A)
private val DotLabel = Color(0xFF6B6B6B)
private val Green = Color(0xFF6FBF8B)
private val Coral = Color(0xFFD9776F)

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
            .background(OnboardingBg)
            .statusBarsPadding()
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
                else -> DoneStep(onBack = { step = 3 }, onFinish = finish)
            }
        }
    }
}

// ── Shared scaffolding ──────────────────────────────────────────────────────────────────────

/** Full-page scaffold shared by every step: scrollable body with the primary action pinned to
 * the bottom above the nav bar — matching the reference, where the CTA never scrolls away.
 * Steps manage their own internal padding and positioning to match the reference exactly.
 */
@Composable
private fun OnboardingPage(buttonContent: @Composable () -> Unit, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(OnboardingBg),
    ) {
        Box(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            content()
        }
        Column(
            Modifier.fillMaxWidth().background(OnboardingBg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                buttonContent()
            }
        }
    }
}

/** The rounded hero card every step opens with: 32dp corners, hairline border, fixed-dark surface. */
@Composable
private fun HeroCard(height: Dp, content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(32.dp))
            .background(HeroBg)
            .border(1.dp, Hairline, RoundedCornerShape(32.dp)),
        content = content,
    )
}

/**
 * Standard hero-card interior: status-bar row, nav row, then a decoration canvas filling the
 * rest. Decorations anchor to the canvas edges (never to absolute screen pixels), so the
 * composition survives any screen width without clipping or drifting.
 */
@Composable
private fun HeroCardScaffold(
    height: Dp,
    navRow: (@Composable () -> Unit)? = null,
    decorations: @Composable BoxScope.() -> Unit,
) {
    HeroCard(height = height) {
        Column(Modifier.fillMaxSize()) {
            HeroStatusBar()
            if (navRow != null) navRow()
            Box(Modifier.fillMaxSize(), content = decorations)
        }
    }
}

/** Row 1 inside every hero card: the fake phone status bar (time + signal/wifi/battery). */
@Composable
private fun HeroStatusBar() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("9:41", style = chakraText(FontWeight.Bold, 14, 18), color = Color.White)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            HeroStatusGlyph(Icons.Filled.SignalCellular4Bar)
            HeroStatusGlyph(Icons.Filled.Wifi)
            HeroStatusGlyph(Icons.Filled.BatteryFull)
        }
    }
}

@Composable
private fun HeroStatusGlyph(icon: ImageVector, tint: Color = Color.White) {
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
}

/** White circular back button (steps 2-5): white bg, dark arrow — matches the reference. */
@Composable
private fun BackButton(onBack: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(onClick = onBack),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = "Back",
            tint = OnboardingBg,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** Skip pill (steps 1-4). Transparent pill like the reference — no background by default. */
@Composable
private fun SkipButton(onSkip: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(9999.dp))
            .clickable(onClick = onSkip)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text("Skip", style = chakraText(FontWeight.Bold, 15, 20), color = Color.White)
    }
}

/** Header row 2: back on the left, Skip on the right; absent sides keep their slot for balance. */
@Composable
private fun HeroNavRow(onBack: (() -> Unit)?, onSkip: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (onBack != null) BackButton(onBack) else Spacer(Modifier.size(40.dp))
        if (onSkip != null) SkipButton(onSkip) else Spacer(Modifier.size(40.dp))
    }
}

/** Progress dots + page label — reference: 3dp dots, active is w-9 h-3. */
@Composable
private fun ProgressRow(activeIndex: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(STEPS) { i ->
                val active = i == activeIndex
                Box(
                    Modifier
                        .size(if (active) 36.dp else 12.dp, 12.dp)
                        .clip(if (active) RoundedCornerShape(50) else CircleShape)
                        .background(if (active) Color.White else DotInactive),
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            "%02d / %02d".format(activeIndex + 1, STEPS),
            style = chakraText(FontWeight.Bold, 12, 16, 1.44f),
            color = DotLabel,
        )
    }
}

/** Headline + subtitle block (px-6 pt-5 in the reference). */
@Composable
private fun HeadlineBlock(
    title: String,
    subtitle: String,
    titleSize: Int = 40,
    titleWeight: FontWeight = FontWeight.Bold,
    titleLineHeight: Int? = null,
    subtitleSize: Int = 15,
    subtitleLineHeight: Int? = null,
    letterSpacing: Float = 0f,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 20.dp)) {
        Text(
            title,
            style = chakraText(titleWeight, titleSize, titleLineHeight ?: titleSize, letterSpacing),
            color = Color.White,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            subtitle,
            style = chakraText(FontWeight.Normal, subtitleSize, subtitleLineHeight ?: subtitleSize + 6),
            color = SubtleText,
        )
    }
}

/** The white pill CTA shared by all steps: label left (28dp inset), dark circle glyph right. */
@Composable
private fun PillButton(label: String, icon: ImageVector, onClick: () -> Unit, height: Dp = 64.dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9999.dp))
            .background(Color.White)
            .clickable(onClick = onClick),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(height)
                .padding(start = 28.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = chakraText(FontWeight.Bold, 18, 22), color = OnboardingBg, modifier = Modifier.weight(1f))
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(OnboardingBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
    }
}

// ── Decoration primitives: edge-anchored, so nothing drifts off-screen ──────────────────────

/** Right-aligned insets pull inward from the right edge; left-aligned from the left edge.
 * Offsets are scaled proportionally to the available width so decorations stay correctly
 * placed across screen sizes (no overlap on small screens, no drift on large ones).
 */
@Composable
private fun BoxScope.Deco(
    size: Dp,
    color: Color,
    alignment: Alignment,
    corner: Dp,
    rotation: Float = 0f,
    insetX: Dp = 0.dp,
    insetY: Dp = 0.dp,
) {
    BoxWithConstraints(
        Modifier
            .align(alignment)
            .size(size)
            .graphicsLayer { rotationZ = rotation }
            .clip(RoundedCornerShape(corner))
            .background(color),
    ) {
        val fromEnd = alignment == Alignment.TopEnd || alignment == Alignment.BottomEnd
        // Reference width ~393dp (Pixel 6); scale offsets proportionally so they adapt
        val scale = constraints.maxWidth / 393f
        val x = (if (fromEnd) -insetX else insetX) * scale
        val y = insetY * scale
        Modifier.offset(x = x, y = y)
    }
}

@Composable
private fun BoxScope.DecoPill(
    width: Dp,
    height: Dp,
    color: Color,
    alignment: Alignment,
    rotation: Float = 0f,
    insetX: Dp = 0.dp,
    insetY: Dp = 0.dp,
) {
    BoxWithConstraints(
        Modifier
            .align(alignment)
            .size(width, height)
            .graphicsLayer { rotationZ = rotation }
            .clip(RoundedCornerShape(50))
            .background(color),
    ) {
        val fromEnd = alignment == Alignment.TopEnd || alignment == Alignment.BottomEnd
        val scale = constraints.maxWidth / 393f
        val x = (if (fromEnd) -insetX else insetX) * scale
        val y = insetY * scale
        Modifier.offset(x = x, y = y)
    }
}

// ── Step 1 · Welcome ────────────────────────────────────────────────────────────────────────
@Composable
internal fun Welcome(onNext: () -> Unit) {
    OnboardingPage(
        buttonContent = {
            PillButton("Get started", Icons.AutoMirrored.Filled.KeyboardArrowRight, onNext, height = 64.dp)
            Spacer(Modifier.height(8.dp))
            Text(
                "Takes less than a minute",
                style = chakraText(FontWeight.Medium, 13, 16),
                color = DotLabel,
                textAlign = TextAlign.Center,
            )
        },
    ) {
        HeroCardScaffold(height = 430.dp, navRow = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Logo: dark rounded-square container (h-10 w-10 rounded-xl bg-[#0e0e0e])
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(OnboardingBg),
                    contentAlignment = Alignment.Center,
                ) {
                    SumvaloLogo(size = 28.dp)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "SUMVALO",
                    style = chakraText(FontWeight.Bold, 15, 20, 0.18f),
                    color = Color.White,
                )
                Spacer(Modifier.weight(1f))
                SkipButton(onNext)
            }
        }, decorations = {
            // Big circle: h-260 w-260 top-10 left-150 bg-[#2a2a2a] rounded-9999px
            Box(
                Modifier
                    .offset(x = 150.dp, y = 10.dp)
                    .size(260.dp)
                    .clip(CircleShape)
                    .background(Hairline),
            )
            // Small deco: h-10 w-10 top-22 left-40 bg-[#0e0e0e] rounded-10px rotate-18deg
            Box(
                Modifier
                    .offset(x = 40.dp, y = 22.dp)
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(OnboardingBg)
                    .graphicsLayer { rotationZ = 18f },
            )
            // Tiny deco: h-5 w-5 top-78 left-110 bg-[#ffffff] rounded-9999px
            Box(
                Modifier
                    .offset(x = 110.dp, y = 78.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.White),
            )
            // ₹ gauge: h-184 w-184 top-46 left-166 bg-[#ffffff] shadow absolute
            Box(
                Modifier
                    .offset(x = 166.dp, y = 46.dp)
                    .size(184.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(148.dp)
                        .clip(CircleShape)
                        .border(6.dp, OnboardingBg, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "₹",
                        style = chakraText(FontWeight.ExtraBold, 92, 92),
                        color = OnboardingBg,
                    )
                }
            }
            // Bottom stats: bottom-28 left-24 gap-2
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Bottom,
            ) {
                Row(
                    Modifier
                        .offset(x = 24.dp, y = -28.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Box(Modifier.size(12.dp, 80.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF3A3A3A)))
                    Box(Modifier.size(12.dp, 128.dp).clip(RoundedCornerShape(14.dp)).background(OnboardingBg))
                    Box(Modifier.size(12.dp, 176.dp).clip(RoundedCornerShape(14.dp)).background(Color.White))
                }
            }
            // Growth pill: bottom-34 left-196 -rotate-4deg px-4 py-2 bg-[#0e0e0e] rounded-9999px
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Bottom,
            ) {
                Box(
                    Modifier
                        .offset(x = 196.dp, y = -34.dp)
                        .graphicsLayer { rotationZ = -4f },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .background(OnboardingBg)
                            .clip(RoundedCornerShape(9999.dp)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("▲", style = chakraText(FontWeight.Bold, 14, 18), color = Green)
                        Text("2.4% this month", style = chakraText(FontWeight.Bold, 14, 18), color = Green)
                    }
                }
            }
        })

        ProgressRow(0)
        HeadlineBlock(
            title = "Take control of your money.",
            subtitle = "Track income, expenses, budgets and goals — privately, on your phone.",
            titleSize = 44,
            titleWeight = FontWeight.ExtraBold,
            titleLineHeight = 42,
            subtitleSize = 16,
            subtitleLineHeight = 23,
        )

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BenefitCard(Icons.Outlined.Lock, "On-device", Modifier.weight(1f))
            BenefitCard(Icons.Outlined.PersonOff, "No account", Modifier.weight(1f))
            BenefitCard(Icons.Outlined.Block, "No ads", Modifier.weight(1f))
        }
    }
}

@Composable
private fun BenefitCard(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .padding(horizontal = 14.dp, vertical = 16.dp),
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Hairline),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.height(12.dp))
        Text(label, style = chakraText(FontWeight.Bold, 14, 18), color = Color.White)
    }
}

// ── Step 2 · Features ───────────────────────────────────────────────────────────────────────
@Composable
internal fun FeaturesStep(onBack: () -> Unit, onNext: () -> Unit) {
    OnboardingPage(
        buttonContent = {
            PillButton("Continue", Icons.AutoMirrored.Filled.KeyboardArrowRight, onNext, height = 64.dp)
        },
    ) {
        HeroCardScaffold(height = 320.dp, navRow = { HeroNavRow(onBack = onBack, onSkip = onNext) }, decorations = {
            // Big circle: h-230 w-230 -top-20 left-170 bg-[#2a2a2a] rounded-9999px
            Box(
                Modifier
                    .offset(x = 170.dp, y = -20.dp)
                    .size(230.dp)
                    .clip(CircleShape)
                    .background(Hairline),
            )
            // Small deco: h-9 w-9 top-12 left-34 bg-[#0e0e0e] rounded-10px rotate-18deg
            Box(
                Modifier
                    .offset(x = 34.dp, y = 12.dp)
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(OnboardingBg)
                    .graphicsLayer { rotationZ = 18f },
            )
            // Tiny deco: h-5 w-5 top-30 left-104 bg-[#ffffff] rounded-9999px
            Box(
                Modifier
                    .offset(x = 104.dp, y = 30.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.White),
            )
            // Bullseye target: h-164 w-164 top-8 left-186 bg-[#ffffff] with shadow
            Box(
                Modifier
                    .offset(x = 186.dp, y = 8.dp)
                    .size(164.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(128.dp)
                        .clip(CircleShape)
                        .background(OnboardingBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(OnboardingBg),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(Modifier.size(22.dp).clip(CircleShape).background(Color.White))
                        }
                    }
                }
            }

            // Shield-with-check badge: text-104px top-96 left-148 -rotate-6deg
            Box(
                Modifier
                    .offset(x = 148.dp, y = 96.dp)
                    .size(104.dp)
                    .graphicsLayer { rotationZ = -6f },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = OnboardingBg, modifier = Modifier.size(96.dp))
                Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
            }

            // Bar chart, bottom-left: three flex-col columns gap-1
            Column(
                Modifier
                    .offset(x = 22.dp, y = -22.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Bottom,
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Box(Modifier.size(11.dp, 24.dp).clip(RoundedCornerShape(10.dp)).background(Color.White))
                        Box(Modifier.size(11.dp, 40.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF3A3A3A)))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Box(Modifier.size(11.dp, 32.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF3A3A3A)))
                        Box(Modifier.size(11.dp, 64.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF3A3A3A)))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Box(Modifier.size(11.dp, 40.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF3A3A3A)))
                        Box(Modifier.size(11.dp, 96.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF3A3A3A)))
                    }
                }
            }
        })

        ProgressRow(1)
        HeadlineBlock(
            title = "Track, plan and stay in control.",
            subtitle = "",
            titleSize = 40,
            titleWeight = FontWeight.ExtraBold,
            titleLineHeight = 40,
            subtitleSize = 16,
            subtitleLineHeight = 23,
        )

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FeatureRow(Icons.Outlined.BarChart, Hairline, Color.White, "Track everything", "Income, expenses and card dues in one place.")
            FeatureRow(Icons.Outlined.Adjust, Color.White, OnboardingBg, "Plan with confidence", "Budgets, savings goals and safe-to-spend pace.")
            FeatureRow(Icons.Outlined.VerifiedUser, Hairline, Color.White, "Private by design", "No account, no cloud, no tracking. Your data stays on your device.")
        }
    }
}

@Composable
private fun BarColumn(topColor: Color, topHeight: Dp, bottomColor: Color, bottomHeight: Dp) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(44.dp, topHeight).clip(RoundedCornerShape(10.dp)).background(topColor))
        Box(Modifier.size(44.dp, bottomHeight).clip(RoundedCornerShape(12.dp)).background(bottomColor))
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, iconBg: Color, iconTint: Color, title: String, text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = chakraText(FontWeight.Bold, 16, 20), color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(text, style = chakraText(FontWeight.Normal, 14, 19), color = SubtleText)
        }
    }
}

// ── Step 3 · History ────────────────────────────────────────────────────────────────────────
@Composable
internal fun HistoryStep(onBack: () -> Unit, onNext: () -> Unit) {
    var selected by rememberSaveable { mutableIntStateOf(0) }

    OnboardingPage(
        buttonContent = {
            PillButton("Continue", Icons.AutoMirrored.Filled.KeyboardArrowRight, onNext, height = 64.dp)
        },
    ) {
        HeroCardScaffold(height = 256.dp, navRow = { HeroNavRow(onBack = onBack, onSkip = onNext) }, decorations = {
            // Big circle: h-190 w-190 top-34 left-196 bg-[#2a2a2a] rounded-9999px
            Box(
                Modifier
                    .offset(x = 196.dp, y = 34.dp)
                    .size(190.dp)
                    .clip(CircleShape)
                    .background(Hairline),
            )
            // Small deco: h-5 w-5 top-118 left-30 bg-[#ffffff] rounded-9999px
            Box(
                Modifier
                    .offset(x = 30.dp, y = 118.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.White),
            )
            // Deco: h-8 w-8 top-104 left-322 bg-[#0e0e0e] rounded-10px rotate-18deg
            Box(
                Modifier
                    .offset(x = 322.dp, y = 104.dp)
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(OnboardingBg)
                    .graphicsLayer { rotationZ = 18f },
            )
            // Receipt card: h-150 w-28 top-12 left-132 bg-[#ffffff] rounded-18px rotate-6deg
            Box(
                Modifier
                    .offset(x = 132.dp, y = 12.dp)
                    .size(28.dp, 150.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .graphicsLayer { rotationZ = 6f },
            ) {
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ReceiptBar(56.dp, 12.dp)
                    ReceiptBar(null, 10.dp)
                    ReceiptBar(64.dp, 10.dp)
                    ReceiptBar(null, 10.dp)
                    ReceiptBar(48.dp, 10.dp)
                }
            }
            // ₹ speech bubble: h-66 w-88 top-22 left-40 bg-[#0e0e0e] rounded-22px rotate-8deg
            Box(
                Modifier
                    .offset(x = 40.dp, y = 22.dp)
                    .size(88.dp, 66.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(OnboardingBg)
                    .graphicsLayer { rotationZ = 8f },
                contentAlignment = Alignment.Center,
            ) {
                Text("₹", style = chakraText(FontWeight.ExtraBold, 38, 40), color = Color.White)
            }
            // Small deco: h-5 w-5 top-78 left-58 bg-[#0e0e0e] rounded-4px rotate-45deg
            Box(
                Modifier
                    .offset(x = 58.dp, y = 78.dp)
                    .size(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(OnboardingBg)
                    .graphicsLayer { rotationZ = 45f },
            )
            // Repeat badge: h-78 w-78 top-66 left-232 bg-[#ffffff] shadow
            Box(
                Modifier
                    .offset(x = 232.dp, y = 66.dp)
                    .size(78.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Repeat, contentDescription = null, tint = OnboardingBg, modifier = Modifier.size(40.dp))
            }
        })

        ProgressRow(2)
        HeadlineBlock(
            title = "Your history, your way.",
            subtitle = "Track transactions, enable SMS tracking, import a bank statement or add entries manually — it's up to you.",
            titleSize = 40,
            titleWeight = FontWeight.ExtraBold,
            titleLineHeight = 40,
            subtitleSize = 15,
            subtitleLineHeight = 21,
        )

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HistoryOptionRow(Icons.Outlined.Edit, "Quick manual entry", "Tags, splits and receipts in seconds.", selected == 0) { selected = 0 }
            HistoryOptionRow(Icons.Outlined.Sms, "SMS tracking", "Auto-logs bank SMS, on-device.", selected == 1) { selected = 1 }
            HistoryOptionRow(Icons.Outlined.Description, "Bank statement import", "CSV from HDFC, ICICI, SBI and more.", selected == 2) { selected = 2 }
            HistoryOptionRow(Icons.Outlined.Repeat, "Recurring on autopilot", "Rent, SIPs and subscriptions.", selected == 3) { selected = 3 }
        }
    }
}

@Composable
private fun ReceiptBar(barWidth: Dp?, barHeight: Dp) {
    val modifier = if (barWidth == null) Modifier.fillMaxWidth() else Modifier.size(barWidth, barHeight)
    Box(modifier.clip(RoundedCornerShape(50)).background(OnboardingBg))
}

@Composable
private fun HistoryOptionRow(icon: ImageVector, title: String, text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Color.White else CardBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) OnboardingBg else TileBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = chakraText(FontWeight.Bold, 16, 20),
                color = if (selected) OnboardingBg else Color.White,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text,
                style = chakraText(if (selected) FontWeight.Medium else FontWeight.Normal, 13, 18),
                color = if (selected) OnboardingBg else SubtleText,
            )
        }
        Spacer(Modifier.width(12.dp))
        if (selected) {
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(OnboardingBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        } else {
            Box(Modifier.size(32.dp).clip(CircleShape).border(3.dp, Color(0xFF3A3A3A), CircleShape))
        }
    }
}

// ── Step 4 · Notifications (permission gate) ────────────────────────────────────────────────

@Composable
internal fun NotificationsStep(onBack: () -> Unit, onNext: () -> Unit) {
    val granted by rememberNotificationPermission()
    val request = rememberNotificationRequest()
    var requested by rememberSaveable { mutableStateOf(false) }
    val ask: () -> Unit = {
        requested = true
        request()
    }

    NotificationsScreen(
        onBack = onBack,
        onNext = onNext,
        granted = granted,
        showDeniedHelp = requested && !granted,
        onAllowClick = { if (granted) onNext() else ask() },
    )
}

/** Pure UI for the notifications gate; permission wiring lives in [NotificationsStep]. */
@Composable
internal fun NotificationsScreen(
    onBack: () -> Unit,
    onNext: () -> Unit,
    granted: Boolean,
    showDeniedHelp: Boolean,
    onAllowClick: () -> Unit,
) {
    var cardBills by rememberSaveable { mutableStateOf(true) }
    var spendingAlerts by rememberSaveable { mutableStateOf(true) }
    var planned by rememberSaveable { mutableStateOf(true) }

    OnboardingPage(
        buttonContent = {
            PillButton(
                if (granted) "Continue" else "Allow notifications",
                if (granted) Icons.AutoMirrored.Filled.KeyboardArrowRight else Icons.Outlined.NotificationsNone,
                onClick = onAllowClick,
                height = 64.dp,
            )
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(9999.dp))
                    .clickable(onClick = onNext)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Maybe later", style = chakraText(FontWeight.Bold, 15, 20), color = SubtleText)
            }
        },
    ) {
        HeroCardScaffold(height = 270.dp, navRow = { HeroNavRow(onBack = onBack, onSkip = onNext) }, decorations = {
            // Big circle: h-200 w-200 top-10 left-150 bg-[#2a2a2a] rounded-9999px
            Box(
                Modifier
                    .offset(x = 150.dp, y = 10.dp)
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(Hairline),
            )
            // Small deco: h-5 w-5 top-28 left-40 bg-[#ffffff] rounded-9999px
            Box(
                Modifier
                    .offset(x = 40.dp, y = 28.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.White),
            )
            // Deco: h-8 w-8 top-40 left-100 bg-[#0e0e0e] rounded-10px rotate-18deg
            Box(
                Modifier
                    .offset(x = 100.dp, y = 40.dp)
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(OnboardingBg)
                    .graphicsLayer { rotationZ = 18f },
            )
            // Small deco: h-5 w-5 top-30 left-322 bg-[#0e0e0e] rounded-4px rotate-45deg
            Box(
                Modifier
                    .offset(x = 322.dp, y = 30.dp)
                    .size(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(OnboardingBg)
                    .graphicsLayer { rotationZ = 45f },
            )
            // Small deco: h-5 w-5 top-0 left-226 bg-[#ffffff] rounded-9999px
            Box(
                Modifier
                    .offset(x = 226.dp, y = 0.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.White),
            )
            // Bell dome: h-104 w-28 top-12 left-180 bg-[#ffffff] rounded-56px
            Box(
                Modifier
                    .offset(x = 180.dp, y = 12.dp)
                    .size(112.dp, 104.dp)
                    .clip(RoundedCornerShape(56.dp))
                    .background(Color.White),
            )
            // Bell rim bar: h-6 w-156 top-104 left-158 bg-[#ffffff]
            Box(
                Modifier
                    .offset(x = 158.dp, y = 104.dp)
                    .size(156.dp, 24.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White),
            )
            // Bell inner bar: h-6 w-156 top-92 left-158 bg-[#0e0e0e]
            Box(
                Modifier
                    .offset(x = 158.dp, y = 92.dp)
                    .size(156.dp, 24.dp)
                    .clip(RoundedCornerShape(50))
                    .background(OnboardingBg),
            )
            // Bell clapper: h-8 w-8 top-134 left-220 rounded-9999px bg-[#0e0e0e]
            Box(
                Modifier
                    .offset(x = 220.dp, y = 134.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(OnboardingBg),
            )
            // Count badge: h-12 w-12 border-4 border-[#1f1f1f] bg-[#d9776f] top-[-4px] left-[262px]
            Box(
                Modifier
                    .offset(x = 262.dp, y = -4.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Coral)
                    .border(4.dp, HeroBg, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("3", style = chakraText(FontWeight.Bold, 20, 22), color = Color.White)
            }
            // Bill-due chip: rounded-2xl bg-[#0e0e0e] px-4 py-3 shadow top-[118px] left-[18px] -rotate-7deg
            Column(
                Modifier
                    .offset(x = 18.dp, y = 118.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Bottom,
            ) {
                Box(
                    Modifier
                        .graphicsLayer { rotationZ = -7f },
                    contentAlignment = Alignment.Center,
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
                                .background(OnboardingBg)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(Coral))
                            Text("Bill due in 3 days", style = chakraText(FontWeight.Bold, 15, 18), color = Color.White)
                        }
                    }
                }
            }
        })

        ProgressRow(3)
        HeadlineBlock(
            title = "Turn on notifications.",
            subtitle = "Timely reminders for bills, unusual spending, warranties and scheduled payments.",
            titleSize = 40,
            titleWeight = FontWeight.ExtraBold,
            titleLineHeight = 40,
            subtitleSize = 15,
            subtitleLineHeight = 21,
        )

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NotifRow(Icons.Outlined.CalendarToday, "Card bills", "A reminder before each due date.", cardBills) { cardBills = !cardBills }
            NotifRow(Icons.Outlined.NotificationsNone, "Spending alerts", "Unusual spending or a budget running hot.", spendingAlerts) { spendingAlerts = !spendingAlerts }
            NotifRow(Icons.Outlined.EventAvailable, "Things you planned", "Subscriptions and scheduled payments.", planned) { planned = !planned }
        }

        if (showDeniedHelp) {
            NotificationDeniedHelp(Modifier.fillMaxWidth())
        }
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
                .background(if (on) OnboardingBg else Color(0xFF8A8A8A)),
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
            .background(CardBg)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(TileBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = chakraText(FontWeight.Bold, 16, 20), color = Color.White)
            Spacer(Modifier.height(2.dp))
            Text(text, style = chakraText(FontWeight.Normal, 13, 18), color = SubtleText)
        }
        Spacer(Modifier.width(12.dp))
        NotifToggle(on, onToggle)
    }
}

// ── Step 5 · All set ────────────────────────────────────────────────────────────────────────
@Composable
internal fun DoneStep(onBack: () -> Unit, onFinish: () -> Unit) {
    OnboardingPage(
        buttonContent = {
            PillButton("Open Sumvalo", Icons.AutoMirrored.Filled.KeyboardArrowRight, onFinish, height = 64.dp)
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(9999.dp))
                    .clickable(onClick = onFinish)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Add an account first", style = chakraText(FontWeight.Bold, 15, 20), color = SubtleText)
            }
        },
    ) {
        HeroCardScaffold(height = 380.dp, navRow = { HeroNavRow(onBack = null, onSkip = null) }, decorations = {
            // Big white check disc: h-184 w-184 top-30 left-91 bg-[#ffffff] shadow
            Box(
                Modifier
                    .offset(x = 91.dp, y = 30.dp)
                    .size(184.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null, tint = OnboardingBg, modifier = Modifier.size(112.dp))
            }

            // Confetti decorations
            // Small deco: h-7 w-7 top-30 left-36 bg-[#0e0e0e] rounded-8px rotate-18deg
            Box(
                Modifier
                    .offset(x = 36.dp, y = 30.dp)
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(OnboardingBg)
                    .graphicsLayer { rotationZ = 18f },
            )
            // Tiny deco: h-5 w-5 top-110 left-22 bg-[#ffffff] rounded-9999px
            Box(
                Modifier
                    .offset(x = 22.dp, y = 110.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.White),
            )
            // Green pill: h-4 w-12 top-190 left-30 bg-[#6fbf8b] rounded-9999px rotate-28deg
            Box(
                Modifier
                    .offset(x = 30.dp, y = 190.dp)
                    .size(12.dp, 16.dp)
                    .clip(CircleShape)
                    .background(Green)
                    .graphicsLayer { rotationZ = -28f },
            )
            // Small deco: h-5 w-5 top-240 left-90 bg-[#0e0e0e] rounded-4px rotate-40deg
            Box(
                Modifier
                    .offset(x = 90.dp, y = 240.dp)
                    .size(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(OnboardingBg)
                    .graphicsLayer { rotationZ = 40f },
            )
            // ChangeHistory icon: text-34px text-[#f5f5f5] top-64 left-68 -rotate-14deg
            Box(
                Modifier
                    .offset(x = 68.dp, y = 64.dp)
                    .graphicsLayer { rotationZ = -14f },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.ChangeHistory, contentDescription = null, tint = Color(0xFFF5F5F5), modifier = Modifier.size(34.dp))
            }
            // Tiny deco: h-3 w-3 top-160 left-80 bg-[#ffffff] rounded-9999px
            Box(
                Modifier
                    .offset(x = 80.dp, y = 160.dp)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color.White),
            )
            // Small deco: h-8 w-8 top-22 left-296 bg-[#0e0e0e] rounded-9px rotate-16deg
            Box(
                Modifier
                    .offset(x = 296.dp, y = 22.dp)
                    .size(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(OnboardingBg)
                    .graphicsLayer { rotationZ = -16f },
            )
            // Tiny deco: h-6 w-6 top-96 left-316 bg-[#ffffff] rounded-9999px
            Box(
                Modifier
                    .offset(x = 316.dp, y = 96.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White),
            )
            // Green pill: h-4 w-14 top-64 left-256 bg-[#6fbf8b] rounded-9999px rotate-32deg
            Box(
                Modifier
                    .offset(x = 256.dp, y = 64.dp)
                    .size(14.dp, 16.dp)
                    .clip(CircleShape)
                    .background(Green)
                    .graphicsLayer { rotationZ = 32f },
            )
            // ChangeHistory icon: text-40px text-[#6fbf8b] top-170 left-300 rotate-22deg
            Box(
                Modifier
                    .offset(x = 300.dp, y = 170.dp)
                    .graphicsLayer { rotationZ = 22f },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.ChangeHistory, contentDescription = null, tint = Green, modifier = Modifier.size(40.dp))
            }
            // Small deco: h-5 w-5 top-246 left-262 bg-[#0e0e0e] rounded-5px rotate-24deg
            Box(
                Modifier
                    .offset(x = 262.dp, y = 246.dp)
                    .size(20.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(OnboardingBg)
                    .graphicsLayer { rotationZ = 24f },
            )
            // Tiny deco: h-3.5 w-3.5 top-0 left-180 bg-[#ffffff] rounded-9999px
            Box(
                Modifier
                    .offset(x = 180.dp, y = 0.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color.White),
            )
            // Green pill: h-3 w-10 top-256 left-160 bg-[#6fbf8b] rounded-9999px rotate-8deg
            Box(
                Modifier
                    .offset(x = 160.dp, y = 256.dp)
                    .size(10.dp, 12.dp)
                    .clip(CircleShape)
                    .background(Green)
                    .graphicsLayer { rotationZ = -8f },
            )
            // Tiny deco: h-4 w-4 top-224 left-318 bg-[#ffffff] rounded-9999px
            Box(
                Modifier
                    .offset(x = 318.dp, y = 224.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color.White),
            )
        })

        ProgressRow(4)
        HeadlineBlock(
            title = "You're all set.",
            subtitle = "Add bank accounts and cards anytime from Accounts, or jump straight in and add transactions as you go.",
            titleSize = 40,
            titleWeight = FontWeight.ExtraBold,
            titleLineHeight = 40,
            subtitleSize = 15,
            subtitleLineHeight = 21,
        )

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatCard("Tracking", "Manual", Modifier.weight(1f))
            StatCard("Alerts", "On", Modifier.weight(1f))
            StatCard("Data", "On-device", Modifier.weight(1f))
        }
    }
}

/** Green-check stat card: icon circle on top, small caps label, bold value. */
@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .padding(horizontal = 14.dp, vertical = 16.dp),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Green),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(label, style = chakraText(FontWeight.Bold, 12, 16, 0.48f), color = SubtleText)
        Text(value, style = chakraText(FontWeight.Bold, 16, 20), color = Color.White)
    }
}
