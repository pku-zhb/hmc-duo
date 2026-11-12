package com.zhuhuibin.hypermusiccover.duo

import org.junit.Assert.*
import org.junit.Test

class NotificationRowExposureTest {
    @Test fun twentyDpAtTheBottomDoesNotCountAsTheExpandedNotification() {
        assertFalse(NotificationRowExposure.usable(249f, 53f, 1400f, 1f))
        assertTrue(NotificationRowExposure.usable(249f, 249f, 1400f, 1f))
    }

    @Test fun aCompressedNativePileIsNotAListEvenWhenItsFirstCardHasRoom() {
        assertFalse(NotificationRowExposure.usable(227f, 227f, 1400f, .98f))
        assertFalse(NotificationRowExposure.usable(183f, 183f, 1400f, .90f))
        assertTrue(NotificationRowExposure.usable(227f, 227f, 1400f, 1f))
    }

    @Test fun fullBoundsAndViewAlphaCannotOverrideTheNativeNumberFade() {
        assertFalse(NotificationRowExposure.usable(249f, 249f, 1400f, 1f, 0f))
        assertFalse(NotificationRowExposure.usable(249f, 249f, 1400f, 1f, .4f))
        assertFalse(NotificationRowExposure.usable(249f, 249f, 1400f, 1f, Float.NaN))
        assertTrue(NotificationRowExposure.usable(249f, 249f, 1400f, 1f, .99f))
    }

    @Test fun mediaAndLiveCardOcclusionCannotExposeANotificationBehindThem() {
        val shown = NotificationRowExposure.uncoveredHeight(1000f, 1249f,
            listOf(1050f to 1500f, 800f to 1060f))
        assertEquals(0f, shown, .01f)
        assertFalse(NotificationRowExposure.usable(249f, shown, 1400f, 1f))
    }

    @Test fun disjointVisibleSliversDoNotAddUpToAReadableCard() {
        assertEquals(90f, NotificationRowExposure.uncoveredHeight(0f, 200f,
            listOf(90f to 110f)), .01f)
        assertEquals(200f, NotificationRowExposure.uncoveredHeight(0f, 200f,
            listOf(-100f to -10f, 250f to 400f)), .01f)
    }

    @Test fun anOversizedRowCanUseTheViewportWithoutBeingFullyOnScreen() {
        assertTrue(NotificationRowExposure.usable(1600f, 900f, 1000f, 1f))
        assertFalse(NotificationRowExposure.usable(1600f, 700f, 1000f, 1f))
        assertFalse(NotificationRowExposure.usable(0f, 900f, 1000f, 1f))
        assertFalse(NotificationRowExposure.usable(250f, Float.NaN, 1000f, 1f))
    }
}
