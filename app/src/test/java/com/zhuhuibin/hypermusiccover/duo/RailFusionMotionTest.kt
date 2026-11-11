package com.zhuhuibin.hypermusiccover.duo

import org.junit.Assert.*
import org.junit.Test

class RailFusionMotionTest {
    private val keys = listOf("notifications", "calendar", "music")
    private fun slots(keys: List<String>, selected: String, single: Boolean = false) =
        RailIslandStrip.layout(keys, selected, 46f, 460f, 1f, single)!!.slots.associate {
            it.key to RailFusionMotion.Box(10f, it.top, 46f, it.height)
        }
    private fun art(box: RailFusionMotion.Box) = RailFusionMotion.Box(box.x + 8, box.y + 8, 30f, 30f)
    private fun frame(plan: RailFusionMotion.Plan, t: Float) = plan.frame(t,
        art(plan.before.getValue(plan.from)), art(plan.after.getValue(plan.to)), .62f)
    private fun assertBox(a: RailFusionMotion.Box, b: RailFusionMotion.Box, tolerance: Float = .001f) {
        assertEquals(a.x, b.x, tolerance); assertEquals(a.y, b.y, tolerance)
        assertEquals(a.w, b.w, tolerance); assertEquals(a.h, b.h, tolerance)
    }

    @Test fun twoSeatsMergeInBothDirectionsAndKeepBothIconsAtTheHandoff() {
        val two = listOf("notifications", "music")
        for ((from, to) in listOf(two[0] to two[1], two[1] to two[0])) {
            val plan = RailFusionMotion.plan(from, to, slots(two, from), slots(two, to))!!
            assertTrue(plan.joined)
            val before = frame(plan, .49999f)
            val after = frame(plan, .5f)
            assertBox(before.primary, after.primary, .01f)
            // Outgoing artwork arrives at its own circle, incoming artwork leaves its old circle.
            assertEquals(after.seats.getValue(from).cx, before.icon.cx, .01f)
            assertEquals(after.seats.getValue(from).cy, before.icon.cy, .01f)
            assertEquals(before.seats.getValue(to).cx, after.icon.cx, .01f)
            assertEquals(before.seats.getValue(to).cy, after.icon.cy, .01f)
            assertEquals(1f, before.iconAlpha, 0f)
            assertEquals(1f, after.iconAlpha, 0f)
            assertEquals(0f, before.detail, .001f)
            assertEquals(0f, after.detail, .001f)
            assertEquals(0f, before.companionMaterial, .001f)
            assertEquals(0f, after.companionMaterial, .001f)
        }
    }

    @Test fun adjacentSwitchDoesNotMoveOrCoverTheUnrelatedBubble() {
        for ((from, to) in listOf("music" to "calendar", "notifications" to "calendar")) {
            val plan = RailFusionMotion.plan(from, to, slots(keys, from), slots(keys, to))!!
            val unrelated = keys.single { it != from && it != to }
            repeat(101) { i ->
                val f = frame(plan, i / 100f)
                val untouched = plan.before.getValue(unrelated)
                assertBox(untouched, f.seats.getValue(unrelated))
                assertTrue(f.primary.y + f.primary.h <= untouched.y ||
                    f.primary.y >= untouched.y + untouched.h)
            }
        }
    }

    @Test fun wrappingBetweenDistantSeatsPreservesEveryInterveningGap() {
        val many = listOf("notifications", "delivery", "calendar", "music")
        for ((from, to) in listOf(many.first() to many.last(), many.last() to many.first())) {
            val plan = RailFusionMotion.plan(from, to, slots(many, from), slots(many, to))!!
            assertFalse(plan.joined)
            repeat(101) { i ->
                val f = frame(plan, i / 100f)
                assertEquals(many, f.seats.keys.toList())
                f.seats.values.zipWithNext().forEach { (a, b) ->
                    assertEquals(12f, b.y - a.y - a.h, .001f)
                }
                assertTrue(f.seats.values.all { it.valid() })
            }
            val a = frame(plan, .49999f)
            val b = frame(plan, .5f)
            assertEquals(b.seats.getValue(from).cy, a.icon.cy, .01f)
            assertEquals(a.seats.getValue(to).cy, b.icon.cy, .01f)
        }
    }

    @Test fun bothNativeShellsLandExactlyWithoutASecondRelocation() {
        for (from in keys) for (to in keys.filter { it != from }) {
            val before = slots(keys, from)
            val after = slots(keys, to)
            val plan = RailFusionMotion.plan(from, to, before, after)!!
            val first = frame(plan, 0f)
            val last = frame(plan, 1f)
            assertFalse(first.incoming); assertTrue(last.incoming)
            assertBox(before.getValue(from), first.primary)
            assertBox(after.getValue(to), last.primary)
            before.forEach { (key, box) -> assertBox(box, first.seats.getValue(key)) }
            after.forEach { (key, box) -> assertBox(box, last.seats.getValue(key)) }
            assertBox(art(before.getValue(from)), first.icon)
            assertBox(art(after.getValue(to)), last.icon)
            assertEquals(1f, first.detail, 0f); assertEquals(1f, last.detail, 0f)
            assertEquals(1f, first.companionMaterial, 0f); assertEquals(1f, last.companionMaterial, 0f)
        }
    }

    @Test fun colorOsKeepsItsGlassVisibleWhileChangingThePictureAtTheRoundWaist() {
        val plan = RailFusionMotion.plan("music", "calendar", slots(keys, "music", true),
            slots(keys, "calendar", true))!!
        assertTrue(plan.single)
        val a = frame(plan, .49999f)
        val b = frame(plan, .5f)
        assertBox(a.primary, b.primary, .01f)
        assertEquals(46f, b.primary.h, .001f)
        assertEquals(0f, b.iconAlpha, 0f)
        assertTrue(frame(plan, .8f).iconAlpha > .99f)
    }

    @Test fun invalidOrChangedViewportSettlesInsteadOfUsingStaleSeats() {
        assertNull(RailFusionMotion.plan("music", "music", slots(keys, "music"), slots(keys, "music")))
        assertNull(RailFusionMotion.plan("music", "calendar", slots(keys, "music"),
            slots(keys - "notifications", "calendar")))
        val bad = slots(keys, "music").toMutableMap().apply {
            this["calendar"] = RailFusionMotion.Box(0f, Float.NaN, 46f, 46f)
        }
        assertNull(RailFusionMotion.plan("music", "calendar", bad, slots(keys, "calendar")))
    }

    @Test fun overflowWindowChangesCanSettleWithoutDroppingTheRequestedItem() {
        val many = (0..20).map { "live-$it" }
        val from = many.first()
        val to = RailIslandStrip.cycle(many, from, true)!!
        assertEquals(many.last(), to)
        assertNull(RailFusionMotion.plan(from, to, slots(many, from), slots(many, to)))
        assertTrue(to in slots(many, to))
    }

    @Test fun malformedProgressSettlesAndAllIntermediateGeometryStaysFinite() {
        val plan = RailFusionMotion.plan("music", "calendar", slots(keys, "music"), slots(keys, "calendar"))!!
        assertBox(frame(plan, 1f).primary, frame(plan, Float.NaN).primary)
        assertBox(frame(plan, 0f).primary, frame(plan, -1f).primary)
        assertBox(frame(plan, 1f).primary, frame(plan, 2f).primary)
        repeat(1001) { i ->
            val f = frame(plan, i / 1000f)
            assertTrue(f.primary.valid() && f.icon.valid())
            assertTrue(f.detail in 0f..1f && f.companionMaterial in 0f..1f)
        }
    }
}
