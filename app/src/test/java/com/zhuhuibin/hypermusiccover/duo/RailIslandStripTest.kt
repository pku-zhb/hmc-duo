package com.zhuhuibin.hypermusiccover.duo

import org.junit.Assert.*
import org.junit.Test

class RailIslandStripTest {
    private val keys = listOf("notifications", "delivery", "timer", "music")
    @Test fun updatesKeepCategorySeatsAndExistingFocusOrder() {
        assertEquals(keys, RailIslandStrip.order(listOf("music", "timer", "delivery", "notifications"),
            listOf("delivery", "timer"), "notifications", "music"))
        assertEquals(listOf("notifications", "music"),
            RailIslandStrip.order(listOf("music", "notifications"), keys, "notifications", "music"))
    }
    @Test fun twoIslandsSwapTowardTheirNeighbourWithoutDroppingOrDuplicatingABubble() {
        val two = listOf("notifications", "music")
        var selected = "music"
        repeat(6) {
            val up = selected == "music"
            val next = RailIslandStrip.adjacent(two, selected, up)!!
            assertNotEquals(selected, next)
            assertEquals(selected, RailIslandStrip.adjacent(two, next, !up))
            val plan = RailIslandStrip.layout(two, next, 46f, 460f, 1f, false)!!
            assertEquals(two, plan.slots.map { it.key })
            assertEquals(1, plan.slots.count { it.primary })
            selected = next
        }
    }
    @Test fun hyperOsSwipesVisitOnlyTheImmediateSeatInEachDirection() {
        assertEquals("delivery", RailIslandStrip.adjacent(keys, "notifications", false))
        assertEquals("notifications", RailIslandStrip.adjacent(keys, "delivery", true))
        assertEquals("timer", RailIslandStrip.adjacent(keys, "delivery", false))
        assertEquals("delivery", RailIslandStrip.adjacent(keys, "timer", true))
        assertEquals("music", RailIslandStrip.adjacent(keys, "timer", false))
        assertEquals("timer", RailIslandStrip.adjacent(keys, "music", true))
    }
    @Test fun hyperOsCannotWrapPastTheTopOrBottomSeat() {
        assertNull(RailIslandStrip.adjacent(keys, "notifications", true))
        assertNull(RailIslandStrip.adjacent(keys, "music", false))
    }
    @Test fun aMissingSelectionDoesNotInventAnAdjacentTarget() {
        for (up in listOf(false, true)) {
            assertNull(RailIslandStrip.adjacent(keys, null, up))
            assertNull(RailIslandStrip.adjacent(keys, "removed", up))
        }
    }
    @Test fun emptyAndSingleItemRailsHaveNoAdjacentTarget() {
        for (up in listOf(false, true)) {
            assertNull(RailIslandStrip.adjacent(emptyList(), null, up))
            assertNull(RailIslandStrip.adjacent(emptyList(), "music", up))
            assertNull(RailIslandStrip.adjacent(listOf("music"), "music", up))
        }
    }
    @Test fun colorOsRetainsItsCircularSelectionAtBothEnds() {
        assertEquals("music", RailIslandStrip.cycle(keys, "notifications", true))
        assertEquals("notifications", RailIslandStrip.cycle(keys, "music", false))
    }
    @Test fun contentSwipesOpenMusicThenLiveCardsThenNotifications() {
        fun next(collapsed: List<String>) = RailIslandStrip.expansionTarget(collapsed, "notifications", "music")
        assertEquals("music", next(keys))
        assertEquals("delivery", next(keys - "music"))
        assertEquals("timer", next(keys - "music" - "delivery"))
        assertEquals("notifications", next(listOf("notifications")))
        assertEquals("music", next(listOf("music")))
        assertNull(next(emptyList()))
    }
    @Test fun everySelectionFitsAboveShortcutsWithoutChangingVisualOrder() {
        for (selected in keys) {
            val p = RailIslandStrip.layout(keys, selected, 46f, 460f, 1f, false, 4)!!
            assertEquals(keys, p.slots.map { it.key })
            assertEquals(0, p.hidden)
            assertTrue(p.slots.first().top >= 0f)
            p.slots.zipWithNext().forEach { (a, b) -> assertTrue(a.top + a.height + 8f <= b.top + .001f) }
            assertEquals(460f, p.slots.last().let { it.top + it.height }, .001f)
            assertEquals(selected, p.slots.single { it.primary }.key)
        }
    }
    @Test fun overflowKeepsTheSelectedSeatVisibleAndEveryItemReachable() {
        val many = (0..15).map { "focus-$it" }
        var selected = many.first()
        val visited = mutableSetOf<String>()
        repeat(many.size) {
            visited += selected
            val p = RailIslandStrip.layout(many, selected, 46f, 460f, 1f, false)!!
            assertTrue(p.hidden > 0)
            assertEquals(selected, p.slots.single { it.primary }.key)
            assertTrue(p.slots.all { it.top >= 0 && it.top + it.height <= 460.001f })
            if (visited.size < many.size) selected = RailIslandStrip.adjacent(many, selected, false)!!
        }
        assertEquals(many.toSet(), visited)
        assertNull(RailIslandStrip.adjacent(many, selected, false))
    }
    @Test fun colorOsKeepsOneCapsuleWithTheSameSelectionPriority() {
        val p = RailIslandStrip.layout(keys, "delivery", 46f, 460f, 1f, true)!!
        assertEquals(listOf("delivery"), p.slots.map { it.key })
        assertNull(RailIslandStrip.layout(keys, "missing", 46f, 460f, 1f, false))
    }
}
