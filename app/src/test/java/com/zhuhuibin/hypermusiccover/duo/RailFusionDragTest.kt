package com.zhuhuibin.hypermusiccover.duo

import org.junit.Assert.*
import org.junit.Test

class RailFusionDragTest {
    @Test fun aStationaryFingerHoldsTheEntireExchangeAtOneFrame() {
        val span = RailFusionDrag.span(221f, 1f)
        val held = RailFusionDrag.progress(0f, -36f, true, span)
        assertTrue(held > 0f && held < .5f)
        repeat(100) { assertEquals(held, RailFusionDrag.progress(0f, -36f, true, span), 0f) }
        assertEquals(.5f, RailFusionDrag.progress(0f, -10000f, true, span), 0f)
    }
    @Test fun pullingBackFollowsTheSameFramesInReverse() {
        val span = RailFusionDrag.span(221f, 1f)
        val out = (0..80).map { RailFusionDrag.progress(0f, -it.toFloat(), true, span) }
        val back = (80 downTo 0).map { RailFusionDrag.progress(0f, -it.toFloat(), true, span) }
        assertEquals(out, back.reversed())
        assertEquals(0f, back.last(), 0f)
        assertFalse(RailFusionDrag.commit(.1f, -10f, 0f, true, 1f, false))
        assertFalse(RailFusionDrag.commit(.5f, -80f, 0f, true, 1f, true))
    }
    @Test fun catchingASettlingExchangeKeepsItsExactPoseWithoutFinishingIt() {
        for (start in listOf(.16f, .5f, .76f)) {
            assertEquals(start, RailFusionDrag.progress(start, 0f, true, 100f), 0f)
            assertTrue(RailFusionDrag.progress(start, 20f, true, 100f) < start)
        }
        assertEquals(.76f, RailFusionDrag.progress(.76f, -100f, true, 100f), 0f)
    }
    @Test fun releaseHonorsDirectionAndCancellation() {
        assertTrue(RailFusionDrag.commit(.4f, -70f, 0f, true, 1f, false))
        assertFalse(RailFusionDrag.commit(.4f, -70f, 700f, true, 1f, false))
        assertTrue(RailFusionDrag.commit(.1f, 20f, 1500f, false, 1f, false))
        assertFalse(RailFusionDrag.commit(.1f, 20f, 1500f, false, 1f, true))
        assertTrue(RailFusionDrag.settleDuration(.48f, 1f) < RailFusionDrag.settleDuration(.1f, 1f))
    }
}
