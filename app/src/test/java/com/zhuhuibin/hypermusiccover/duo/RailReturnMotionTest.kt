package com.zhuhuibin.hypermusiccover.duo

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.hypot

class RailReturnMotionTest {
    private fun slots(keys: List<String>, selected: String) =
        RailIslandStrip.layout(keys, selected, 46f, 460f, 1f, false)!!.slots.associate {
            it.key to RailFusionMotion.Box(100f, it.top, 46f, it.height)
        }
    private fun art(box: RailFusionMotion.Box) = RailFusionMotion.Box(box.x+8, box.y+8, 30f, 30f)
    private fun frame(plan: RailReturnMotion.Plan, p: Float) = plan.frame(p,
        art(plan.before[plan.selected] ?: plan.after.getValue(plan.selected)), art(plan.after.getValue(plan.selected)))

    @Test fun returningCardStartsInsideTheExistingCapsuleAndLandsInItsOwnSeat() {
        val before = slots(listOf("notifications"), "notifications")
        val after = slots(listOf("notifications", "music"), "notifications")
        val plan = RailReturnMotion.plan("music", "notifications", before, after)!!
        val start = frame(plan, 0f)
        val born = start.seats.getValue("music")
        val parent = start.primary
        assertEquals(1f, born.w, 0f)
        assertTrue(born.y >= parent.y && born.y + born.h <= parent.y + parent.h)
        assertEquals(0f, plan.iconAlpha(0f), 0f)
        val end = frame(plan, 1f)
        assertEquals(after, end.seats)
        assertEquals(1f, plan.iconAlpha(1f), 0f)
        assertTrue((1..99).any { frame(plan, it/100f).surface.neck != null })
    }
    @Test fun birthAndSeparationUseTheSameNativeProgressAndCanReverse() {
        val before = slots(listOf("music"), "music")
        val after = slots(listOf("notifications", "music"), "music")
        val plan = RailReturnMotion.plan("notifications", "music", before, after)!!
        val out = (0..100).map { frame(plan, it/100f) }
        val back = (100 downTo 0).map { frame(plan, it/100f) }
        assertEquals(out, back.reversed())
        assertEquals(out[37], frame(plan, .37f))
    }
    @Test fun aNonAdjacentReturnGoesAroundTheUnrelatedCircle() {
        val before = slots(listOf("notifications", "calendar"), "notifications")
        val after = slots(listOf("notifications", "calendar", "music"), "notifications")
        val plan = RailReturnMotion.plan("music", "notifications", before, after)!!
        assertTrue(plan.detour)
        for (i in 0..100) {
            val f = frame(plan, i/100f)
            val bubble = f.seats.getValue("music")
            val other = f.seats.getValue("calendar")
            assertTrue("collision at $i", hypot(bubble.cx-other.cx, bubble.cy-other.cy) + .01f >= (bubble.w+other.w)/2)
        }
        assertEquals(after, frame(plan, 1f).seats)
    }
    @Test fun theFirstReturningContentGrowsFromItsTargetCircleDuringTheCardExit() {
        val after = slots(listOf("music"), "music")
        val plan = RailReturnMotion.plan("music", "music", emptyMap(), after)!!
        assertTrue(plan.single)
        val start = frame(plan, 0f)
        val mid = frame(plan, .5f)
        val end = frame(plan, 1f)
        assertTrue(start.primary.w < 1f && start.primary.h < 1f)
        assertTrue(mid.primary.h > mid.primary.w && mid.primary.h < end.primary.h)
        assertEquals(0f, start.detail, 0f)
        assertEquals(after.getValue("music"), end.primary)
        assertEquals(art(end.primary), end.icon)
        assertEquals(1f, end.detail, 0f)
    }
    @Test fun missingOrExistingSeatCannotStartASecondBirth() {
        val before = slots(listOf("music"), "music")
        assertNull(RailReturnMotion.plan("music", "music", before, before))
        assertNull(RailReturnMotion.plan("calendar", "music", before, before))
    }
}
