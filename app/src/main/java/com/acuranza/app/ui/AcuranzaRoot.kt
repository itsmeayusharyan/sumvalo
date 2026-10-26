package com.acuranza.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.acuranza.app.data.prefs.Settings
import com.acuranza.app.data.prefs.ThemeMode
import com.acuranza.app.di.AppContainer
import com.acuranza.app.designsystem.components.SumvaloLogo
import com.acuranza.app.designsystem.components.glassSurface
import com.acuranza.app.designsystem.components.LogoShape
import com.acuranza.app.designsystem.components.PrimaryButton
import com.acuranza.app.designsystem.components.pressScale
import com.acuranza.app.designsystem.theme.BrandPalette
import com.acuranza.app.designsystem.theme.Motion
import com.acuranza.app.designsystem.theme.motionTween
import com.acuranza.app.designsystem.theme.SumvaloTheme as SumvaloThemeObject
import com.acuranza.app.designsystem.theme.rememberReducedMotion
import com.acuranza.app.model.TxnType
import com.acuranza.app.notify.Notifications
import com.acuranza.app.security.BiometricGate
import com.acuranza.app.security.UnlockResult
import com.acuranza.app.ui.accounts.AccountDetailScreen
import com.acuranza.app.ui.accounts.AccountEditScreen
import com.acuranza.app.ui.accounts.AccountsScreen
import com.acuranza.app.ui.bills.BillsScreen
import com.acuranza.app.ui.common.LocalContainer
import com.acuranza.app.ui.common.LocalHideAmounts
import com.acuranza.app.ui.common.LocalSettings
import com.acuranza.app.ui.home.HomeScreen
import com.acuranza.app.ui.insights.InsightsScreen
import com.acuranza.app.ui.nav.Routes
import com.acuranza.app.ui.nav.TabRoutes
import com.acuranza.app.ui.nav.Tabs
import com.acuranza.app.ui.onboarding.OnboardingScreen
import com.acuranza.app.ui.planning.PlanningScreen
import com.acuranza.app.ui.settings.CategoriesScreen
import com.acuranza.app.ui.settings.MoreScreen
import com.acuranza.app.ui.transactions.AddTransactionScreen
import com.acuranza.app.ui.transactions.ReviewScreen
import com.acuranza.app.ui.transactions.TransactionDetailScreen
import com.acuranza.app.ui.transactions.TransactionsScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.acuranza.app.ui.nav.navigateToTab
import androidx.compose.ui.text.style.TextOverflow

/** Root of the UI: theme, splash, app lock, onboarding and navigation. */
@Composable
fun SumvaloRoot(container: AppContainer, initialRoute: String?, onRouteConsumed: () -> Unit) {
    val settingsState by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
    val settings = settingsState
    // While prefs are still loading (first frames), follow the system instead of defaulting dark:
    // a light-pinned user otherwise sees a dark splash flicker on every cold start. The splash
    // covers the DataStore read; once it lands, the pinned theme applies.
    val dark = when (settings?.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        else -> isSystemInDarkTheme()   // SYSTEM, or prefs not loaded yet
    }

    val activity = LocalContext.current as? androidx.activity.ComponentActivity
    DisposableEffect(activity, dark) {
        activity?.enableEdgeToEdge(
            statusBarStyle = if (dark) {
                androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
            },
            navigationBarStyle = if (dark) {
                androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
            },
        )
        onDispose {}
    }

    // The keypad's haptic, built from the live settings state: it vibrates through the same
    // strength implementation as every other interaction, and updates when preferences change.
    val context = androidx.compose.ui.platform.LocalContext.current
    val keypadHaptic = remember(context, settings) {
        settings?.let {
            com.acuranza.app.ui.common.AppHaptics.of(
                context,
                it.hapticsEnabled,
                it.hapticKeypad,
                it.hapticActions,
                it.hapticTabsAndButtons,
                com.acuranza.app.ui.common.HapticIntensity.of(it.hapticIntensity),
            )
        }
    }

    com.acuranza.app.designsystem.theme.SumvaloTheme(
        darkTheme = dark,
        accent = settings?.accentChoice ?: com.acuranza.app.model.AccentColor.BLUE,
        colorTheme = settings?.colorTheme ?: com.acuranza.app.designsystem.theme.ColorTheme.DEFAULT,
        amoled = settings?.amoled ?: false,
    ) {
        CompositionLocalProvider(
            LocalContainer provides container,
            LocalSettings provides (settings ?: Settings()),
            LocalHideAmounts provides (settings?.hideAmounts ?: false),
            // The one gate for staged motion: staggered entrances skip entirely, stock tweens and
            // springs already collapse via the system's animation-duration scale.
            com.acuranza.app.designsystem.theme.LocalReducedMotion provides rememberReducedMotion(),
            com.acuranza.app.designsystem.theme.LocalHapticsEnabled provides (settings?.hapticsEnabled ?: true),
            com.acuranza.app.designsystem.theme.LocalKeypadHapticsEnabled provides ((settings?.hapticsEnabled ?: true) && (settings?.hapticKeypad ?: true)),
            com.acuranza.app.designsystem.theme.LocalKeypadHapticTick provides ({ keypadHaptic?.keypadTick() }),
            com.acuranza.app.designsystem.theme.LocalHapticTick provides ({ keypadHaptic?.tick() }),
        ) {
            var splashDone by remember { mutableStateOf(AppLock.splashShown) }
            LaunchedEffect(Unit) {
                if (!AppLock.splashShown) {
                    delay(700)
                    AppLock.splashShown = true
                }
                splashDone = true
            }
            val lockEnabled = settings?.biometricLockEnabled == true
            val unlocked by AppLock.unlocked.collectAsStateWithLifecycle()

            val owner = LocalLifecycleOwner.current
            DisposableEffect(owner, lockEnabled) {
                val obs = LifecycleEventObserver { _, e ->
                    when (e) {
                        Lifecycle.Event.ON_STOP -> AppLock.onBackground()
                        Lifecycle.Event.ON_START -> AppLock.onForeground(lockEnabled)
                        else -> Unit
                    }
                }
                owner.lifecycle.addObserver(obs)
                onDispose { owner.lifecycle.removeObserver(obs) }
            }

            // Sumvalo requires notification permission before the main app: reminders are a core feature,
            // so a missing permission keeps the user on the gate instead of inside the app.
            var notifGranted by com.acuranza.app.ui.onboarding.rememberNotificationPermission()
            val stage = when {
                settings == null || !splashDone -> Stage.Splash
                !settings.onboardingDone -> Stage.Onboarding
                !notifGranted -> Stage.Gate
                else -> Stage.Main
            }
            val locked = lockEnabled && !unlocked && stage != Stage.Splash

            // Theme switches ease the backdrop over 220ms instead of hard-flashing the whole
            // screen; the content tree is never cross-faded because that would recreate the
            // NavHost and lose navigation and scroll state.
            val backdrop by androidx.compose.animation.animateColorAsState(
                targetValue = MaterialTheme.colorScheme.background,
                animationSpec = com.acuranza.app.designsystem.theme.motionTween(
                    com.acuranza.app.designsystem.theme.Motion.STANDARD,
                ),
                label = "theme-backdrop",
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(backdrop)
                    .windowInsetsPadding(WindowInsets.statusBars),
            ) {
                // The lock is drawn over the app instead of replacing it, so re-locking after a minute away keeps
                // the back stack and any half-filled form. While locked the app underneath is invisible to the eye
                // and to accessibility services.
                Box(
                    Modifier
                        .fillMaxSize()
                        .then(if (locked) Modifier.graphicsLayer { alpha = 0f }.clearAndSetSemantics { } else Modifier),
                ) {
                    val stageFade = com.acuranza.app.designsystem.theme.motionTween<Float>(Motion.STANDARD)
                    val stageFadeOut = com.acuranza.app.designsystem.theme.motionTween<Float>(Motion.FAST)
                    AnimatedContent(
                        targetState = stage,
                        transitionSpec = {
                            fadeIn(stageFade) togetherWith fadeOut(stageFadeOut)
                        },
                        label = "stage",
                    ) { s ->
                        when (s) {
                            Stage.Splash -> Splash()
                            Stage.Onboarding -> OnboardingScreen()
                            Stage.Gate -> com.acuranza.app.ui.onboarding.NotificationGateScreen()
                            Stage.Main -> MainShell(initialRoute, onRouteConsumed)
                        }
                    }
                }
                if (locked) LockScreen(onUnlocked = { AppLock.markUnlocked() })
            }
        }
    }
}

private enum class Stage { Splash, Onboarding, Gate, Main }

@Composable
private fun Splash() {
    val splashSpec = motionTween<Float>(Motion.EMPHASIZED)
    var start by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { start = true }
    val alpha by animateFloatAsState(if (start) 1f else 0f, splashSpec, label = "splash")
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer { this.alpha = alpha },
        ) {
            SumvaloLogo(size = 88.dp, shape = LogoShape.RoundedSquare)
            Spacer(Modifier.height(12.dp))
            Text("Sumvalo", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = SumvaloThemeObject.colors.ink)
        }
    }
}

@Composable
private fun LockScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    var available by remember { mutableStateOf(BiometricGate.isAvailable(context)) }
    // App PIN / decoy PIN (see ui.tools.PinLockPad); biometrics always open the real profile.
    val toolsPrefs by remember { com.acuranza.app.tools.ToolsSettings.get(context).prefs }.collectAsStateWithLifecycle(initialValue = null)
    val realTheme = com.acuranza.app.ui.common.LocalSettings.current.themeMode
    val unlock: () -> Unit = unlock@{
        val activity = context as? FragmentActivity ?: return@unlock
        available = BiometricGate.isAvailable(context)
        if (!available) {
            // Fail closed: with no screen lock on the phone there is nothing to verify the user with.
            message = "Set a screen lock (PIN, pattern or fingerprint) on your phone to unlock Sumvalo."
            return@unlock
        }
        scope.launch {
            when (val r = BiometricGate.authenticate(activity)) {
                UnlockResult.Success -> {
                    com.acuranza.app.security.DecoySession.deactivate()
                    onUnlocked()
                }
                is UnlockResult.Error -> message = r.message
            }
        }
    }
    // With an app PIN set the PIN pad shows first, so a decoy PIN can be entered without a prompt in the way.
    LaunchedEffect(toolsPrefs?.pinSet) { if (toolsPrefs?.pinSet == false) unlock() }
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SumvaloLogo(size = 72.dp, shape = LogoShape.RoundedSquare)
            Text("Sumvalo is locked", style = MaterialTheme.typography.titleLarge, color = SumvaloThemeObject.colors.ink)
            message?.let { Text(it, color = SumvaloThemeObject.colors.subtleText, style = MaterialTheme.typography.bodyMedium) }
            Spacer(Modifier.height(8.dp))
            val pinPrefs = toolsPrefs
            if (pinPrefs?.pinSet == true) {
                com.acuranza.app.ui.tools.PinLockPad(
                    prefs = pinPrefs,
                    onMatch = { match ->
                        scope.launch {
                            if (match == com.acuranza.app.security.PinMatch.DECOY) {
                                com.acuranza.app.security.DecoySession.activate(context, realTheme)
                            } else {
                                com.acuranza.app.security.DecoySession.deactivate()
                            }
                            onUnlocked()
                        }
                    },
                    onBiometric = if (available) unlock else null,
                )
            } else {
                PrimaryButton(text = "Unlock", onClick = unlock, icon = Icons.Outlined.Fingerprint, modifier = Modifier.padding(horizontal = 48.dp).fillMaxWidth())
            }
            if (!available && pinPrefs?.pinSet != true) {
                androidx.compose.material3.TextButton(onClick = {
                    runCatching {
                        context.startActivity(
                            android.content.Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                }) { Text("Open security settings") }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MainShell(initialRoute: String?, onRouteConsumed: () -> Unit) {
    val fabEnter = motionTween<Float>(Motion.FAST)
    val fabExit = motionTween<Float>(Motion.INSTANT)
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val landingTabPref = com.acuranza.app.ui.common.LocalSettings.current.landingTab
    // The bar tucks away while the keyboard is up so text fields keep the space.
    val imeVisible = WindowInsets.isImeVisible
    // Track the active tab independently of the current route so the bottom bar
    // stays visible (and the right tab stays highlighted) when the user drills
    // into sub-screens such as Account Detail or Transaction Detail.
    var activeTab by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(current) {
        if (current != null && current in TabRoutes) {
            activeTab = current
        }
    }
    val showBar = activeTab != null && !imeVisible

    LaunchedEffect(initialRoute) {
        val target = when (initialRoute) {
            Notifications.ROUTE_REVIEW -> Routes.REVIEW
            Notifications.ROUTE_SMS_TRACKING -> Routes.SMS_TRACKING
            Notifications.ROUTE_BILLS -> Routes.BILLS
            Notifications.ROUTE_INSIGHTS -> Routes.INSIGHTS
            Notifications.ROUTE_RECURRING -> Routes.RECURRING
            com.acuranza.app.tools.QuickLaunch.ROUTE_ADD_EXPENSE -> Routes.addTxn(TxnType.EXPENSE)
            com.acuranza.app.tools.QuickLaunch.ROUTE_ADD_INCOME -> Routes.addTxn(TxnType.INCOME)
            com.acuranza.app.tools.QuickLaunch.ROUTE_TRANSFER -> Routes.addTxn(TxnType.TRANSFER)
            com.acuranza.app.tools.QuickLaunch.ROUTE_TRANSACTIONS -> Routes.TRANSACTIONS
            com.acuranza.app.tools.QuickLaunch.ROUTE_BILLS -> Routes.BILLS
            com.acuranza.app.tools.QuickLaunch.ROUTE_ACCOUNTS -> Routes.ACCOUNTS
            com.acuranza.app.tools.QuickLaunch.ROUTE_PLANNING -> Routes.PLANNING
            com.acuranza.app.tools.QuickLaunch.ROUTE_REPORTS -> Routes.REPORTS
            com.acuranza.app.tools.QuickLaunch.ROUTE_SAVINGS_GOAL -> Routes.SAVINGS_GOAL
            else -> null
        }
        if (target != null) {
            if (target in TabRoutes) nav.navigateToTab(target) else nav.navigate(target) { launchSingleTop = true }
            onRouteConsumed()
        } else {
            // Plain launch: honour the saved landing tab, but only from the start destination —
            // never after process/activity recreation (nav state is already restored) and never
            // over an explicit route (widgets, notifications, deep links — handled above).
            val saved = com.acuranza.app.ui.nav.TabRoutes.firstOrNull { it == landingTabPref }
            if (saved != null && saved != Routes.HOME && nav.currentDestination?.route == Routes.HOME) {
                nav.navigateToTab(saved)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnimatedVisibility(
                visible = showBar,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                BottomBar(nav, activeTab)
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                activeTab == Routes.HOME || activeTab == Routes.TRANSACTIONS,
                enter = fadeIn(fabEnter),
                exit = fadeOut(fabExit),
            ) {
                AddFab { nav.navigate(Routes.addTxn()) }
            }
        },
        floatingActionButtonPosition = androidx.compose.material3.FabPosition.End,
    ) { padding ->
        // When the bar is visible, reserve space for it above the gesture area.
        // padding.calculateBottomPadding() returns 0 because contentWindowInsets is zeros,
        // so we add the bar height plus a small gap manually.
        val bottomExtra = if (showBar) 60.dp + 12.dp else 0.dp
        Box(
            Modifier.padding(bottom = padding.calculateBottomPadding() + bottomExtra)
        ) {
            SumvaloNavHost(nav)
        }
    }
}

@Composable
private fun BottomBar(nav: NavHostController, activeTab: String?) {
    val s = com.acuranza.app.ui.common.LocalSettings.current
    val tabs = com.acuranza.app.ui.nav.NavTabs.resolve(
        com.acuranza.app.ui.nav.NavTabs.decodeLayout(s.navTabLayout),
        com.acuranza.app.ui.nav.NavTabs.decodeLabels(s.navTabLabels),
        com.acuranza.app.ui.nav.NavTabs.decodeIcons(s.navTabIcons),
    )
    SumvaloBottomBar(activeTab, onTab = { route -> nav.navigateToTab(route) }, tabs = tabs, showLabels = s.navShowLabels, floating = s.navFloatingBar)
}

/**
 * Bottom navigation bar: a floating pill-shaped surface that hovers above the gesture area
 * with side margins, a hairline border and a restrained accent wash behind the selected tab.
 * No glass, no shadow, no gradient — solid surface derived from theme tokens only.
 *
 * The [floating] parameter is accepted for API compatibility (the settings editor persists it);
 * the bar is always rendered as a floating pill.
 */
@Composable
fun SumvaloBottomBar(
    current: String?,
    onTab: (String) -> Unit,
    tabs: List<com.acuranza.app.ui.nav.NavTabs.ResolvedTab> =
        com.acuranza.app.ui.nav.NavTabs.resolve(com.acuranza.app.ui.nav.NavTabs.decodeLayout(null), emptyMap(), emptyMap()),
    showLabels: Boolean = true,
    floating: Boolean = true,
) {
    val colors = SumvaloThemeObject.colors
    val haptic = com.acuranza.app.ui.common.rememberHaptic()
    val barShape = RoundedCornerShape(28.dp)
    val pillShape = RoundedCornerShape(20.dp)

    Box(
        Modifier
            .fillMaxWidth()
            // Side margins make the bar a floating pill instead of a docked edge-to-edge strip.
            .padding(horizontal = 16.dp)
            // System gesture padding keeps it above gesture navigation on all devices.
            .navigationBarsPadding()
            // Display cutout (notch) support.
            .windowInsetsPadding(WindowInsets.displayCutout)
            .background(colors.cardSurface, barShape)
            .border(1.dp, colors.hairline, barShape)
            .padding(vertical = 7.dp)
            .height(60.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.filter { it.visible }.forEach { tab ->
                val resolvedIcon = com.acuranza.app.ui.nav.NavTabs.iconOf(tab.route, tab.iconId)
                val selected = current == tab.route
                val reduced = com.acuranza.app.designsystem.theme.LocalReducedMotion.current
                val pill by animateFloatAsState(
                    targetValue = if (selected) 1f else 0f,
                    animationSpec = if (reduced) androidx.compose.animation.core.snap()
                    else spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
                    label = "tab-pill",
                )
                val iconTint by animateColorAsState(
                    targetValue = if (selected) colors.accent else colors.tertiaryText,
                    animationSpec = com.acuranza.app.designsystem.theme.motionTween(220),
                    label = "tab-icon",
                )
                val labelColor by animateColorAsState(
                    targetValue = if (selected) colors.ink else colors.tertiaryText,
                    animationSpec = com.acuranza.app.designsystem.theme.motionTween(220),
                    label = "tab-label",
                )
                val interaction = remember { MutableInteractionSource() }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(vertical = 4.dp)
                        .pressScale(interaction, scaleDown = 0.97f)
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            role = androidx.compose.ui.semantics.Role.Tab,
                        ) {
                            if (!selected) {
                                haptic.tick()
                                onTab(tab.route)
                            }
                        }
                        .semantics {
                            this.selected = selected
                            contentDescription = tab.label
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    // Selected-state pill: a compact accent wash behind icon + label, sized
                    // smaller than the tab slot so it reads as a true pill behind the content.
                    Box(
                        Modifier
                            .width(56.dp)
                            .height(40.dp)
                            .align(Alignment.Center)
                            .graphicsLayer {
                                alpha = pill
                                scaleX = 0.85f + 0.15f * pill
                                scaleY = 0.85f + 0.15f * pill
                            }
                            .clip(pillShape)
                            .background(colors.accentTint),
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            resolvedIcon.icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(22.dp),
                        )
                        if (showLabels) {
                            Text(
                                tab.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                fontSize = 10.sp,
                                color = labelColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 3.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** The corner add button, shown above the bar on Home and Transactions. */
@Composable
private fun AddFab(onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .size(56.dp)
            .clip(shape)
            .background(SumvaloThemeObject.colors.ink)
            .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
            .semantics { contentDescription = "Add transaction" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Outlined.Add, contentDescription = null, tint = SumvaloThemeObject.colors.onInk, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun SumvaloNavHost(nav: NavHostController) {
    val idArg = listOf(navArgument("id") { type = NavType.LongType })
    // Captured here because NavHost's transition lambdas are not composable scopes.
    val navFadeIn = motionTween<Float>(Motion.STANDARD)
    val navFadeOut = motionTween<Float>(Motion.FAST)
    NavHost(
        navController = nav,
        startDestination = Routes.HOME,
        // Expressive pushes: position follows a fast, barely-bouncy spring; opacity fades in
        // parallel. Pops mirror it, slightly quicker. The system animation scale (and the
        // reduced-motion gate via Motion's specs) still collapses these to instant when asked.
        enterTransition = {
            if (targetState.destination.route in TabRoutes) fadeIn(navFadeIn)
            else slideInHorizontally(
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = 0.9f,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
                ),
            ) { it / 4 } + fadeIn(navFadeIn)
        },
        exitTransition = { fadeOut(navFadeOut) },
        popEnterTransition = { fadeIn(navFadeIn) },
        popExitTransition = {
            if (initialState.destination.route in TabRoutes) fadeOut(navFadeOut)
            else slideOutHorizontally(
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = 0.9f,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
                ),
            ) { it / 4 } + fadeOut(navFadeIn)
        },
    ) {
        composable(Routes.HOME) { HomeScreen(nav) }
        composable(Routes.TRANSACTIONS) { TransactionsScreen(nav) }
        composable(Routes.PLANNING) { PlanningScreen(nav) }
        composable(Routes.SETTINGS) { MoreScreen(nav) }

        composable(Routes.REVIEW) { ReviewScreen(nav) }
        composable(
            Routes.ADD_TXN,
            arguments = listOf(
                navArgument("type") { type = NavType.StringType; defaultValue = TxnType.EXPENSE.name },
                navArgument("account") { type = NavType.LongType; defaultValue = -1L },
            ),
        ) { entry ->
            val type = runCatching { TxnType.valueOf(entry.arguments?.getString("type") ?: "") }.getOrDefault(TxnType.EXPENSE)
            val account = entry.arguments?.getLong("account")?.takeIf { it > 0 }
            AddTransactionScreen(nav, editId = null, initialType = type, initialAccountId = account)
        }
        composable(Routes.EDIT_TXN, arguments = idArg) { entry ->
            AddTransactionScreen(nav, editId = entry.arguments?.getLong("id"), initialType = TxnType.EXPENSE, initialAccountId = null)
        }
        composable(Routes.TXN_DETAIL, arguments = idArg) { entry -> TransactionDetailScreen(nav, entry.arguments!!.getLong("id")) }
        composable(Routes.ACCOUNTS) { AccountsScreen(nav) }
        composable(Routes.ACCOUNT_DETAIL, arguments = idArg) { entry -> AccountDetailScreen(nav, entry.arguments!!.getLong("id")) }
        composable(
            Routes.ACCOUNT_EDIT,
            arguments = listOf(
                navArgument("id") { type = NavType.LongType; defaultValue = -1L },
                navArgument("card") { type = NavType.BoolType; defaultValue = false },
            ),
        ) { entry ->
            AccountEditScreen(
                nav,
                editId = entry.arguments?.getLong("id")?.takeIf { it > 0 },
                defaultCard = entry.arguments?.getBoolean("card") ?: false,
            )
        }
        composable(Routes.BILLS) { BillsScreen(nav) }
        composable(Routes.NOTIFICATIONS) { com.acuranza.app.ui.notifications.NotificationsScreen(nav) }
        composable(Routes.EDIT_HOME) { com.acuranza.app.ui.home.EditHomeScreen(nav) }
        composable(Routes.SETTINGS_MANAGEMENT) { com.acuranza.app.ui.settings.ManagementSettingsPage(nav) }
        composable(Routes.SETTINGS_FINANCIAL) { com.acuranza.app.ui.settings.FinancialToolsPage(nav) }
        composable(Routes.SMS_TRACKING) { com.acuranza.app.ui.settings.SmsTrackingScreen(nav) }
        composable(Routes.SETTINGS_PRIVACY) { com.acuranza.app.ui.settings.PrivacySecurityPage(nav) }
        composable(Routes.SETTINGS_NOTIFICATIONS) { com.acuranza.app.ui.settings.NotificationsPage(nav) }
        composable(Routes.SETTINGS_APPEARANCE) { com.acuranza.app.ui.settings.AppearancePage(nav) }
        composable(Routes.SETTINGS_EDIT) { com.acuranza.app.ui.settings.SettingsEditScreen(nav) }
        composable(Routes.SETTINGS_HAPTICS) { com.acuranza.app.ui.settings.HapticSettingsScreen(nav) }
        composable(Routes.SETTINGS_NAV_TABS) { com.acuranza.app.ui.settings.NavTabsEditorScreen(nav) }
        composable(Routes.SETTINGS_DATA) { com.acuranza.app.ui.settings.DataManagementPage(nav) }
        composable(Routes.SETTINGS_SMS_IMPORTS) { com.acuranza.app.ui.settings.SmsImportsScreen(nav) }
        composable(Routes.SAVINGS_GOAL) { com.acuranza.app.ui.home.SavingsGoalScreen(nav) }
        composable(Routes.WIDGETS_PREVIEW) { com.acuranza.app.ui.tools.WidgetsPreviewScreen(nav) }
        composable(Routes.PROFILE) { com.acuranza.app.ui.settings.ProfileScreen(nav) }
        composable(Routes.INSIGHTS) { InsightsScreen(nav) }
        composable(Routes.CATEGORIES) { CategoriesScreen(nav) }
        composable(Routes.PENDING_ACCOUNTS) { com.acuranza.app.ui.accounts.PendingAccountsScreen(nav) }
        composable(Routes.UNCATEGORISED) { com.acuranza.app.ui.transactions.UncategorisedScreen(nav) }
        composable(Routes.RECURRING) { com.acuranza.app.ui.features.RecurringScreen(nav) }
        composable(
            Routes.RECURRING_EDIT,
            arguments = listOf(
                navArgument("id") { type = NavType.LongType; defaultValue = -1L },
                navArgument("series") { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
        ) { entry ->
            com.acuranza.app.ui.features.RecurringEditScreen(
                nav,
                entry.arguments!!.getLong("id").takeIf { it > 0 },
                entry.arguments!!.getString("series")?.takeIf { it.isNotBlank() },
            )
        }
        composable(Routes.LOANS) { com.acuranza.app.ui.features.LoansScreen(nav) }
        composable(Routes.LOAN_DETAIL, arguments = idArg) { entry -> com.acuranza.app.ui.features.LoanDetailScreen(nav, entry.arguments!!.getLong("id")) }
        composable(Routes.LOAN_EDIT, arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L })) { entry ->
            com.acuranza.app.ui.features.LoanEditScreen(nav, entry.arguments!!.getLong("id").takeIf { it > 0 })
        }
        composable(Routes.DEBTS) { com.acuranza.app.ui.features.DebtsScreen(nav) }
        composable(Routes.AUTO_BACKUP) { com.acuranza.app.ui.features.AutoBackupScreen(nav) }
        composable(Routes.STATEMENT_IMPORT) { com.acuranza.app.ui.features.StatementImportScreen(nav) }
        composable(Routes.REPORTS) { com.acuranza.app.ui.features.ReportsScreen(nav) }
        composable(Routes.YEAR_REVIEW, arguments = listOf(navArgument("year") { type = NavType.IntType })) { entry ->
            com.acuranza.app.ui.features.YearReviewScreen(nav, entry.arguments!!.getInt("year"))
        }
        composable(Routes.CALENDAR) { com.acuranza.app.ui.features.CalendarScreen(nav) }
        composable(Routes.ABOUT) { com.acuranza.app.ui.about.AboutScreen(nav) }
        composable(Routes.ABOUT_PRIVACY) { com.acuranza.app.ui.about.PrivacyPolicyScreen(nav) }
        composable(Routes.ABOUT_TERMS) { com.acuranza.app.ui.about.TermsScreen(nav) }
        composable(Routes.ABOUT_GPL) { com.acuranza.app.ui.about.GplScreen(nav) }
    }
}

