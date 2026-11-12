package com.zhuhuibin.hypermusiccover.duo

import org.junit.Assert.*
import org.junit.Test

class NotificationTransitionReadTest {
    @Test fun cachedSceneExitDoesNotQueryTheSystemOrBouncer() {
        for ((goingAway, aod, screenOn) in listOf(
            Triple(true, false, true), Triple(false, true, true), Triple(false, false, false))) {
            assertFalse(NotificationTransitionPolicy.scene(goingAway, aod, screenOn,
                { error("must not query system state") }, { error("must not inspect bouncer") }))
        }
    }

    @Test fun onlyALockedAwakeSceneReadsBouncerVisibility() {
        assertFalse(NotificationTransitionPolicy.scene(false, false, true,
            { false }, { error("unlocked scene must not inspect bouncer") }))
        var reads = 0
        assertFalse(NotificationTransitionPolicy.scene(false, false, true,
            { true }, { reads++; true }))
        assertTrue(NotificationTransitionPolicy.scene(false, false, true,
            { true }, { reads++; false }))
        assertEquals(2, reads)
    }

    @Test fun rejectedOrUnknownRoleDoesNotReadLaterGetters() {
        for (rejected in listOf(true, null)) {
            assertFalse(NotificationTransitionPolicy.ordinary({ rejected },
                { error("heads-up already rejected") }, { error("heads-up already rejected") }))
            assertFalse(NotificationTransitionPolicy.ordinary({ false }, { rejected },
                { error("pinned role already rejected") }))
        }
        val reads = mutableListOf<String>()
        assertTrue(NotificationTransitionPolicy.ordinary(
            { reads += "headsUp"; false }, { reads += "pinned"; false }, { reads += "leaving"; false }))
        assertEquals(listOf("headsUp", "pinned", "leaving"), reads)
    }
}
