package com.zhuhuibin.hypermusiccover.duo

import org.junit.Assert.*
import org.junit.Test

class RailSelectionPolicyTest {
    @Test fun returnedMusicTakesTheCapsuleFromThePreviousNotification() {
        assertEquals("music", RailSelectionPolicy.select("music", null, "notification", listOf("music", "notification")))
    }
    @Test fun explicitReturnWinsOverASimultaneousNotificationArrival() {
        assertEquals("music", RailSelectionPolicy.select("music", "new", "old", listOf("music", "old", "new")))
    }
    @Test fun normalArrivalAndMissingSessionStillUseAvailableIslands() {
        assertEquals("new", RailSelectionPolicy.select(null, "new", "old", listOf("old", "new")))
        assertEquals("note", RailSelectionPolicy.select("music", null, "missing", listOf("note")))
        assertNull(RailSelectionPolicy.select("music", null, null, emptyList()))
    }
}
