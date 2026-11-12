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

    @Test fun sharedListEnumDoesNotHideANormalPileBehindReleasedMediaAndFocusCards() {
        val e = NotificationListExposure()
        assertEquals(NotificationListExposure.Action.NONE, e.sample(0, "LIST", true))
        assertEquals(NotificationListExposure.Action.NONE, e.sample(100, "LIST", false, busy = true))
        assertEquals(NotificationListExposure.Action.NONE, e.sample(500, "LIST", false))
        assertEquals(NotificationListExposure.Action.CAPTURE, e.sample(900, "LIST", false))
        assertTrue(e.holdsIsland)
        assertFalse(e.listPresented("LIST", false, false))
    }

    @Test fun numberWithoutAnotherEnterCallbackStillReturnsPreviouslyOpenNormals() {
        val e = NotificationListExposure()
        e.sample(0, "LIST", true)
        assertEquals(NotificationListExposure.Action.NONE, e.sample(100, "NUMBER"))
        assertEquals(NotificationListExposure.Action.CAPTURE, e.sample(500, "NUMBER"))
        assertTrue(e.holdsIsland)
    }

    @Test fun nativeGestureCanRecoverHiddenNumberEvenWithoutEarlierUsableListSample() {
        val e = NotificationListExposure()
        e.interacted()
        assertEquals(NotificationListExposure.Action.NONE, e.sample(100, "NUMBER"))
        assertEquals(NotificationListExposure.Action.CAPTURE, e.sample(500, "NUMBER"))
    }

    @Test fun settledNumberAlreadyOwnedByTheIslandDoesNotStartAnotherReturn() {
        val e = NotificationListExposure()
        e.interacted()
        for (time in listOf(100L, 500L, 1000L))
            assertEquals(NotificationListExposure.Action.NONE, e.sample(time, "NUMBER", collapsed = true))
    }

    @Test fun aNativeUpwardScrollCanReopenCapturedNotificationsWithoutAnotherBubbleTap() {
        val e = NotificationListExposure()
        e.failedOpen()
        e.interacted()
        assertEquals(NotificationListExposure.Action.NONE,
            e.sample(100, "LIST", true, busy = true, collapsed = true))
        assertEquals(NotificationListExposure.Action.NONE,
            e.sample(500, "LIST", true, collapsed = true))
        assertEquals(NotificationListExposure.Action.OPENED,
            e.sample(628, "LIST", true, collapsed = true))
        assertFalse(e.holdsIsland)
    }

    @Test fun wakeGeometryCannotReplaceTheSavedCollapsedChoice() {
        val e = NotificationListExposure()
        for (time in listOf(100L, 500L, 1000L))
            assertEquals(NotificationListExposure.Action.NONE,
                e.sample(time, "LIST", true, collapsed = true))
    }

    @Test fun reversingAnOpeningWithANativeCollapseCancelsEveryDelayedListRetry() {
        val e = NotificationListExposure()
        e.request(0)
        e.interacted()
        assertEquals(NotificationListExposure.Action.RETARGET_LIST, e.sample(200, "NUMBER"))
        e.cancelOpen() // Latest HANDS_UP target is NUMBER, before native rows finish fading.
        for (time in listOf(300L, 450L, 700L, 1400L, 2600L)) {
            assertEquals(NotificationListExposure.Action.NONE,
                e.sample(time, "LIST", true, collapsed = true))
            assertFalse(e.opening)
        }
        assertEquals(NotificationListExposure.Action.NONE, e.sample(3000, "NUMBER", collapsed = true))
    }

    @Test fun anOldNativeGestureCannotReopenAClosedCategoryWhenAnotherCardChangesTheStack() {
        val e = NotificationListExposure()
        e.interacted()
        e.sample(0, "LIST", true)
        // The settled expanded list consumes its gesture; later geometry is not a new choice.
        assertEquals(NotificationListExposure.Action.NONE, e.sample(500, "LIST", true, collapsed = true))
        assertEquals(NotificationListExposure.Action.NONE, e.sample(1000, "LIST", true, collapsed = true))
        e.interacted()
        assertEquals(NotificationListExposure.Action.NONE, e.sample(1200, "LIST", true, collapsed = true))
        assertEquals(NotificationListExposure.Action.OPENED, e.sample(1328, "LIST", true, collapsed = true))
    }

    @Test fun repeatedInterruptedOpensRemainRetryableWithoutKeepingOlderIntent() {
        val e = NotificationListExposure()
        repeat(20) { turn ->
            val now = turn * 3000L
            e.request(now)
            e.interacted()
            e.sample(now + 100, "LIST", true, busy = true)
            e.cancelOpen()
            assertEquals(NotificationListExposure.Action.NONE,
                e.sample(now + 800, "LIST", true, collapsed = true))
            assertEquals(NotificationListExposure.Action.NONE,
                e.sample(now + 1000, "LIST", true, collapsed = true))
            e.request(now + 1500)
            assertTrue(e.holdsIsland)
            assertEquals(NotificationListExposure.Action.NONE, e.sample(now + 1700, "LIST", true))
            assertEquals(NotificationListExposure.Action.OPENED, e.sample(now + 1828, "LIST", true))
            assertFalse(e.holdsIsland)
        }
    }
}
