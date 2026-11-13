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

    @Test fun aPreviouslyInitializedMapWithoutItsLiveCardCannotBlockNormalSaving() {
        val s = NavigationPresentationSession()
        // This is the reported state: armed=true, no map page/connection or navigation card.
        // Page connection is deliberately not the authority: the map may be disabled or closed.
        for (landscape in listOf(false, true, false)) {
            assertFalse(s.observe(true, true, false, navigationCardPresent = false))
            assertFalse(s.active) // rememberLockPresentation must remain allowed.
            var captured = false
            s.remember(landscape) { captured = true; baseline }
            assertFalse(captured)
        }
    }

    @Test fun anActualNavigationCardKeepsTheOverrideEphemeralAcrossAodAndDisplays() {
        val s = NavigationPresentationSession()
        s.observe(true, true, false, navigationCardPresent = true)
        s.remember(false) { baseline }
        s.remember(true) { baseline.copy(musicView = MiniPlayerConfig.VIEW_ARTWORK_AND_LYRICS) }
        for (aod in listOf(false, true, false)) {
            assertFalse(s.observe(true, true, aod, navigationCardPresent = true))
            assertTrue(s.active) // The temporary navigation layout cannot overwrite last-lock state.
            s.remember(false) { baseline.copy(collapsed = MiniPlayerConfig.COLLAPSE_ALL) }
        }
        s.observe(false, true, false, navigationCardPresent = true)
        assertEquals(baseline, s.restore(false))
        assertEquals(MiniPlayerConfig.VIEW_ARTWORK_AND_LYRICS, s.restore(true)?.musicView)
    }

    @Test fun removalReleasesAStaleArmedSessionAndRestoresBothExistingSnapshotsOnce() {
        val s = NavigationPresentationSession()
        s.observe(true, true, false, navigationCardPresent = true)
        s.remember(false) { baseline }
        s.remember(true) { baseline.copy(collapsed = MiniPlayerConfig.COLLAPSE_ALL) }
        assertTrue(s.observe(true, true, true, navigationCardPresent = false))
        assertFalse(s.active)
        // Repeated stale readiness must not clear pending restoration on the other display.
        assertFalse(s.observe(true, true, false, navigationCardPresent = false))
        assertEquals(baseline, s.restore(false))
        assertFalse(s.observe(true, false, false, navigationCardPresent = false))
        assertEquals(MiniPlayerConfig.COLLAPSE_ALL, s.restore(true)?.collapsed)
        assertNull(s.restore(false))
        assertNull(s.restore(true))
    }

    @Test fun aNewNavigationAfterStaleReleaseCapturesTheLatestNormalChoice() {
        val s = NavigationPresentationSession()
        s.observe(true, true, false, navigationCardPresent = true)
        s.remember(false) { baseline }
        s.observe(true, true, false, navigationCardPresent = false)
        assertEquals(baseline, s.restore(false))
        val nextNormal = baseline.copy(collapsed = MiniPlayerConfig.COLLAPSE_NOTIFICATIONS,
            musicView = MiniPlayerConfig.VIEW_LYRICS)
        s.observe(true, true, false, navigationCardPresent = true)
        s.remember(false) { nextNormal }
        s.observe(true, true, false, navigationCardPresent = false)
        assertEquals(nextNormal, s.restore(false))
    }

    @Test fun aManuallyCollapsedNavigationIsReopenedAndClockReservedBeforeTheAodCapture() {
        val s = NavigationPresentationSession()
        s.observe(true, true, false, navigationCardPresent = true)
        assertTrue(s.entry.takeFocus("navigation"))
        val order = mutableListOf<String>()
        s.prepareSleep(true, {
            assertTrue(s.entry.takeFocus("navigation"))
            order += "navigation-layout"
        }, { order += "clock-reserved" })
        order += "native-aod-capture"
        assertEquals(listOf("navigation-layout", "clock-reserved", "native-aod-capture"), order)
    }

    @Test fun endingNavigationDuringSleepPreparationCannotReserveItsClock() {
        val s = NavigationPresentationSession()
        s.observe(true, true, false, navigationCardPresent = true)
        var reserved = false
        s.prepareSleep(true, { s.observe(true, true, true, navigationCardPresent = false) }, { reserved = true })
        assertFalse(reserved)
        assertFalse(s.active)
    }

    @Test fun plainAodRetainsTheExistingDeferredPresentationAndNativeClock() {
        val s = NavigationPresentationSession()
        s.observe(true, true, false, navigationCardPresent = true)
        assertTrue(s.entry.takeFocus("navigation"))
        s.prepareSleep(false, { fail("Plain AOD must not synchronously reopen a map") }, { fail("Plain AOD clock stays native") })
        assertTrue(s.entry.takeFocus("navigation"))
    }

    @Test fun readinessWithoutTheLiveCardCannotRegainClockOwnershipAtSleep() {
        val s = NavigationPresentationSession()
        s.observe(true, true, false, navigationCardPresent = false)
        var reserved = false
        s.prepareSleep(true, { s.observe(true, true, true, navigationCardPresent = false) }, { reserved = true })
        assertFalse(reserved)
    }
}
