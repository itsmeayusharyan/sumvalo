package com.acuranza.app.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cottage
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.Cottage
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Weekend
import androidx.compose.material.icons.outlined.Bungalow
import androidx.compose.material.icons.filled.Bungalow
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Edit Navigation Tabs (Settings → Appearance → Edit navigation tabs): the customization model for
 * the bottom bar. Everything is pure and string-encoded, mirroring [com.acuranza.app.ui.home.HomeLayout]:
 * stable ids persist, unknown ids drop out, missing tabs return at their default spot, and display
 * customizations (labels, icons) never touch a tab's route or destination — only what the user sees.
 *
 * Encodings (all comma/colon strings in the existing preferences):
 *  - layout:   `home,insights,!activity,settings` — order, `!` marks hidden
 *  - labels:   `home:House,insights:Stats` — overrides only, display-only
 *  - icons:    `insights:bar_chart` — overrides only, ids from [ICON_CHOICES]
 */
object NavTabs {

    /** A selectable icon pair (unselected + selected) for a tab. */
    data class IconChoice(val id: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

    /** The curated icon choices per tab — every pair exists in the app's icon dependency. */
    val ICON_CHOICES: Map<String, List<IconChoice>> = mapOf(
        Routes.HOME to listOf(
            IconChoice("home", "House", Icons.Outlined.Home, Icons.Filled.Home),
            IconChoice("cottage", "Cottage", Icons.Outlined.Cottage, Icons.Filled.Cottage),
            IconChoice("weekend", "Armchair", Icons.Outlined.Weekend, Icons.Filled.Weekend),
            IconChoice("bungalow", "Bungalow", Icons.Outlined.Bungalow, Icons.Filled.Bungalow),
            IconChoice("space_dashboard", "Dashboard", Icons.Outlined.SpaceDashboard, Icons.Filled.SpaceDashboard),
        ),
        Routes.INSIGHTS to listOf(
            IconChoice("query_stats", "Chart", Icons.Outlined.QueryStats, Icons.Filled.QueryStats),
            IconChoice("bar_chart", "Bars", Icons.Outlined.BarChart, Icons.Filled.BarChart),
            IconChoice("pie_chart", "Pie", Icons.Outlined.PieChart, Icons.Filled.PieChart),
            IconChoice("trending_up", "Trend", Icons.Outlined.TrendingUp, Icons.Filled.TrendingUp),
        ),
        Routes.TRANSACTIONS to listOf(
            IconChoice("receipt_long", "Receipt", Icons.AutoMirrored.Outlined.ReceiptLong, Icons.AutoMirrored.Filled.ReceiptLong),
            IconChoice("receipt", "Stub", Icons.Outlined.Receipt, Icons.Filled.Receipt),
            IconChoice("list_alt", "List", Icons.AutoMirrored.Outlined.ListAlt, Icons.AutoMirrored.Filled.ListAlt),
            IconChoice("assignment", "Clipboard", Icons.Outlined.Assignment, Icons.AutoMirrored.Filled.Assignment),
        ),
        Routes.SETTINGS to listOf(
            IconChoice("settings", "Gear", Icons.Outlined.Settings, Icons.Filled.Settings),
            IconChoice("tune", "Sliders", Icons.Outlined.Tune, Icons.Filled.Tune),
            IconChoice("construction", "Tools", Icons.Outlined.Construction, Icons.Filled.Build),
        ),
    )

    /** The default icon id per tab (today's look). */
    private val DEFAULT_ICON = mapOf(
        Routes.HOME to "home",
        Routes.INSIGHTS to "query_stats",
        Routes.TRANSACTIONS to "receipt_long",
        Routes.SETTINGS to "settings",
    )

    /** One tab as the user customized it. */
    data class ResolvedTab(
        val route: String,
        val label: String,
        val iconId: String,
        val visible: Boolean,
    )

    fun iconOf(route: String, iconId: String?): IconChoice =
        ICON_CHOICES[route]?.firstOrNull { it.id == iconId } ?: ICON_CHOICES[route]!!.first { it.id == DEFAULT_ICON[route] }

    /** Order + visibility from the saved layout string; unknown ids drop, missing tabs return. */
    fun decodeLayout(raw: String?): List<Pair<String, Boolean>> {
        val base = Tabs.map { it.route to true }
        if (raw.isNullOrBlank()) return base
        val saved = raw.split(",").mapNotNull { token ->
            val hidden = token.startsWith("!")
            Tabs.firstOrNull { it.route == token.removePrefix("!") }?.let { it.route to !hidden }
        }.distinctBy { it.first }
        val seen = saved.map { it.first }.toSet()
        return saved + base.filter { it.first !in seen }
    }

    fun encodeLayout(layout: List<Pair<String, Boolean>>): String =
        layout.joinToString(",") { if (it.second) it.first else "!" + it.first }

    /** `route:Label` overrides — only valid routes keep, invalid labels ignored. */
    fun decodeLabels(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split(",").mapNotNull { token ->
            val route = token.substringBefore(":")
            val label = token.substringAfter(":").trim()
            if (token.contains(":") && route in Tabs.map { it.route } && label.isNotEmpty() && label.length <= 12) route to label else null
        }.toMap()
    }

    fun encodeLabels(labels: Map<String, String>): String =
        labels.entries.joinToString(",") { "${it.key}:${it.value}" }

    /** `route:iconId` overrides — only curated ids for that route are accepted. */
    fun decodeIcons(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split(",").mapNotNull { token ->
            if (!token.contains(":")) return@mapNotNull null
            val route = token.substringBefore(":")
            val iconId = token.substringAfter(":")
            if (ICON_CHOICES[route]?.any { it.id == iconId } == true) route to iconId else null
        }.toMap()
    }

    fun encodeIcons(icons: Map<String, String>): String =
        icons.entries.joinToString(",") { "${it.key}:${it.value}" }

    /** The tabs to render, in order, with the user's labels and icons resolved. */
    fun resolve(layout: List<Pair<String, Boolean>>, labels: Map<String, String>, icons: Map<String, String>): List<ResolvedTab> =
        layout.map { (route, visible) ->
            ResolvedTab(
                route = route,
                label = labels[route] ?: Tabs.first { it.route == route }.label,
                iconId = icons[route] ?: DEFAULT_ICON.getValue(route),
                visible = visible,
            )
        }

    /** Reorders within the layout; out-of-range and hidden-slot moves keep hidden positions stable. */
    fun move(layout: List<Pair<String, Boolean>>, from: Int, to: Int): List<Pair<String, Boolean>> {
        if (from == to || from !in layout.indices || to !in layout.indices) return layout
        return layout.toMutableList().apply { add(to, removeAt(from)) }
    }

    fun setVisibility(layout: List<Pair<String, Boolean>>, route: String, visible: Boolean): List<Pair<String, Boolean>> =
        layout.map { if (it.first == route) it.first to visible else it }

    /**
     * The landing-tab guard: a hidden tab can't be the default landing tab, so hiding it moves
     * the landing to the first visible tab — never a crash, never an unreachable destination.
     */
    fun landingAfterHide(landingTab: String?, layout: List<Pair<String, Boolean>>): String? {
        val visible = layout.filter { it.second }.map { it.first }
        if (visible.isEmpty()) return null
        return if (landingTab != null && landingTab in visible) landingTab else visible.first()
    }
}
