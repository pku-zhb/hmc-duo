package com.zhuhuibin.hypermusiccover.duo

import org.junit.Assert.*
import org.junit.Test

class NotificationBlendPaletteTest {
    private val native = listOf(NotificationBlendColor(0x80336699.toInt(), 3),
        NotificationBlendColor(0xffaabbcc.toInt(), 17))

    @Test fun aFadePreservesRgbAndBlendModeAndScalesNativeAlphaOnce() {
        val faded = fadeNotificationPalette(native, .5f)
        assertEquals(listOf(NotificationBlendColor(0x40336699, 3),
            NotificationBlendColor(0x80aabbcc.toInt(), 17)), faded)
        assertEquals(native, fadeNotificationPalette(native, 1f))
    }

    @Test fun repeatedFramesDoNotCompoundTheirOwnFade() {
        var palette = native
        val lease = NotificationPropertyLease({ palette }, { palette = it }, { it.toList() })
        repeat(8) { lease.set(fadeNotificationPalette(lease.baseline, .5f)) }
        assertEquals(fadeNotificationPalette(native, .5f), palette)
        lease.restore()
        assertEquals(native, palette)
    }

    @Test fun externalPaletteAfterTheLastFrameIsNotOverwrittenAtHandback() {
        var palette = native
        val lease = NotificationPropertyLease({ palette }, { palette = it }, { it.toList() })
        lease.set(fadeNotificationPalette(lease.baseline, .2f))
        val aod = listOf(NotificationBlendColor(0x18112233, 9))
        palette = aod
        lease.restore()
        assertSame(aod, palette)
    }

    @Test fun externalPaletteDuringMorphBecomesTheRestorationTarget() {
        var palette = native
        val lease = NotificationPropertyLease({ palette }, { palette = it }, { it.toList() })
        lease.set(fadeNotificationPalette(lease.baseline, .2f))
        val next = listOf(NotificationBlendColor(0x60445566, 4))
        palette = next
        lease.set(fadeNotificationPalette(lease.baseline, .5f))
        assertEquals(fadeNotificationPalette(next, .5f), palette)
        lease.restore()
        assertEquals(next, palette)
    }

    @Test fun endingTheFadeDoesNotRequireAnAodBinderOrLeaveDelayedWrites() {
        var palette = native
        var writes = 0
        val lease = NotificationPropertyLease({ palette }, { palette = it; writes++ }, { it.toList() })
        lease.set(fadeNotificationPalette(lease.baseline, .1f))
        lease.restore() // OEM AOD animation currently defers material rebinding.
        assertEquals(native, palette)
        val aod = listOf(NotificationBlendColor(0x12223344, 6))
        palette = aod
        lease.restore()
        assertSame(aod, palette)
        assertEquals(2, writes)
    }
}
