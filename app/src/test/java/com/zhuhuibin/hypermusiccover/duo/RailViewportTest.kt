package com.zhuhuibin.hypermusiccover.duo

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class RailViewportTest {
    private val keys = (0..8).map { "item-$it" }
    private fun slots(selected: String, limit: Int) =
        RailIslandStrip.layout(keys, selected, 46f, 370f, 1f, false, limit)!!.slots.associate {
            it.key to RailFusionMotion.Box(0f, it.top, 46f, it.height)
        }
    private fun art(b: RailFusionMotion.Box) = RailFusionMotion.Box(b.x + 8, b.y + 8, 30f, 30f)

    @Test fun defaultsAndImportsKeepTheLimitBetweenTwoAndFive() {
        assertEquals(3, MiniPlayerConfig.railVisibleLimit(JSONObject(MiniPlayerConfig.defaultJson())))
        for (n in listOf(-1, 2, 3, 4, 5, 50)) {
            val config = JSONObject(MiniPlayerConfig.normalizedJson(
                JSONObject().put(MiniPlayerConfig.RAIL_VISIBLE_LIMIT, n).toString()))
            assertEquals(n.coerceIn(2, 5), MiniPlayerConfig.railVisibleLimit(config))
        }
    }

    @Test fun allLimitsFitTheOuterViewportAndReachEveryItemWithoutWrapping() {
        for (limit in 2..5) for (selected in keys) {
            val p = RailIslandStrip.layout(keys, selected, 46f, 370f, 1f, false, limit)!!
            assertEquals(limit, p.slots.size)
            assertEquals(keys.size - limit, p.hiddenAbove + p.hiddenBelow)
            assertEquals(selected, p.slots.single { it.primary }.key)
            assertTrue(p.slots.all { it.top >= 0 && it.top + it.height <= 370.001f })
            assertEquals(keys.subList(p.hiddenAbove, keys.size - p.hiddenBelow), p.slots.map { it.key })
            val h = p.slots.single { it.primary }.height
            assertTrue(h >= 156f)
            if (limit > 3) assertTrue(h < 221f)
            assertNotNull(RightRailLayout.face(46f, h, 1f, true))
        }
    }

    @Test fun crossingEveryViewportBoundaryHasAContinuousReversibleBoundedAnimation() {
        for (limit in 2..5) for (index in 0 until keys.lastIndex) for (up in listOf(false, true)) {
            val from = keys[if (up) index + 1 else index]
            val to = keys[if (up) index else index + 1]
            val before = slots(from, limit); val after = slots(to, limit)
            val motion = RailFusionMotion.plan(from, to, before, after)!!
            fun frame(p: Float) = motion.frame(p, art(before.getValue(from)), art(after.getValue(to)), .62f)
            assertEquals(before.getValue(from), frame(0f).primary)
            assertEquals(after.getValue(to), frame(1f).primary)
            for (i in 0..100) {
                val f = frame(i / 100f)
                assertTrue(f.primary.valid())
                assertTrue(f.primary.y >= -.01f && f.primary.y + f.primary.h <= 370.01f)
                if (motion.windowed) {
                    val active = if (f.incoming) to else from
                    val visible = 1 + f.seats.keys.count { it != active && (f.seatAlpha[it] ?: 1f) > .001f }
                    assertTrue("limit=$limit at=$i visible=$visible", visible <= limit)
                }
                assertEquals(f, frame(i / 100f))
            }
            val a = frame(.49999f); val b = frame(.5f)
            assertEquals(a.primary.y, b.primary.y, .02f)
            assertEquals(a.primary.h, b.primary.h, .02f)
        }
    }
}
