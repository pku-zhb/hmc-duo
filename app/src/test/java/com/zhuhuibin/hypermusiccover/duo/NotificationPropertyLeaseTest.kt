package com.zhuhuibin.hypermusiccover.duo

import org.junit.Assert.*
import org.junit.Test

class NotificationPropertyLeaseTest {
    @Test fun idleAndRepeatedCleanupNeverWriteToTheNativeProperty() {
        var alpha = .8f
        var writes = 0
        val lease = NotificationPropertyLease({ alpha }, { alpha = it; writes++ })
        lease.restore()
        lease.set(.8f)
        lease.restore()
        assertEquals(0, writes)
        lease.set(.2f)
        lease.restore()
        lease.restore()
        assertEquals(.8f, alpha, 0f)
        assertEquals(2, writes)
    }

    @Test fun nativeAlphaWrittenDuringMorphBecomesTheNewBaseline() {
        var alpha = 1f
        val lease = NotificationPropertyLease({ alpha }, { alpha = it })
        lease.set(.3f)
        alpha = .65f
        lease.set(.5f)
        lease.restore()
        assertEquals(.65f, alpha, 0f)
    }

    @Test fun nativeWriteAfterOurLastFrameSurvivesCleanup() {
        var drawableAlpha = 255
        val lease = NotificationPropertyLease({ drawableAlpha }, { drawableAlpha = it })
        lease.set(60)
        drawableAlpha = 90
        lease.restore()
        assertEquals(90, drawableAlpha)
    }

    @Test fun independentlyChangedOutlineAndClipDoNotShareOwnership() {
        var outline: Any? = Any()
        val nativeOutline = outline
        var clip = false
        val outlineLease = NotificationPropertyLease({ outline }, { outline = it })
        val clipLease = NotificationPropertyLease({ clip }, { clip = it })
        outlineLease.set(Any())
        clipLease.set(true)
        val nextOutline = Any()
        outline = nextOutline
        clipLease.restore()
        outlineLease.restore()
        assertSame(nextOutline, outline)
        assertNotSame(nativeOutline, outline)
        assertFalse(clip)
    }

    private data class Bounds(var bottom: Int)

    @Test fun mutableClipCopiesAndNullRestorationDoNotAliasTheFrame() {
        var clip: Bounds? = null
        val lease = NotificationPropertyLease({ clip }, { clip = it }, { it?.copy() })
        val frame = Bounds(20)
        lease.set(frame)
        frame.bottom = 40
        assertEquals(Bounds(20), clip)
        lease.restore()
        assertNull(clip)
        clip = Bounds(40)
        val second = NotificationPropertyLease({ clip }, { clip = it }, { it?.copy() })
        second.set(null)
        second.restore()
        assertEquals(Bounds(40), clip)
    }

    @Test fun failedSetterDoesNotGrantPermissionToRestore() {
        var alpha = 1f
        val lease = NotificationPropertyLease({ alpha }, { _: Float -> error("unsupported setter") })
        runCatching { lease.set(.2f) }
        alpha = .4f
        lease.restore()
        assertEquals(.4f, alpha, 0f)
    }
}
