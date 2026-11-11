package com.os4.musiccover

import org.junit.Assert.*
import org.junit.Test

class RailShortcutActivationTest {
    private class Native : RailShortcutActivation.Target {
        var selected = -1
        var active = false
        var launched = -1
        val calls = mutableListOf<String>()
        override fun initialize(left: Boolean) { selected = if (left) 0 else 1; calls += "down" }
        override fun activate() { check(selected >= 0) { "OEM launch view was not initialized" }; active = true; calls += "active" }
        fun nativeRelease() {
            calls += "up"
            if (active) { check(selected >= 0); launched = selected }
        }
    }
    private fun ready(side: Int): RailShortcutTapState = RailShortcutTapState().apply {
        begin(side, 20f, 30f, 1000, 8f); end(20f, 30f, 1100)
    }
    @Test fun bothShortcutsInitializeTheirNativeSelectionBeforeTheNormalReleaseLaunches() {
        for (side in 0..1) {
            val native = Native()
            assertTrue(RailShortcutActivation.prepare(ready(side), side, 1101, true, false, native))
            native.nativeRelease()
            assertEquals(side, native.launched)
            assertEquals(listOf("down", "active", "up"), native.calls)
        }
    }
    @Test fun cancelledWrongSideUnlockedAndAlreadyActiveGesturesDoNotStartAnotherLaunch() {
        val tap = ready(0)
        val native = Native()
        assertFalse(RailShortcutActivation.prepare(tap, 1, 1101, true, false, native))
        assertFalse(RailShortcutActivation.prepare(tap, 0, 1101, false, false, native))
        assertFalse(RailShortcutActivation.prepare(tap, 0, 1101, true, true, native))
        tap.cancel()
        assertFalse(RailShortcutActivation.prepare(tap, 0, 1101, true, false, native))
        assertTrue(native.calls.isEmpty())
    }
}
