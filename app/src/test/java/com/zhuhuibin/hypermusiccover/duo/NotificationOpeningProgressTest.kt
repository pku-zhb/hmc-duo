package com.zhuhuibin.hypermusiccover.duo

import org.junit.Assert.*
import org.junit.Test

class NotificationOpeningProgressTest {
    @Test fun bubbleLeavesDuringTheNativeScrollWhileItsLogicalOwnerStillBlocksUnlock() {
        val exposure = NotificationListExposure()
        val visual = NotificationOpeningProgress()
        exposure.request(0)
        visual.begin(1f, 0, 600)
        assertEquals(.5f, visual.alpha(.5f, 300), .001f)
        assertTrue(exposure.holdsIsland)
        assertEquals(0f, visual.alpha(0f, 600), .001f)
        assertEquals(NotificationListExposure.Action.NONE,
            exposure.observe(500, true, true, "LIST", false, true, false))
        assertTrue(exposure.holdsIsland)
        exposure.observe(600, true, false, "LIST", false, true, false)
        assertEquals(NotificationListExposure.Action.OPENED,
            exposure.observe(728, true, false, "LIST", false, true, false))
        assertFalse(exposure.holdsIsland)
    }

    @Test fun opaqueButHiddenMixedStackRowsUseScrollProgress() {
        val p = NotificationOpeningProgress()
        p.begin(0f, 0, 600)
        assertEquals(1f, p.alpha(0f, 0), .001f)
        assertEquals(.5f, p.alpha(0f, 300), .001f)
        assertEquals(0f, p.alpha(0f, 600), .001f)
    }

    @Test fun nativeSpringAndRetargetDoNotFlashTheDisappearingBubbleBack() {
        val p = NotificationOpeningProgress()
        p.begin(1f, 0, 600)
        assertEquals(.25f, p.alpha(.25f, 450), .001f)
        p.retarget(700)
        assertEquals(.25f, p.alpha(.4f, 400), .001f)
        assertEquals(0f, p.alpha(0f, 700), .001f)
    }

    @Test fun retryStartsFromTheActuallyRestoredPose() {
        val p = NotificationOpeningProgress()
        p.begin(1f, 0, 600)
        p.alpha(0f, 600)
        p.begin(.5f, 200, 600)
        assertEquals(1f, p.alpha(.5f, 200), .001f)
        assertEquals(.5f, p.alpha(.25f, 400), .001f)
    }

    @Test fun unreadableGeometryNeverInventsACompletedVisualHandoff() {
        val p = NotificationOpeningProgress()
        p.begin(null, null, null)
        assertEquals(1f, p.alpha(Float.NaN, null), .001f)
        p.begin(1f, 500, 100)
        assertEquals(.5f, p.alpha(null, 300), .001f)
    }
}
