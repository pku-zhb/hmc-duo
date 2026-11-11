package com.zhuhuibin.hypermusiccover.duo

import org.junit.Assert.*
import org.junit.Test

class NativeMediaControlPolicyTest {
    @Test fun realSeekBarsAndTheOemProgressWrapperKeepTheirGestures() {
        assertTrue(NativeMediaControlPolicy.owns(null, true, false))
        assertTrue(NativeMediaControlPolicy.owns("media_progress_bar", false, false))
        assertTrue(NativeMediaControlPolicy.owns("custom_seek", true, true))
    }
    @Test fun playbackControlsKeepTheirGesturesWithoutClaimingTheWholeCard() {
        for (id in listOf("action0", "action4", "actionPlayPause", "actionPrev", "actionNext",
                "media_output_switcher", "media_seamless", "cover_source"))
            assertTrue(NativeMediaControlPolicy.owns(id, false, true))
        for (id in listOf("mi_media_controls", "media_bg", "actions", "album_art", "album_art_image", "title"))
            assertFalse(NativeMediaControlPolicy.owns(id, false, true))
        assertFalse(NativeMediaControlPolicy.owns("action0", false, false))
        assertFalse(NativeMediaControlPolicy.owns("media_seamless", false, false))
        assertFalse(NativeMediaControlPolicy.owns("cover_source", false, false))
        assertTrue(NativeMediaControlPolicy.owns("vendor_source_alias", false, true, true))
        assertFalse(NativeMediaControlPolicy.owns("vendor_output_wave", false, false, true))
    }
    @Test fun hiddenDisabledOrUnlaidControlsDoNotKeepTheirOldTouchArea() {
        assertTrue(NativeMediaControlPolicy.interactive(true, true, 1f, 80, 40))
        assertFalse(NativeMediaControlPolicy.interactive(false, true, 1f, 80, 40))
        assertFalse(NativeMediaControlPolicy.interactive(true, false, 1f, 80, 40))
        assertFalse(NativeMediaControlPolicy.interactive(true, true, 0f, 80, 40))
        assertFalse(NativeMediaControlPolicy.interactive(true, true, Float.NaN, 80, 40))
        assertFalse(NativeMediaControlPolicy.interactive(true, true, 1f, 0, 40))
        assertFalse(NativeMediaControlPolicy.interactive(true, true, 1f, 80, 0))
    }
    @Test fun aHiddenAncestorCannotLeaveVisibleChildrenWithNativeTouchHoles() {
        val hiddenHeader = NativeMediaControlPolicy.effectiveAlpha(1f, 0f, 1f)
        val button = NativeMediaControlPolicy.effectiveAlpha(hiddenHeader, 1f, 1f)
        assertEquals(0f, button, 0f)
        assertFalse(NativeMediaControlPolicy.interactive(true, true, button, 80, 40))
        val hiddenGrandparent = NativeMediaControlPolicy.effectiveAlpha(1f, 1f, 0f)
        assertEquals(0f, NativeMediaControlPolicy.effectiveAlpha(hiddenGrandparent, .5f, 1f), 0f)
    }
    @Test fun nestedFadesUseTheirCombinedVisibilityAndRejectInvalidValues() {
        val parent = NativeMediaControlPolicy.effectiveAlpha(1f, .5f, 1f)
        assertEquals(.25f, NativeMediaControlPolicy.effectiveAlpha(parent, .5f, 1f), .0001f)
        val fading = NativeMediaControlPolicy.effectiveAlpha(.02f, .5f, .5f)
        assertFalse(NativeMediaControlPolicy.interactive(true, true, fading, 80, 40))
        assertEquals(0f, NativeMediaControlPolicy.effectiveAlpha(Float.NaN, 1f, 1f), 0f)
        assertEquals(0f, NativeMediaControlPolicy.effectiveAlpha(1f, 1f, Float.POSITIVE_INFINITY), 0f)
    }
}
