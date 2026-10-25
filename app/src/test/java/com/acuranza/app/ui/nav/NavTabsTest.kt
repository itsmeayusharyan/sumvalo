package com.acuranza.app.ui.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Edit-Navigation-Tabs model: layout/label/icon encodings, unknown-id safety, landing-guard
 * and the visible-tab minimum — all pure, so persistence and reset are testable without DataStore.
 */
class NavTabsTest {

    @Test
    fun `null layout is today's four tabs in order`() {
        val base = NavTabs.decodeLayout(null)
        assertEquals(Tabs.map { it.route }, base.map { it.first })
        assertTrue(base.all { it.second })
    }

    @Test
    fun `layout round-trips with order and hidden markers`() {
        val layout = listOf("insights" to true, "home" to true, "settings" to false, "transactions" to true)
        assertEquals(layout, NavTabs.decodeLayout(NavTabs.encodeLayout(layout)))
    }

    @Test
    fun `unknown ids drop out and missing tabs return visible`() {
        val decoded = NavTabs.decodeLayout("home,garbage,!transactions")
        assertEquals(Tabs.map { it.route }.toSet(), decoded.map { it.first }.toSet())
        assertEquals(true, decoded.first { it.first == "insights" }.second)
        assertEquals(false, decoded.first { it.first == "transactions" }.second)
    }

    @Test
    fun `labels keep only valid routes and sane names`() {
        val labels = NavTabs.decodeLabels("home:House,nowhere:Nope,insights:")
        assertEquals(mapOf("home" to "House"), labels)
    }

    @Test
    fun `icons accept only curated choices for that tab`() {
        assertEquals(mapOf("insights" to "bar_chart"), NavTabs.decodeIcons("insights:bar_chart"))
        assertEquals(emptyMap<String, String>(), NavTabs.decodeIcons("insights:home,home:nonexistent"))
    }

    @Test
    fun `resolve applies overrides display-only`() {
        val resolved = NavTabs.resolve(
            NavTabs.decodeLayout("home,!insights,transactions,settings"),
            mapOf("home" to "Casa"),
            mapOf("insights" to "pie_chart"),
        )
        assertEquals("Casa", resolved.first { it.route == "home" }.label)
        assertEquals("pie_chart", resolved.first { it.route == "insights" }.iconId)
        assertEquals("Insights", resolved.first { it.route == "insights" }.label) // untouched tab keeps its name
        assertEquals("insights", resolved.first { !it.visible }.route)
    }

    @Test
    fun `move ignores out-of-range and no-op`() {
        val base = NavTabs.decodeLayout(null)
        assertEquals(base, NavTabs.move(base, 0, 0))
        assertEquals(base, NavTabs.move(base, -1, 2))
        assertEquals(base, NavTabs.move(base, 0, 99))
    }

    @Test
    fun `hiding the landing tab moves the landing to the first visible tab`() {
        val layout = NavTabs.decodeLayout("home,insights,!transactions,settings")
        assertEquals("home", NavTabs.landingAfterHide("transactions", layout))
        assertEquals("home", NavTabs.landingAfterHide(null, layout))
        // Landing still visible: unchanged.
        assertEquals("settings", NavTabs.landingAfterHide("settings", layout))
        // Everything hidden is refused upstream by the two-visible minimum; null only if empty.
        assertEquals(null, NavTabs.landingAfterHide("home", listOf("home" to false)))
    }

    @Test
    fun `the default icon is today's look`() {
        assertEquals("home", NavTabs.iconOf("home", null).id)
        assertEquals("query_stats", NavTabs.iconOf("insights", "garbage").id)
        assertEquals("receipt_long", NavTabs.iconOf("transactions", null).id)
        assertEquals("settings", NavTabs.iconOf("settings", null).id)
    }
}
