package com.zhuhuibin.hypermusiccover.duo

import org.junit.Assert.*
import org.junit.Test

class RailExpandGestureTest {
    @Test fun nativeDispatchIsCancelledOnceAndOpeningWaitsForRelease() {
        val g = RailExpandGesture()
        g.start(true)
        assertFalse(g.move(1f, -3f, 8f))
        assertTrue(g.move(3f, -12f, 8f))
        assertFalse(g.move(5f, -60f, 8f))
        assertTrue(g.commit(5f, -60f, 0f, 1f, false))
        assertFalse(g.commit(5f, -60f, 0f, 1f, true))
    }
    @Test fun shortAndReturnedDragsCancelWithoutOpeningOrUnlocking() {
        val g = RailExpandGesture(); g.start(true); g.move(0f, -20f, 8f)
        assertFalse(g.commit(0f, -20f, 0f, 1f, false))
        assertFalse(g.commit(0f, 0f, -2000f, 1f, false))
        g.reset(); assertFalse(g.claimed)
    }
    @Test fun downwardAndNoContentGesturesStayNativeForTheWholeStream() {
        val g = RailExpandGesture()
        g.start(true)
        assertFalse(g.move(0f, 20f, 8f))
        assertFalse(g.move(0f, -100f, 8f))
        g.start(false)
        assertFalse(g.move(0f, -100f, 8f))
    }
    @Test fun aDiagonalInTheGapCannotTurnIntoAnUnlockAfterStartingSideways() {
        val g = RailExpandGesture(); g.start(true)
        assertFalse(g.move(-20f, -2f, 8f))
        assertTrue(g.move(-40f, -12f, 8f))
        assertTrue(g.claimed)
        assertFalse(g.commit(-60f, -30f, -1200f, 1f, false))
        assertTrue(g.commit(-20f, -70f, 0f, 1f, false))
    }
}
