package com.acuranza.app.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The dashboard sections a user can show, hide and reorder from Settings → Appearance → Edit home.
 * The enum's declaration order is the default dashboard order, and [id] is what gets persisted,
 * so renaming a label never breaks a saved layout and dropping a section from the app degrades
 * gracefully on the next decode.
 */
enum class HomeSection(val id: String, val label: String, val description: String) {
    QUICK("quick", "Quick actions", "Transfer, bills, reports and accounts"),
    SAFE("safe", "Left to spend", "What's safe to spend until payday"),
    STATS("stats", "This month", "Income, spending and savings so far"),
    PACE("pace", "Spending pace", "This month against your usual pace"),
    BILLS("bills", "Upcoming bills", "Card bills that are due"),
    INSIGHT("insight", "Insight", "The most important finding right now"),
    SPEND("spend", "Where it went", "Category breakdown of this month's spending"),
    SIX("six", "Income vs spending", "Six months of income against spending"),
    HEAT("heat", "Daily spending", "A day-by-day heatmap of the month"),
    NETWORTH("networth", "Net worth trend", "How your net worth moved over time"),
    RECENT("recent", "Recent", "Your latest transactions"),
    ACCOUNTS("accounts", "Accounts", "Balances across your accounts"),
}

/** The section's icon in the Edit-home list; one consistent outline style. */
val HomeSection.icon: ImageVector
    get() = when (this) {
        HomeSection.QUICK -> Icons.Outlined.Apps
        HomeSection.SAFE -> Icons.Outlined.Savings
        HomeSection.STATS -> Icons.Outlined.Equalizer
        HomeSection.PACE -> Icons.Outlined.Speed
        HomeSection.BILLS -> Icons.Outlined.CreditCard
        HomeSection.INSIGHT -> Icons.Outlined.Bolt
        HomeSection.SPEND -> Icons.Outlined.PieChart
        HomeSection.SIX -> Icons.Outlined.BarChart
        HomeSection.HEAT -> Icons.Outlined.CalendarMonth
        HomeSection.NETWORTH -> Icons.AutoMirrored.Outlined.ShowChart
        HomeSection.RECENT -> Icons.AutoMirrored.Outlined.ReceiptLong
        HomeSection.ACCOUNTS -> Icons.Outlined.AccountBalance
    }

/**
 * Pure encode/decode for the persisted home layout, so persistence, ordering and reset behaviour
 * are unit-testable without DataStore. Layouts are stored as one comma-separated string of section
 * ids; a `!` prefix marks a hidden section.
 */
object HomeLayout {
    const val HIDDEN_PREFIX = "!"
    val DEFAULT: List<HomeSection> = HomeSection.entries.toList()

    /** Every known section with its visibility, in the saved order. `null` means stock layout. */
    fun decode(raw: String?): List<Pair<HomeSection, Boolean>> {
        if (raw.isNullOrBlank()) return DEFAULT.map { it to true }
        val saved = raw.split(',').filter { it.isNotBlank() }
        val known = saved.mapNotNull { token ->
            val hidden = token.startsWith(HIDDEN_PREFIX)
            HomeSection.entries.firstOrNull { it.id == token.removePrefix(HIDDEN_PREFIX) }
                ?.let { it to !hidden }
        }.distinctBy { it.first }
        // Sections unknown to the saved layout (added by a later app version) join at the end, visible.
        val seen = known.map { it.first }.toSet()
        return known + DEFAULT.filter { it !in seen }.map { it to true }
    }

    fun encode(layout: List<Pair<HomeSection, Boolean>>): String =
        layout.joinToString(",") { (section, visible) ->
            if (visible) section.id else HIDDEN_PREFIX + section.id
        }

    /** The sections the dashboard should render, in order. */
    fun visible(layout: List<Pair<HomeSection, Boolean>>): List<HomeSection> =
        layout.filter { it.second }.map { it.first }

    /** Swaps [index] with its neighbour; out-of-range moves are ignored. */
    fun move(layout: List<Pair<HomeSection, Boolean>>, index: Int, up: Boolean): List<Pair<HomeSection, Boolean>> {
        val to = if (up) index - 1 else index + 1
        if (index !in layout.indices || to !in layout.indices) return layout
        return layout.toMutableList().apply { add(to, removeAt(index)) }
    }

    /**
     * Drag-and-drop reorder within the VISIBLE sections: moves the visible entry at
     * [fromVisible] to [toVisible], walking over hidden entries so their relative positions in
     * the saved order stay stable. Both indexes count visible sections only. Out-of-range or
     * no-op moves return the layout unchanged.
     */
    fun moveVisibleTo(
        layout: List<Pair<HomeSection, Boolean>>,
        fromVisible: Int,
        toVisible: Int,
    ): List<Pair<HomeSection, Boolean>> {
        val visibleIdx = layout.indices.filter { layout[it].second }
        if (fromVisible !in visibleIdx.indices || toVisible !in visibleIdx.indices || fromVisible == toVisible) return layout
        val from = visibleIdx[fromVisible]
        val to = visibleIdx[toVisible]
        return layout.toMutableList().apply { add(to, removeAt(from)) }
    }

    /** Toggles one section's visibility, leaving order untouched. */
    fun setVisibility(
        layout: List<Pair<HomeSection, Boolean>>,
        section: HomeSection,
        visible: Boolean,
    ): List<Pair<HomeSection, Boolean>> = layout.map {
        if (it.first == section) it.first to visible else it
    }
}
