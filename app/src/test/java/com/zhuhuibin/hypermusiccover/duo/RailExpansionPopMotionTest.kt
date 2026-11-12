package com.zhuhuibin.hypermusiccover.duo

import org.junit.Assert.*
import org.junit.Test

class RailExpansionPopMotionTest {
    @Test fun beginsAtTheExactVisiblePoseAndEndsFullyTransparent() {
        assertEquals(RailExpansionPopMotion.Frame(1f, 1f), RailExpansionPopMotion.frame(0f))
        assertEquals(0f, RailExpansionPopMotion.frame(1f).alpha, 0f)
        assertEquals(1.075f, RailExpansionPopMotion.frame(1f).scale, .0001f)
    }

    @Test fun swellsBeforeFadingWithoutShrinkingOrFlashingBack() {
        val first = RailExpansionPopMotion.frame(.1f)
        assertTrue(first.scale > 1f)
        assertEquals(1f, first.alpha, 0f)
        var previous = RailExpansionPopMotion.frame(0f)
        for (step in 1..100) {
            val next = RailExpansionPopMotion.frame(step / 100f)
            assertTrue(next.scale >= previous.scale && next.scale <= 1.076f)
            assertTrue(next.alpha <= previous.alpha && next.alpha >= 0f)
            previous = next
        }
    }

    @Test fun materialAndContentUseOneFrameAtEveryProgress() {
        // A shell and its children share these two view properties; no second fade clock.
        val middle = RailExpansionPopMotion.frame(.5f)
        assertTrue(middle.scale > 1.05f)
        assertTrue(middle.alpha in .3f.. .7f)
        assertTrue(RailExpansionPopMotion.DURATION_MS in 220L..320L)
    }

    @Test fun outOfRangeOrInvalidProgressCannotExposeAnUnboundedSurface() {
        assertEquals(RailExpansionPopMotion.frame(0f), RailExpansionPopMotion.frame(-1f))
        assertEquals(RailExpansionPopMotion.frame(1f), RailExpansionPopMotion.frame(2f))
        assertEquals(0f, RailExpansionPopMotion.frame(Float.NaN).alpha, 0f)
        assertEquals(0f, RailExpansionPopMotion.frame(Float.POSITIVE_INFINITY).alpha, 0f)
    }
}
