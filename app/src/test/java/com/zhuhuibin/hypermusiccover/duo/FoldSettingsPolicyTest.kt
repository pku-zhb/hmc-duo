package com.zhuhuibin.hypermusiccover.duo

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class FoldSettingsPolicyTest {
    @Test fun importingLegacySwitchesCannotReactivateRetiredBehavior() {
        val retired = listOf(MiniPlayerConfig.ADAPTIVE_WIDTH,
            MiniPlayerConfig.LIST_WITHOUT_PLAYER, MiniPlayerConfig.COMPACT_NOTIFICATIONS)
        val input = JSONObject().put(MiniPlayerConfig.ENABLED, true)
            .put(MiniPlayerConfig.MEDIA_COLLAPSED_DEFAULT, true)
            .put("statusAtDate", true).put("backgroundBlur", true).put("noteTextScroll", true)
        retired.forEach { input.put(it, true) }
        val normalized = MiniPlayerConfig.normalizedJson(input.toString())
        val result = JSONObject(normalized)
        retired.forEach { assertFalse(it, result.getBoolean(it)) }
        assertTrue(result.getBoolean(MiniPlayerConfig.ENABLED))
        assertTrue(MiniPlayerConfig.mediaCollapsedByDefault(normalized))
        for (key in listOf("statusAtDate", "backgroundBlur", "noteTextScroll")) assertFalse(result.has(key))
        assertEquals(normalized, MiniPlayerConfig.normalizedJson(normalized))
    }

    @Test fun fullCoverKeepsItsShapeAndArtworkGeometry() {
        val old = CoverCardStyle(CoverCardStyle.FULL, .8f, .4f, .15f)
        val supported = FoldSettingsPolicy.coverStyle(old)
        assertEquals(CoverCardStyle.FULL, supported.mode)
        assertEquals(old.fill, supported.fill, 0f)
        assertEquals(old.pos, supported.pos, 0f)
        assertEquals(old.corner, supported.corner, 0f)
        assertSame(supported, FoldSettingsPolicy.coverStyle(supported))
        assertEquals(CoverCardStyle.FULL, old.mode)
    }

    @Test fun backgroundChoiceRemainsIndependentFromForegroundShape() {
        assertTrue(FoldSettingsPolicy.wallpaperBackground(true))
        assertFalse(FoldSettingsPolicy.wallpaperBackground(false))
        assertFalse(FoldSettingsPolicy.nativeBackdrop(false))
        assertTrue(FoldSettingsPolicy.nativeBackdrop(true))
    }
}
