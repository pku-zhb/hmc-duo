package com.zhuhuibin.hypermusiccover.duo

import org.junit.Assert.*
import org.junit.Test

class NotificationGestureIntentTest {
    @Test fun aCapsuleDragDoesNotCountAsANativeGestureWhenTheStackDidNotMove() {
        val gesture = NotificationGestureIntent()
        gesture.begin(100f, 500f, 40, 12f)
        assertFalse(gesture.moved(102f, 300f, 40))
        assertFalse(gesture.moved(102f, 300f, 42))
    }

    @Test fun seekAndHorizontalNotificationSwipesCannotChooseAListFromBackgroundReflow() {
        val gesture = NotificationGestureIntent()
        gesture.begin(100f, 500f, 40, 12f)
        assertFalse(gesture.moved(300f, 520f, 90))
        assertFalse(gesture.moved(101f, 507f, 90))
    }

    @Test fun nativeVerticalScrollingOnAnyCardCanSupplyFreshListIntent() {
        val gesture = NotificationGestureIntent()
        gesture.begin(100f, 500f, 40, 12f)
        assertTrue(gesture.moved(102f, 450f, 90))
        gesture.clear()
        assertFalse(gesture.moved(102f, 430f, 110))
        gesture.begin(200f, 200f, 90, 12f)
        assertTrue(gesture.moved(202f, 250f, 40))
    }

    @Test fun missingOrInvalidCoordinatesCannotCarryIntentIntoTheNextGesture() {
        val gesture = NotificationGestureIntent()
        gesture.begin(100f, 500f, null, 12f)
        assertFalse(gesture.moved(100f, 450f, 40))
        gesture.begin(100f, 500f, 40, 12f)
        assertFalse(gesture.moved(Float.NaN, 450f, 90))
        assertFalse(gesture.moved(100f, 450f, null))
    }

    @Test fun equalScrollTargetsCanStillExposeANormalRowThroughStateOrFade() {
        val gesture = NotificationGestureIntent()
        gesture.begin(100f, 500f, 40, 12f, "NUMBER", 1f)
        assertTrue(gesture.moved(102f, 450f, 40, "STACK", 1f))
        assertTrue(gesture.moved(102f, 450f, 40, "NUMBER", .5f))
        assertFalse(gesture.moved(150f, 502f, 40, "LIST", 0f))
        assertFalse(gesture.needsNativeEvidence(150f, 502f))
    }

    @Test fun anUpwardReleasedUsableRowIsNotCollapsedJustBecauseTargetsAreEqual() {
        val gesture = NotificationGestureIntent()
        gesture.begin(100f, 500f, 40, 12f, "NUMBER", 1f)
        gesture.finish(100f, 450f)
        assertFalse(gesture.collapsedAtRelease(40f, 40f, false, true))
        // The release belongs to one native HANDS_UP, not a later unrelated reflow.
        assertTrue(gesture.collapsedAtRelease(40f, 40f, false, true))
        gesture.begin(100f, 500f, 40, 12f)
        gesture.finish(100f, 550f)
        assertTrue(gesture.collapsedAtRelease(40f, 40f, false, true))
        gesture.begin(100f, 500f, 40, 12f)
        gesture.finish(100f, 450f)
        assertTrue(gesture.collapsedAtRelease(40f, 40f, false, false))
    }

    @Test fun explicitCollapseConsumesTheSameDragUntilANewDownBegins() {
        val gesture = NotificationGestureIntent()
        gesture.begin(100f, 500f, 40, 12f, "NUMBER", 1f)
        assertTrue(gesture.moved(100f, 450f, 60, "LIST", 0f))
        gesture.clear()
        assertFalse(gesture.moved(100f, 400f, 80, "LIST", 0f))
        gesture.finish(100f, 400f)
        assertTrue(gesture.collapsedAtRelease(40f, 40f, false, true))
        gesture.begin(100f, 500f, 40, 12f, "NUMBER", 1f)
        assertTrue(gesture.moved(100f, 450f, 40, "LIST", 0f))
    }

    @Test fun equalTargetNativeReversalKeepsAnOpeningThenHonorsTheNextExplicitCollapse() {
        val exposure = NotificationListExposure()
        val gesture = NotificationGestureIntent()
        repeat(10) { turn ->
            val now = turn * 1000L
            exposure.request(now)
            gesture.begin(100f, 500f, 40, 12f, "NUMBER", 1f)
            if (gesture.moved(100f, 450f, 40, "STACK", 0f)) exposure.interacted()
            gesture.finish(100f, 450f)
            assertFalse(gesture.collapsedAtRelease(40f, 40f, false, true))
            assertEquals(NotificationListExposure.Action.NONE,
                exposure.observe(now + 100, true, false, "STACK", false, true, false))
            assertEquals(NotificationListExposure.Action.OPENED,
                exposure.observe(now + 228, true, false, "STACK", false, true, false))
            exposure.request(now + 300)
            gesture.begin(100f, 500f, 40, 12f, "STACK", 0f)
            gesture.finish(100f, 550f)
            assertTrue(gesture.collapsedAtRelease(40f, 40f, false, true))
            exposure.cancelOpen()
            gesture.clear()
            assertEquals(NotificationListExposure.Action.NONE,
                exposure.observe(now + 500, true, false, "LIST", false, true, true))
            assertEquals(NotificationListExposure.Action.NONE,
                exposure.observe(now + 700, true, false, "LIST", false, true, true))
            assertFalse(exposure.opening)
        }
    }
}
