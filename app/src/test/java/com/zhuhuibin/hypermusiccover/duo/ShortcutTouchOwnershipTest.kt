package com.zhuhuibin.hypermusiccover.duo

import org.junit.Assert.*
import org.junit.Test

class ShortcutTouchOwnershipTest {
    @Test fun aBottomContentStreamCannotBecomeAShortcutAfterMovingOverAnIcon() {
        val owner = ShortcutTouchOwnership()
        owner.start(10L, false)
        repeat(10) { assertFalse(owner.allows(10L)) }
        owner.start(20L, true)
        repeat(10) { assertTrue(owner.allows(20L)) }
        owner.start(30L, false)
        assertFalse(owner.allows(30L))
    }

    @Test fun unobservedAndReplacementStreamsKeepNativeHandling() {
        val owner = ShortcutTouchOwnership()
        assertTrue(owner.allows(10L))
        owner.start(10L, false)
        assertTrue(owner.allows(20L))
    }
}
