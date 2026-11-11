package com.os4.musiccover

import org.junit.Assert.*
import org.junit.Test

class RailShortcutTapStateTest {
    @Test fun oneTapActivatesOnlyItsNativeShortcutOnce() {
        val tap = RailShortcutTapState()
        tap.begin(1, 20f, 30f, 1000, 8f)
        tap.end(21f, 31f, 1120)
        assertFalse(tap.claim(0, 1121))
        assertTrue(tap.claim(1, 1121))
        assertFalse(tap.claim(1, 1122))
    }
    @Test fun dragLongPressCancellationAndStaleEventsNeverBecomeTaps() {
        val tap = RailShortcutTapState()
        tap.begin(0, 0f, 0f, 1000, 8f); tap.move(20f, 0f); tap.end(0f, 0f, 1100)
        assertFalse(tap.claim(0, 1100))
        tap.begin(0, 0f, 0f, 2000, 8f); tap.end(0f, 0f, 2500)
        assertFalse(tap.claim(0, 2500))
        tap.begin(0, 0f, 0f, 3000, 8f); tap.cancel(); tap.end(0f, 0f, 3100)
        assertFalse(tap.claim(-1, 3100)); assertFalse(tap.claim(0, 3100))
        tap.begin(0, 0f, 0f, 4000, 8f); tap.end(0f, 0f, 4100)
        assertFalse(tap.claim(0, 4300))
    }
}
