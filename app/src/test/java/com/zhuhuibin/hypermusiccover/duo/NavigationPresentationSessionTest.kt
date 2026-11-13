package com.zhuhuibin.hypermusiccover.duo

import org.junit.Assert.*
import org.junit.Test

class NavigationPresentationSessionTest {
    private val baseline = LockscreenSnapshot(0, MiniPlayerConfig.VIEW_ARTWORK,
        setOf("delivery"), setOf("delivery"), "music")

    @Test fun navigationPreservesTheOriginalChoicesAcrossRepeatedSleepEntries() {
        val s = NavigationPresentationSession()
        assertFalse(s.observe(true, true, false))
        s.remember(false) { baseline }
        assertTrue(s.entry.takeMusic(true))
        assertFalse(s.entry.takeMusic(true))
        s.observe(true, true, true)
        assertTrue(s.entry.takeMusic(true))
        s.remember(false) { baseline.copy(collapsed = MiniPlayerConfig.COLLAPSE_ALL) }
        assertTrue(s.observe(false, true, true))
        assertEquals(baseline, s.restore(false))
        assertNull(s.restore(false))
    }

    @Test fun wakingDoesNotOverrideTheUsersLiveChoicesButALaterLockDoes() {
        val s = NavigationPresentationSession()
        s.observe(true, true, true)
        assertTrue(s.entry.takeNotifications(true))
        s.observe(true, true, false)
        assertFalse(s.entry.takeNotifications(true))
        s.observe(true, false, false)
        s.observe(true, true, false)
        assertTrue(s.entry.takeNotifications(true))
    }

    @Test fun delayedCardsAndBothDisplaysRetainIndependentRestoration() {
        val s = NavigationPresentationSession()
        s.observe(true, true, false)
        s.remember(false) { baseline }
        s.remember(true) { baseline.copy(musicView = MiniPlayerConfig.VIEW_ARTWORK_AND_LYRICS) }
        assertFalse(s.entry.takeMusic(false, true))
        assertTrue(s.entry.takeMusic(true, true))
        assertTrue(s.entry.takeMusic(true, false))
        s.observe(false, false, false)
        assertEquals(baseline, s.restore(false))
        assertEquals(MiniPlayerConfig.VIEW_ARTWORK_AND_LYRICS, s.restore(true)?.musicView)
    }

    @Test fun aNewNavigationCannotRestoreThePreviousNavigationsSnapshot() {
        val s = NavigationPresentationSession()
        s.observe(true, true, false); s.remember(false) { baseline }
        s.observe(false, true, false)
        s.observe(true, true, false)
        assertNull(s.restore(false))
        s.remember(false) { baseline.copy(collapsed = MiniPlayerConfig.COLLAPSE_NOTIFICATIONS) }
        s.observe(false, true, false)
        assertEquals(MiniPlayerConfig.COLLAPSE_NOTIFICATIONS, s.restore(false)?.collapsed)
    }
}
