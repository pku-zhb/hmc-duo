package com.os4.musiccover

import org.junit.Assert.*
import org.junit.Test

class RailDismissOrderTest {
    @Test fun aLonePlayerCanAlwaysReturnToTheRail() {
        assertEquals("music", RailDismissOrder().next(listOf("music"), "music"))
    }
    @Test fun openingNotificationsKeepsMusicAndClosesOneCardPerGesture() {
        val order = RailDismissOrder()
        val available = mutableListOf("music", "notifications", "delivery")
        available.forEach(order::opened)
        for (expected in listOf("delivery", "notifications", "music")) {
            val gesture = MiniPlayerCollapseGesture()
            gesture.start(true)
            assertTrue(gesture.move(0f, 80f, 10f, true))
            val chosen = order.next(available, "music")
            assertEquals(expected, chosen)
            available.remove(chosen); order.closed(chosen)
            assertFalse(gesture.move(0f, 160f, 10f, true))
        }
        assertNull(order.next(available, "music"))
    }
    @Test fun expiredActivityDoesNotBlockTheRemainingCards() {
        val order = RailDismissOrder()
        order.opened("expired"); order.opened("music")
        assertEquals("notifications", order.next(listOf("music", "notifications"), "music"))
    }
    @Test fun aSmallDragDoesNotConsumeTheNextCollapseStep() {
        val gesture = MiniPlayerCollapseGesture()
        gesture.start(true)
        assertFalse(gesture.move(0f, 40f, 48f, true))
        assertTrue(gesture.move(0f, 80f, 48f, true))
        assertFalse(gesture.move(0f, 160f, 48f, true))
    }
    @Test fun reopeningMusicWithoutThePillEntryCanAlwaysCollapseAgain() {
        val order = RailDismissOrder()
        // A retained cover/native selection may keep music available between callbacks.
        // Reopening through lyrics, startup or media controls never calls opened().
        val available = listOf("music")
        val oldClosingMarker = setOf("music")
        repeat(5) {
            val gesture = MiniPlayerCollapseGesture()
            gesture.start(true)
            assertTrue(gesture.move(0f, 100f, 48f, true))
            assertEquals("music", order.next(available, "music", oldClosingMarker, false))
            order.closed("music")
        }
    }
    @Test fun onlyTheLiveMediaExitBlocksMusicWhileOtherCardsRemainIndependent() {
        val order = RailDismissOrder()
        val closing = setOf("music", "delivery")
        assertNull(order.next(listOf("music"), "music", closing, true))
        assertEquals("notifications", order.next(listOf("music", "delivery", "notifications"), "music", closing, true))
        // Normal completion, cancellation and zero-duration exit all release the guard.
        assertEquals("music", order.next(listOf("music"), "music", closing, false))
    }

}
