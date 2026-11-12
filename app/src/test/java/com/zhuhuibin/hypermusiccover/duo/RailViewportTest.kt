package com.zhuhuibin.hypermusiccover.duo

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class RailViewportTest {
    @Test fun obsoleteCountSettingsCannotLimitTheRailAfterAnUpgrade() {
        for (n in listOf(2, 3, 4, 5)) {
            val config = JSONObject(MiniPlayerConfig.normalizedJson(
                JSONObject().put("railVisibleLimit", n).toString()))
            assertFalse(config.has("railVisibleLimit"))
        }
    }

    @Test fun outerViewportShrinksTheCapsuleBeforeDroppingAnySeat() {
        var previousHeight = 222f
        for (count in 1..7) {
            val keys = (0 until count).map(Int::toString)
            val p = RailIslandStrip.layout(keys, keys.last(), 46f, 370f, 1f, false)!!
            assertEquals(keys, p.slots.map { it.key })
            assertEquals(0, p.hidden)
            val h = p.slots.single { it.primary }.height
            assertTrue(h in 46f..previousHeight)
            previousHeight = h
            if (count > 3) assertTrue(h < 221f)
        }
        assertEquals(58f, previousHeight, .001f)
        val keys = (0..7).map(Int::toString)
        val p = RailIslandStrip.layout(keys, keys.last(), 46f, 370f, 1f, false)!!
        assertEquals(keys.drop(1), p.slots.map { it.key })
        assertTrue(p.slots.all { it.height == 46f })
        assertEquals(1, p.hiddenAbove)
        assertEquals(0, p.hiddenBelow)
    }

    @Test fun automaticCapacityFitsChangingDensityAndAvailableHeight() {
        for (density in listOf(1f, 2.75f, 3f)) for (height in listOf(100f, 220f, 370f, 460f, 800f)) {
            val keys = (0..29).map(Int::toString)
            for (selected in listOf(keys.first(), keys.last())) {
                val p = RailIslandStrip.layout(keys, selected, 46f * density,
                    height * density, density, false)!!
                assertEquals(keys.takeLast(p.slots.size), p.slots.map { it.key })
                assertEquals(1, p.slots.count { it.primary })
                assertTrue(p.slots.all { it.top >= -.01f && it.top + it.height <= height * density + .01f })
                assertEquals(keys.size, p.hiddenAbove + p.slots.size)
                assertEquals(0, p.hiddenBelow)
                p.slots.zipWithNext().forEach { (a, b) -> assertTrue(a.top + a.height < b.top) }
            }
        }
    }

    @Test fun everyVisibleNeighbourHasAContinuousBoundedFusionIncludingCircleOnlyOverflow() {
        for (count in 2..9) {
            val keys = (0 until count).map(Int::toString)
            fun slots(selected: String) = RailIslandStrip.layout(keys, selected, 46f, 370f, 1f, false)!!
                .slots.associate { it.key to RailFusionMotion.Box(0f, it.top, 46f, it.height) }
            val visible = slots(keys.last()).keys.toList()
            for ((from, to) in visible.zipWithNext()) {
                val before = slots(from); val after = slots(to)
                val plan = RailFusionMotion.plan(from, to, before, after)!!
                fun art(b: RailFusionMotion.Box) = RailFusionMotion.Box(b.x + 8, b.y + 8, 30f, 30f)
                fun frame(p: Float) = plan.frame(p, art(before.getValue(from)), art(after.getValue(to)), .62f)
                assertEquals(before.getValue(from), frame(0f).primary)
                assertEquals(after.getValue(to), frame(1f).primary)
                for (i in 0..100) {
                    val f = frame(i / 100f)
                    assertTrue(f.primary.valid())
                    assertTrue(f.primary.y >= -.01f && f.primary.y + f.primary.h <= 370.01f)
                }
                assertEquals(frame(.49999f).primary.y, frame(.5f).primary.y, .02f)
            }
        }
    }
}
