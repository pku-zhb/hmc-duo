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
    @Test fun downwardHorizontalAndNoContentGesturesStayNativeForTheWholeStream() {
        val g = RailExpandGesture()
        for (delta in listOf(20f to 2f, 0f to 20f)) {
            g.start(true)
            assertFalse(g.move(delta.first, delta.second, 8f))
            assertFalse(g.move(0f, -100f, 8f))
        }
        g.start(false)
        assertFalse(g.move(0f, -100f, 8f))
    }
}
