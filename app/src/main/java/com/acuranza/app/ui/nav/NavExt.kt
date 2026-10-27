package com.acuranza.app.ui.nav

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

/**
 * Pops the current screen only when it is the resumed, visible screen and something is underneath.
 *
 * A double tap on a back arrow or a Save button otherwise pops twice: the second pop can remove the screen
 * underneath, or empty the back stack and leave a blank window with no bottom bar.
 */
fun NavController.popBackSafely(): Boolean {
    val entry = currentBackStackEntry ?: return false
    if (entry.lifecycle.currentState != Lifecycle.State.RESUMED) return false
    if (previousBackStackEntry == null) return false
    return popBackStack()
}

/**
 * Leaves an editor and the detail screen it was opened from (e.g. after deleting the item), falling back to
 * a plain [popBackSafely] when the detail screen isn't on the stack.
 */
fun NavController.popPast(detailRoute: String): Boolean {
    val entry = currentBackStackEntry ?: return false
    if (entry.lifecycle.currentState != Lifecycle.State.RESUMED) return false
    val hasDetail = runCatching { getBackStackEntry(detailRoute) }.isSuccess
    return if (hasDetail) popBackStack(detailRoute, inclusive = true) else popBackSafely()
}

/** Navigates to a bottom-bar tab, keeping one back stack per tab. Popping up to the start
 * destination clears any nested screens (e.g. Settings → Appearance) so tapping a tab always
 * returns to that tab's top level. If the tab is already the current top destination, the call
 * is a no-op — this prevents NavController state churn when the user taps the active tab.
 *
 * For the Home tab specifically, a re-tap while already on Home is surfaced to the caller via
 * [onHomeReTap]; the caller is responsible for scrolling to the top and refreshing content.
 */
fun NavController.navigateToTab(route: String, onHomeReTap: (() -> Unit)? = null) {
    val alreadyOnTop = currentBackStackEntry?.destination?.route == route
    if (alreadyOnTop) {
        if (route == Routes.HOME) onHomeReTap?.invoke()
        return
    }
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = false
    }
}
