package com.zhuhuibin.hypermusiccover.duo

import org.junit.Assert.*
import org.junit.Test

class NotificationTransitionPolicyTest {
    @Test fun headsUpPinnedLeavingAndUnknownRowsNeverEnterOrdinaryMorphs() {
        val flags = listOf(false, true, null)
        for (headsUp in flags) for (pinned in flags) for (leaving in flags) {
            assertEquals("headsUp=$headsUp pinned=$pinned leaving=$leaving",
                headsUp == false && pinned == false && leaving == false,
                NotificationTransitionPolicy.ordinary(headsUp, pinned, leaving))
        }
    }

    @Test fun unlockAodAndSleepingEachEndAnOtherwiseValidTransition() {
        for (locked in listOf(false, true)) for (away in listOf(false, true))
            for (aod in listOf(false, true)) for (screenOn in listOf(false, true)) {
                assertEquals(locked && !away && !aod && screenOn,
                    NotificationTransitionPolicy.scene(locked, away, aod, screenOn))
            }
    }
}
