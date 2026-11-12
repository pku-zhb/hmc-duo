package com.zhuhuibin.hypermusiccover.duo

import org.junit.Assert.*
import org.junit.Test

class NotificationListExposureTest {
    private fun NotificationListExposure.sample(time: Long, state: String = "STACK", visible: Boolean = false,
        wantsList: Boolean? = true, busy: Boolean = false, active: Boolean = true, collapsed: Boolean = false) =
        observe(time, active, busy, state, wantsList, visible, collapsed)

    @Test fun aScrollRequestDoesNotRemoveTheBubbleBeforeOrdinaryRowsAreVisible() {
        val e = NotificationListExposure()
        e.request(100)
        assertTrue(e.holdsIsland)
        assertEquals(NotificationListExposure.Action.NONE, e.sample(200, "NUMBER", busy = true))
        assertEquals(NotificationListExposure.Action.NONE, e.sample(400, "LIST", visible = false))
        assertTrue(e.holdsIsland)
        assertEquals(NotificationListExposure.Action.NONE, e.sample(500, "LIST", visible = true))
        assertEquals(NotificationListExposure.Action.OPENED, e.sample(628, "LIST", visible = true))
        assertFalse(e.holdsIsland)
    }

    @Test fun thirdSwipeRetargetsTheExplicitListAfterFocusRowsFinishLayout() {
        val e = NotificationListExposure()
        e.request(0)
        assertEquals(NotificationListExposure.Action.RETARGET_LIST, e.sample(200))
        assertTrue(e.holdsIsland)
        assertEquals(NotificationListExposure.Action.NONE, e.sample(300))
        assertEquals(NotificationListExposure.Action.NONE, e.sample(500, "LIST", true))
        assertEquals(NotificationListExposure.Action.OPENED, e.sample(628, "LIST", true))
        assertFalse(e.opening)
    }

    @Test fun aSingleFullyVisibleNotificationNeedsNoExtraTapOrVibration() {
        val e = NotificationListExposure()
        e.request(0)
        assertEquals(NotificationListExposure.Action.NONE, e.sample(200, visible = true, wantsList = false))
        assertEquals(NotificationListExposure.Action.OPENED, e.sample(328, visible = true, wantsList = false))
        assertFalse(e.holdsIsland)
    }

    @Test fun aSettledPartialPileReturnsToAnIslandButNotDuringNativeScrolling() {
        val e = NotificationListExposure()
        assertEquals(NotificationListExposure.Action.NONE, e.sample(100, busy = true))
        assertEquals(NotificationListExposure.Action.NONE, e.sample(900, busy = true))
        assertEquals(NotificationListExposure.Action.NONE, e.sample(1000))
        assertEquals(NotificationListExposure.Action.NONE, e.sample(1399))
        assertEquals(NotificationListExposure.Action.CAPTURE, e.sample(1400))
        assertTrue(e.holdsIsland)
        assertEquals(NotificationListExposure.Action.NONE, e.sample(1900))
    }

    @Test fun aFailedOpenHasBoundedRetriesAndKeepsItsRepresentative() {
        val e = NotificationListExposure()
        e.request(0)
        assertEquals(NotificationListExposure.Action.RETARGET_LIST, e.sample(200, "NUMBER"))
        assertEquals(NotificationListExposure.Action.RETRY_LIST, e.sample(700, "NUMBER"))
        assertEquals(NotificationListExposure.Action.RETRY_LIST, e.sample(1400, "NUMBER"))
        assertEquals(NotificationListExposure.Action.NONE, e.sample(2100, "NUMBER"))
        assertEquals(NotificationListExposure.Action.CAPTURE, e.sample(2400, "NUMBER", busy = true))
        assertFalse(e.opening)
        assertTrue(e.holdsIsland)
    }

    @Test fun aodAndUnlockCancelThePendingOpenWithoutChangingTheSavedChoice() {
        val e = NotificationListExposure()
        e.request(0)
        assertEquals(NotificationListExposure.Action.NONE, e.sample(300, active = false))
        assertFalse(e.holdsIsland)
        for (t in listOf(500L, 1500L, 4000L)) {
            assertEquals(NotificationListExposure.Action.NONE, e.sample(t, "NUMBER"))
            assertFalse(e.holdsIsland)
        }
    }

    @Test fun mixedCardsCanRemainNumberAfterDefaultListAndStillNeedExplicitRetargeting() {
        val e = NotificationListExposure()
        e.request(0)
        assertEquals(NotificationListExposure.Action.RETARGET_LIST,
            e.sample(200, "NUMBER", visible = false, wantsList = null))
        assertTrue(e.holdsIsland)
        assertEquals(NotificationListExposure.Action.NONE,
            e.sample(300, "LIST", visible = false, wantsList = false))
        assertTrue(e.holdsIsland)
        assertEquals(NotificationListExposure.Action.NONE,
            e.sample(500, "LIST", visible = true, wantsList = false))
        assertEquals(NotificationListExposure.Action.OPENED,
            e.sample(628, "LIST", visible = true, wantsList = false))
    }

    @Test fun unknownNativeClickPropertyDoesNotLoseAPartiallyVisiblePile() {
        val e = NotificationListExposure()
        assertEquals(NotificationListExposure.Action.NONE,
            e.sample(100, visible = true, wantsList = null))
        assertEquals(NotificationListExposure.Action.CAPTURE,
            e.sample(500, visible = true, wantsList = null))
        assertTrue(e.holdsIsland)
    }

    @Test fun transientVisibleFrameDuringReflowDoesNotCompleteTheOpen() {
        val e = NotificationListExposure()
        e.request(0)
        assertEquals(NotificationListExposure.Action.NONE, e.sample(100, "LIST", true))
        assertEquals(NotificationListExposure.Action.NONE, e.sample(150, "NUMBER", false, busy = true))
        assertTrue(e.holdsIsland)
        assertEquals(NotificationListExposure.Action.NONE, e.sample(400, "LIST", true, busy = true))
        assertEquals(NotificationListExposure.Action.NONE, e.sample(500, "LIST", true))
        assertEquals(NotificationListExposure.Action.OPENED, e.sample(628, "LIST", true))
    }

    @Test fun anotherSwipeCannotSkipAnOpeningListAndUnlockOnItsFirstVisibleFrame() {
        val e = NotificationListExposure()
        e.request(0)
        assertEquals(NotificationListExposure.Action.NONE, e.sample(100, "LIST", true))
        assertFalse(e.listPresented("LIST", false, true))
        assertTrue(e.holdsIsland)
        assertEquals(NotificationListExposure.Action.NONE, e.sample(227, "LIST", true))
        assertFalse(e.listPresented("LIST", false, true))
        assertEquals(NotificationListExposure.Action.OPENED, e.sample(228, "LIST", true))
        assertTrue(e.listPresented("LIST", false, true))
    }

    @Test fun permanentlyBusyVisibleListReachesTheDeadlineWithoutHidingItsRows() {
        val e = NotificationListExposure()
        e.request(0)
        for (time in listOf(100L, 1000L, 2399L))
            assertEquals(NotificationListExposure.Action.NONE,
                e.sample(time, "LIST", visible = true, busy = true))
        assertEquals(NotificationListExposure.Action.RETAIN,
            e.sample(2400, "LIST", visible = true, busy = true))
        assertFalse(e.opening)
        assertTrue(e.captured)
        assertEquals(NotificationListExposure.Action.NONE,
            e.sample(3000, "LIST", visible = true, busy = true))
        e.request(3100)
        assertTrue(e.opening)
        assertFalse(e.captured)
    }

    @Test fun aVisibleFrameFollowedByBusyCannotResetTheOverallOpeningDeadline() {
        val e = NotificationListExposure()
        e.request(0)
        assertEquals(NotificationListExposure.Action.NONE, e.sample(100, "LIST", visible = true))
        assertEquals(NotificationListExposure.Action.NONE,
            e.sample(150, "LIST", visible = true, busy = true))
        assertEquals(NotificationListExposure.Action.RETAIN,
            e.sample(3000, "LIST", visible = true, busy = true))
        assertFalse(e.opening)
        assertFalse(e.listPresented("LIST", false, true))
        assertEquals(NotificationListExposure.Action.NONE,
            e.sample(3100, "LIST", visible = true, busy = false))
        assertTrue(e.listPresented("LIST", false, true))
    }

    @Test fun unreadableNativeTargetKeepsTheBubbleAndNextGestureCanRetry() {
        val e = NotificationListExposure()
        e.request(0)
        e.failedOpen()
        assertTrue(e.holdsIsland)
        assertFalse(e.opening)
        e.request(500)
        assertTrue(e.opening)
        assertFalse(e.captured)
    }
}
