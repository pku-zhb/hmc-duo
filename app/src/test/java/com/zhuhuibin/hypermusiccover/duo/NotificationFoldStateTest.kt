package com.zhuhuibin.hypermusiccover.duo

import org.junit.Assert.*
import org.junit.Test

class NotificationFoldStateTest {
    @Test fun transientNativeNumberNeverPublishesAnUnwantedRailBubble() {
        val state = NotificationFoldState()
        state.seed(false, false)
        for (native in listOf(false, true, false, true, false))
            assertFalse(state.representedByIsland(native, true))
        assertTrue(state.representedByIsland(true, false))
        state.choose(true)
        assertFalse(state.representedByIsland(false, true))
        assertTrue(state.representedByIsland(true, true))
        state.choose(false)
        assertFalse(state.representedByIsland(true, true))
    }

    @Test fun uncheckedNotificationsRemainOpenAcrossNativeAodAndLatePipelineRuns() {
        val state = NotificationFoldState()
        state.seed(false, false)
        assertEquals("LIST", state.wakeState())
        assertFalse(state.autoFold(true))
        // Native enterNumState/exitNumState report geometry, never a user presentation choice.
        repeat(5) { state.seed(false, false) }
        assertEquals("LIST", state.wakeState())
        assertFalse(state.autoFold(true))
    }

    @Test fun userCollapseWinsUntilTheNextLockSession() {
        val state = NotificationFoldState()
        state.seed(false, false)
        state.choose(true)
        state.seed(false, false)
        assertEquals("NUMBER", state.wakeState())
        assertTrue(state.autoFold(true))
        assertFalse(state.autoFold(false))
        state.beginSession()
        assertEquals("NUMBER", state.wakeState())
        state.seed(false, false)
        assertEquals("LIST", state.wakeState())
    }

    @Test fun explicitOpenWinsOverCollapsedDefaultsAndEachPresentationKeepsItsChoice() {
        val state = NotificationFoldState()
        state.seed(false, true)
        state.choose(false)
        state.seed(true, true)
        assertTrue(state.collapsed)
        state.seed(false, true)
        assertFalse(state.collapsed)
        state.beginSession()
        state.seed(false, true)
        assertTrue(state.collapsed)
    }
}
