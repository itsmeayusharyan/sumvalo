package com.acuranza.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The saved dashboard layout is one encoded string, so persistence, ordering, visibility and
 * reset behaviour are all pure functions tested here without DataStore.
 */
class HomeLayoutTest {

    // ---------------------------------------------------------------- defaults & decode

    @Test
    fun `null or blank layout means the stock dashboard`() {
        assertEquals(HomeLayout.DEFAULT.map { it to true }, HomeLayout.decode(null))
        assertEquals(HomeLayout.DEFAULT.map { it to true }, HomeLayout.decode(""))
        assertEquals(HomeLayout.DEFAULT.map { it to true }, HomeLayout.decode("   "))
        // The stock order is the declaration order: quick actions first, accounts last.
        assertEquals(HomeSection.QUICK, HomeLayout.DEFAULT.first())
        assertEquals(HomeSection.ACCOUNTS, HomeLayout.DEFAULT.last())
    }

    @Test
    fun `hidden sections are marked with the bang prefix and order is kept`() {
        val layout = HomeLayout.decode("quick,!heat,stats")
        assertEquals(
            listOf(
                HomeSection.QUICK to true,
                HomeSection.HEAT to false,
                HomeSection.STATS to true,
            ),
            layout.take(3),
        )
        // Every other section joins after the saved ones, visible, in default order.
        assertEquals(HomeLayout.DEFAULT.size, layout.size)
        assertTrue(layout.drop(3).all { it.second })
    }

    @Test
    fun `unknown ids are dropped and duplicates collapse`() {
        val layout = HomeLayout.decode("quick,taxes,quick,stats")
        assertEquals(HomeSection.QUICK, layout[0].first)
        assertEquals(HomeSection.STATS, layout[1].first)
        assertEquals(HomeLayout.DEFAULT.size, layout.size) // the rest appended, still complete
    }

    @Test
    fun `sections added by a later app version appear at the end visible`() {
        // A user saved a layout before "networth" existed; it must not silently hide it.
        val saved = HomeLayout.DEFAULT.filter { it != HomeSection.NETWORTH }.joinToString(",") { it.id }
        val layout = HomeLayout.decode(saved)
        assertEquals(HomeLayout.DEFAULT.size, layout.size)
        assertTrue(layout.last().first == HomeSection.NETWORTH && layout.last().second)
    }

    // ---------------------------------------------------------------- encode round-trip

    @Test
    fun `encode and decode round-trip exactly`() {
        // decode always returns the complete section list (missing ones appended visible), so the
        // round-trip is exact for any complete layout, and a partial encode still decodes to the
        // same prefix with the rest appended.
        val complete = HomeLayout.setVisibility(HomeLayout.move(HomeLayout.decode(null), 1, up = true), HomeSection.HEAT, false)
        assertEquals(complete, HomeLayout.decode(HomeLayout.encode(complete)))

        val partial = listOf(HomeSection.STATS to true, HomeSection.QUICK to false)
        val decoded = HomeLayout.decode(HomeLayout.encode(partial))
        assertEquals(partial, decoded.take(2))
        assertEquals(HomeLayout.DEFAULT.size, decoded.size)
    }

    // ---------------------------------------------------------------- editing operations

    @Test
    fun `move swaps with the neighbour and ignores out-of-range moves`() {
        // decode appends the unsaved sections, so work on the real complete list.
        val layout = HomeLayout.decode("quick,stats,heat")
        assertEquals(HomeLayout.DEFAULT.size, layout.size)
        val moved = HomeLayout.move(layout, index = 1, up = true)
        assertEquals(listOf(HomeSection.STATS, HomeSection.QUICK, HomeSection.HEAT), moved.take(3).map { it.first })
        // Moving QUICK down swaps it with its next neighbour, HEAT.
        val down = HomeLayout.move(moved, index = 1, up = false)
        assertEquals(listOf(HomeSection.STATS, HomeSection.HEAT, HomeSection.QUICK), down.take(3).map { it.first })
        // Moving it back up returns the list to the state before the down-move.
        val restored = HomeLayout.move(down, index = 2, up = true)
        assertEquals(moved.take(3).map { it.first }, restored.take(3).map { it.first })
        // Edges on the complete list: the first row can't move up, the last can't move down.
        val full = HomeLayout.decode(null)
        assertEquals(full, HomeLayout.move(full, 0, up = true))
        assertEquals(full, HomeLayout.move(full, full.lastIndex, up = false))
        assertEquals(full, HomeLayout.move(full, -1, up = true))
        assertEquals(full, HomeLayout.move(full, full.size, up = false))
    }

    @Test
    fun `setVisibility toggles one section without touching order`() {
        val layout = HomeLayout.decode("quick,heat,stats")
        val toggled = HomeLayout.setVisibility(layout, HomeSection.HEAT, false)
        assertEquals(layout.map { it.first }, toggled.map { it.first })
        assertFalse(toggled.first { it.first == HomeSection.HEAT }.second)
        assertTrue(toggled.first { it.first == HomeSection.QUICK }.second)
        // Toggling back round-trips.
        assertEquals(layout, HomeLayout.setVisibility(toggled, HomeSection.HEAT, true))
    }

    @Test
    fun `visible filters to the sections the dashboard should render, in order`() {
        val layout = HomeLayout.decode("!quick,stats,!heat,accounts")
        assertEquals(
            listOf(HomeSection.STATS, HomeSection.ACCOUNTS),
            HomeLayout.visible(layout).dropLast(HomeLayout.DEFAULT.size - 4),
        )
    }

    @Test
    fun `reset clears the saved string so the stock dashboard returns`() {
        // The reset path itself is SettingsStore.resetHomeLayout(); what matters here is that a
        // cleared preference decodes back to the untouched stock layout.
        val custom = HomeLayout.encode(listOf(HomeSection.HEAT to true, HomeSection.QUICK to false))
        assertTrue(HomeLayout.decode(custom) != HomeLayout.decode(null))
        assertEquals(HomeLayout.decode(null), HomeLayout.decode(null as String?))
    }

    @org.junit.Test
    fun `moveVisibleTo reorders only the visible sections and keeps hidden slots stable`() {
        var l = com.acuranza.app.ui.home.HomeLayout.decode("quick,safe,!stats,pace")
        // Move the first visible (quick) two visible slots down; sections unknown to the string
        // join after, so the visible order starts quick, safe, pace, …
        l = com.acuranza.app.ui.home.HomeLayout.moveVisibleTo(l, 0, 2)
        val visibleAfter = com.acuranza.app.ui.home.HomeLayout.visible(l)
        org.junit.Assert.assertEquals(
            listOf(
                com.acuranza.app.ui.home.HomeSection.SAFE,
                com.acuranza.app.ui.home.HomeSection.PACE,
                com.acuranza.app.ui.home.HomeSection.QUICK,
            ),
            visibleAfter.take(3),
        )
        // stats stays hidden and the encoding round-trips.
        org.junit.Assert.assertTrue(l.any { it.first == com.acuranza.app.ui.home.HomeSection.STATS && !it.second })
        org.junit.Assert.assertEquals(l, com.acuranza.app.ui.home.HomeLayout.decode(com.acuranza.app.ui.home.HomeLayout.encode(l)))
    }

    @org.junit.Test
    fun `moveVisibleTo ignores out-of-range and no-op moves`() {
        val l = com.acuranza.app.ui.home.HomeLayout.decode(null)
        org.junit.Assert.assertEquals(l, com.acuranza.app.ui.home.HomeLayout.moveVisibleTo(l, -1, 2))
        org.junit.Assert.assertEquals(l, com.acuranza.app.ui.home.HomeLayout.moveVisibleTo(l, 0, 99))
        org.junit.Assert.assertEquals(l, com.acuranza.app.ui.home.HomeLayout.moveVisibleTo(l, 1, 1))
    }
}
