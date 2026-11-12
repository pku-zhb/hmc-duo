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
        assertEquals(NotificationListExposure.Action.OPENED, e.sample(500, "LIST", visible = true))
        assertFalse(e.holdsIsland)
    }

    @Test fun thirdSwipeFinishesTheNativeIntermediatePileWithOnlyOneClick() {
        val e = NotificationListExposure()
        e.request(0)
        assertEquals(NotificationListExposure.Action.CLICK_LIST, e.sample(200))
        assertTrue(e.holdsIsland)
        assertEquals(NotificationListExposure.Action.NONE, e.sample(300))
        assertEquals(NotificationListExposure.Action.OPENED, e.sample(500, "LIST", true))
        assertFalse(e.opening)
    }

    @Test fun aSingleFullyVisibleNotificationNeedsNoExtraTapOrVibration() {
        val e = NotificationListExposure()
        e.request(0)
        assertEquals(NotificationListExposure.Action.OPENED, e.sample(200, visible = true, wantsList = false))
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
}
