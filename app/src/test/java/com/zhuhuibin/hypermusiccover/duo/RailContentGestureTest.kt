package com.zhuhuibin.hypermusiccover.duo

import org.junit.Assert.*
import org.junit.Test

class RailContentGestureTest {
    @Test fun aProgressDragKeepsNativeOwnershipDespiteVerticalWobbleAndLeavingTheControl() {
        val g = RailContentGesture()
        g.start(canExpand = true, canCollapse = true, onNativeControl = true)
        for ((x, y) in listOf(45f to -15f, 120f to -75f, 120f to 120f)) {
            assertFalse(g.move(x, y, 8f, true))
            assertTrue(g.nativeControl)
            assertEquals(RailContentGesture.Action.NONE, g.commit(x, y, 3000f, 1f, false))
        }
        assertFalse(g.claimed)
    }

    @Test fun replacementDownReleasesTheOldOwnerAndStartsAnIndependentGesture() {
        val g = RailContentGesture()
        g.start(true, true, false)
        assertTrue(g.move(0f, -20f, 8f, true))
        g.start(true, true, true)
        assertFalse(g.claimed)
        assertTrue(g.nativeControl)
        g.start(false, true, false)
        assertFalse(g.nativeControl)
        assertTrue(g.move(0f, 20f, 8f, true))
        assertEquals(RailContentGesture.Action.COLLAPSE, g.commit(0f, 60f, 0f, 1f, false))
    }

    @Test fun clockAndEmptyContentCanCollapseEvenWhenNoCapsuleRemains() {
        val g = RailContentGesture()
        g.start(canExpand = false, canCollapse = true, onNativeControl = false,
            startsOnScrollableCard = false, atListTop = false)
        assertFalse(g.move(1f, 3f, 8f, false))
        assertTrue(g.move(2f, 20f, 8f, false))
        assertFalse(g.move(2f, 60f, 8f, false))
        assertEquals(RailContentGesture.Action.COLLAPSE, g.commit(2f, 60f, 0f, 1f, false))
    }

    @Test fun scrollingAListFromAwayFromItsTopCannotBecomeACollapseMidGesture() {
        val g = RailContentGesture()
        g.start(false, true, false, startsOnScrollableCard = true, atListTop = false)
        assertFalse(g.move(0f, 40f, 8f, false))
        assertFalse(g.move(0f, 100f, 8f, true))
        assertEquals(RailContentGesture.Action.NONE, g.commit(0f, 100f, 2000f, 1f, false))
    }

    @Test fun shortReversedAndCancelledDownwardDragsDoNotCollapse() {
        val g = RailContentGesture()
        g.start(false, true, false)
        assertTrue(g.move(0f, 20f, 8f, true))
        assertEquals(RailContentGesture.Action.NONE, g.commit(0f, 20f, 0f, 1f, false))
        assertEquals(RailContentGesture.Action.NONE, g.commit(0f, -60f, 2000f, 1f, false))
        assertEquals(RailContentGesture.Action.NONE, g.commit(0f, 60f, 0f, 1f, true))
    }

    @Test fun horizontalNotificationDismissalDoesNotCollapse() {
        val g = RailContentGesture()
        g.start(false, true, false)
        assertFalse(g.move(40f, 10f, 8f, true))
        assertFalse(g.move(40f, 80f, 8f, true))
    }

    @Test fun theAcceptedGapGuardStillClaimsDiagonalUpwardDragsAndCommitsOnlyDeliberateOnes() {
        val g = RailContentGesture()
        g.start(true, true, false)
        assertFalse(g.move(-20f, -2f, 8f, true))
        assertTrue(g.move(-40f, -12f, 8f, true))
        assertEquals(RailContentGesture.Action.NONE, g.commit(-60f, -30f, -1200f, 1f, false))
        assertEquals(RailContentGesture.Action.EXPAND, g.commit(-20f, -70f, 0f, 1f, false))
    }
}
