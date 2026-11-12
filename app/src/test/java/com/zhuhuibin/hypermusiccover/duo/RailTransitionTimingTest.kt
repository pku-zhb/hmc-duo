package com.zhuhuibin.hypermusiccover.duo

import org.junit.Assert.*
import org.junit.Test

class RailTransitionTimingTest {
    @Test fun hyperOsSlowsTheReleasedHalfWithoutChangingColorOs() {
        assertEquals(210L, RailFusionDrag.settleDuration(.5f, 1f))
        assertEquals(260L, RailFusionDrag.settleDuration(.5f, 1f, hyperOs = true))
        assertEquals(320L, RailTransitionTiming.cardExit(true))
        assertEquals(420L, RailTransitionTiming.cardExit(false))
        assertEquals(240L, RailTransitionTiming.minimumBirth(true))
        assertEquals(360L, RailTransitionTiming.minimumBirth(false))
    }

    @Test fun anInstantNativeRemovalStillHasAnIntermediateBirthFrame() {
        val duration = RailTransitionTiming.minimumBirth(false)
        assertEquals(0f, RailTransitionTiming.birthLimit(-1L, duration), 0f)
        assertEquals(.5f, RailTransitionTiming.birthLimit(180L, duration), .001f)
        assertEquals(1f, RailTransitionTiming.birthLimit(500L, duration), 0f)
    }
}
