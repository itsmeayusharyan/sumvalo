package com.acuranza.app.designsystem.theme

import android.provider.Settings
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource

/**
 * Sumvalo's motion system: one set of durations, easings and entrance patterns that every
 * surface shares, plus the reduced-motion gate.
 *
 * Android exposes the user's preference through the global animator duration scale: the
 * accessibility "Remove animations" switch and the developer animation scales all write it, and
 * 0 means "no animation". Compose already multiplies its tween/spring durations by that scale, so
 * every stock animation collapses to instant on its own; [LocalReducedMotion] exists for the
 * things duration scaling can't decide — staggered entrances, parallax and other *staged*
 * effects, which should be skipped outright rather than played with a zero duration.
 */
object Motion {
    /** Micro press-feedback (keypad keys): must read as instant. */
    const val INSTANT = 120

    /** High-frequency feedback: presses, ripples, small state swaps. */
    const val FAST = 150

    /** Standard UI transitions: entrances, colour and content cross-fades. */
    const val STANDARD = 220

    /** Emphasised, infrequent moves: full-screen pushes, sheets. */
    const val EMPHASIZED = 300

    /** Per-item delay for staged entrances; kept small and capped so late rows don't wait. */
    const val STAGGER_MS = 40L

    /** Fade-in-and-rise entrance, our one authored moment per surface. */
    const val ENTRANCE = 260

    /** Drawer/sheet entrance: slightly longer so the backdrop is noticed first. */
    const val SHEET = 340

    /** Shared-element / hero transitions. */
    const val SHARED = 400
}

/** True when the user asked for reduced motion (system animation scale 0 or "Remove animations"). */
val LocalReducedMotion = compositionLocalOf { false }

/** Re-reads the system animation scale; 0 (or the string missing/malformed) counts as reduced. */
fun isReduceMotionEnabled(resolver: android.content.ContentResolver): Boolean =
    runCatching { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
        .getOrDefault(false)

/** Re-reads the preference whenever the app resumes, since it can only change in the background. */
@Composable
fun rememberReducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    var reduced by remember { mutableStateOf(isReduceMotionEnabled(resolver)) }
    LaunchedEffect(resolver) {
        reduced = isReduceMotionEnabled(resolver)
    }
    return reduced
}

/**
 * A spring spec tuned for shared-element and hero transitions: quick to settle with just enough
 * bounce to feel alive but never distracting.
 */
fun springForShared(): FiniteAnimationSpec<Float> =
    spring(
        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
    )

/**
 * A spring spec tuned for card/panel entrances: gentle settle without overshoot.
 */
fun springForCard(): FiniteAnimationSpec<Float> =
    spring(
        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
        stiffness = androidx.compose.animation.core.Spring.StiffnessLow,
    )

/**
 * A spring spec tuned for press feedback: fast settle with zero bounce — press interactions
 * must feel instant and tactile, never bouncy.
 */
fun pressSpring(): FiniteAnimationSpec<Float> =
    spring(
        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,
    )

/**
 * A tween that collapses to an instant snap under reduced motion.
 */
@Composable
fun <T> motionTween(durationMillis: Int = Motion.STANDARD, delayMillis: Int = 0): FiniteAnimationSpec<T> =
    if (LocalReducedMotion.current) snap() else tween(durationMillis, delayMillis, easing = EaseOut)

/**
 * The app's standard entrance: content rises 10dp and fades in over [Motion.ENTRANCE]ms. Rows in a
 * list pass their [index] so entrances stagger by [Motion.STAGGER_MS] up to a cap — after nine
 * steps everything arrives together, so long lists don't crawl in. Under reduced motion the delay
 * and the animation disappear entirely: content is composed at full alpha.
 */
@Composable
fun StaggeredEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val reduced = LocalReducedMotion.current
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!reduced) delay(minOf(index, 8) * Motion.STAGGER_MS)
        shown = true
    }
    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = if (reduced) snap() else tween(Motion.ENTRANCE, easing = EaseOut),
        label = "entrance-alpha",
    )
    val rise by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (shown) 0f else 10.dp.value,
        animationSpec = if (reduced) snap() else tween(Motion.ENTRANCE, easing = EaseOut),
        label = "entrance-rise",
    )
    Box(modifier.graphicsLayer { this.alpha = alpha; translationY = rise.dp.toPx() }) { content() }
}

/**
 * Shared-element transition wrapper: animates scale and opacity between states using the shared
 * element spring spec. Intended for hero ↔ detail transitions where the same visual element
 * changes size/position.
 */
@Composable
fun SharedElement(
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    alpha: Float = 1f,
    content: @Composable () -> Unit,
) {
    val reduced = LocalReducedMotion.current
    Box(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
    ) { content() }
}

/**
 * Staggered list entrance: each item fades and rises, staggered by [Motion.STAGGER_MS].
 * Use as `Modifier.staggeredEntrance(index)` on items of a LazyColumn.
 */
@Composable
fun Modifier.staggeredEntrance(index: Int, maxDelayItems: Int = 10): Modifier {
    if (androidx.compose.ui.platform.LocalInspectionMode.current) return this
    if (LocalReducedMotion.current) return this
    val anim = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(minOf(index, maxDelayItems) * Motion.STAGGER_MS)
        anim.animateTo(1f, tween(Motion.ENTRANCE, easing = EaseOut))
    }
    return this.graphicsLayer {
        alpha = anim.value.coerceIn(0f, 1f)
        translationY = (1f - anim.value) * 10.dp.toPx()
    }
}

/**
 * A quick fade-in for whole sections.
 */
@Composable
fun Modifier.fadeIn(delayMillis: Int = 0): Modifier {
    if (androidx.compose.ui.platform.LocalInspectionMode.current) return this
    if (LocalReducedMotion.current) return this
    val anim = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) { anim.animateTo(1f, tween(Motion.ENTRANCE, delayMillis, easing = EaseOut)) }
    return this.graphicsLayer { alpha = anim.value }
}

/**
 * Pressed feedback: scales to [scaleDown] on press, settles back with a hint of life rather than
 * a flat line. Under reduced motion it snaps — instant feedback, no movement.
 */
@Composable
fun Modifier.pressSpring(interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource, scaleDown: Float = 0.96f): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) scaleDown else 1f,
        animationSpec = if (LocalReducedMotion.current) snap() else springForCard(),
        label = "pressSpring",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
