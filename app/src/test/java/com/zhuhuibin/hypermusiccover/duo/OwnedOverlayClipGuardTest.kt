package com.zhuhuibin.hypermusiccover.duo

import org.junit.Assert.*
import org.junit.Test

class OwnedOverlayClipGuardTest {
    private data class Clip(var left: Int, var top: Int, var right: Int, var bottom: Int)
    private class Overlay(var clip: Clip? = null) {
        var alpha = 1f
        var visible = true
        var interactive = true
        var writes = 0
    }
    private val hidden = Clip(-1, -1, -1, -1)
    private fun guard() = OwnedOverlayClipLeases<Overlay, Clip>(hidden,
        read = { it.clip },
        write = { target, clip -> target.clip = clip; target.writes++ },
        copy = { it.copy() })

    @Test fun anOriginallyUnclippedOverlayReturnsToANullClip() {
        val guard = guard()
        val view = Overlay()
        guard.suppress(view)
        assertTrue(guard.hasOverrides)
        assertEquals(hidden, view.clip)
        guard.restoreAll()
        assertNull(view.clip)
        assertFalse(guard.hasOverrides)
    }

    @Test fun originalBoundsAreCopiedRatherThanAliasedToMutableCallerState() {
        val original = Clip(1, 2, 80, 90)
        val view = Overlay(original)
        val guard = guard()
        guard.suppress(view)
        original.bottom = 500
        assertNotSame(hidden, view.clip)
        guard.restoreAll()
        assertEquals(Clip(1, 2, 80, 90), view.clip)
        assertNotSame(original, view.clip)
    }

    @Test fun newAndReplacedOverlayTargetsAreRestoredAfterSeveralBlockedFrames() {
        val guard = guard()
        val old = Overlay(Clip(0, 0, 40, 80))
        val replacement = Overlay()
        val glass = Overlay(Clip(4, 5, 150, 170))
        guard.suppress(old)
        guard.suppress(replacement)
        guard.suppress(glass)
        // The old overlay is pooled, not visited again, but still needs its original clip.
        repeat(4) { guard.suppress(replacement); guard.suppress(glass) }
        assertEquals(1, replacement.writes)
        guard.restoreAll()
        assertEquals(Clip(0, 0, 40, 80), old.clip)
        assertNull(replacement.clip)
        assertEquals(Clip(4, 5, 150, 170), glass.clip)
        assertFalse(guard.hasOverrides)
    }

    @Test fun anAnimationCannotExposePixelsByChangingAlphaVisibilityOrInteraction() {
        val guard = guard()
        val view = Overlay()
        guard.suppress(view)
        view.alpha = .35f
        view.visible = false
        view.interactive = false
        guard.suppress(view)
        assertEquals(hidden, view.clip)
        guard.restoreAll()
        assertEquals(.35f, view.alpha, 0f)
        assertFalse(view.visible)
        assertFalse(view.interactive)
    }

    @Test fun anExternalClipChangeWhileBlockedBecomesTheNewRestorationBaseline() {
        val guard = guard()
        val view = Overlay(Clip(0, 0, 40, 80))
        guard.suppress(view)
        view.clip = Clip(8, 9, 80, 100)
        guard.suppress(view)
        assertEquals(hidden, view.clip)
        guard.restoreAll()
        assertEquals(Clip(8, 9, 80, 100), view.clip)
    }

    @Test fun aClipChangedByAnotherOwnerBeforeReleaseIsNeverOverwritten() {
        val guard = guard()
        val view = Overlay(Clip(0, 0, 40, 80))
        guard.suppress(view)
        val external = Clip(7, 8, 70, 90)
        view.clip = external
        guard.restoreAll()
        assertSame(external, view.clip)
        assertEquals(1, view.writes)
        assertFalse(guard.hasOverrides)
        guard.restoreAll()
        assertSame(external, view.clip)
    }

    @Test fun anExternalRemovalOfTheClipIsPreservedAcrossTheNextBlockAndRelease() {
        val guard = guard()
        val view = Overlay(Clip(0, 0, 40, 80))
        guard.suppress(view)
        view.clip = null
        guard.suppress(view)
        guard.restoreAll()
        assertNull(view.clip)
    }
}
