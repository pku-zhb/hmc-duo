package com.zhuhuibin.hypermusiccover.duo

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class FoldSettingsPolicyTest {
    @Test fun importingLegacySwitchesCannotReactivateRetiredBehavior() {
        val retired = listOf(MiniPlayerConfig.ADAPTIVE_WIDTH,
            MiniPlayerConfig.LIST_WITHOUT_PLAYER, MiniPlayerConfig.COMPACT_NOTIFICATIONS,
            MiniPlayerConfig.BACKGROUND_BLUR, MiniPlayerConfig.NOTE_TEXT_SCROLL)
        val input = JSONObject().put(MiniPlayerConfig.ENABLED, true)
            .put(MiniPlayerConfig.MEDIA_COLLAPSED_DEFAULT, true)
            .put(MiniPlayerConfig.STATUS_AT_DATE, false)
        retired.forEach { input.put(it, true) }
        val normalized = MiniPlayerConfig.normalizedJson(input.toString())
        val result = JSONObject(normalized)
        retired.forEach { assertFalse(it, result.getBoolean(it)) }
        assertTrue(result.getBoolean(MiniPlayerConfig.ENABLED))
        assertTrue(MiniPlayerConfig.mediaCollapsedByDefault(normalized))
        assertFalse(result.getBoolean(MiniPlayerConfig.STATUS_AT_DATE))
        assertEquals(normalized, MiniPlayerConfig.normalizedJson(normalized))
    }

    @Test fun pendingFullCoverFallsBackWithoutLosingArtworkGeometry() {
        val old = CoverCardStyle(CoverCardStyle.FULL, .8f, .4f, .15f)
        val supported = FoldSettingsPolicy.coverStyle(old)
        assertEquals(CoverCardStyle.CARD, supported.mode)
        assertEquals(old.fill, supported.fill, 0f)
        assertEquals(old.pos, supported.pos, 0f)
        assertEquals(old.corner, supported.corner, 0f)
        assertSame(supported, FoldSettingsPolicy.coverStyle(supported))
        // The renderer's original FULL representation remains available for future repair.
        assertEquals(CoverCardStyle.FULL, old.mode)
    }

    @Test fun pendingBackgroundCannotResumeThroughImportOrTurningOffTheRail() {
        assertFalse(FoldSettingsPolicy.wallpaperBackground(true))
        assertFalse(FoldSettingsPolicy.wallpaperBackground(false))
        assertTrue(FoldSettingsPolicy.nativeBackdrop(false))
        assertTrue(FoldSettingsPolicy.nativeBackdrop(true))
    }
}
