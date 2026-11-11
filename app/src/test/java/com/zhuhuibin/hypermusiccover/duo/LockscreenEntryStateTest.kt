package com.zhuhuibin.hypermusiccover.duo

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class LockscreenEntryStateTest {
    @Test fun lateMusicAndNotificationsAreClaimedIndependentlyOncePerLockSession() {
        val state = LockscreenEntryState()
        assertFalse(state.takeMusic(false))
        assertTrue(state.takeNotifications(true))
        assertFalse(state.takeNotifications(true))
        assertTrue(state.takeMusic(true))
        // Repeated refreshes, AOD wakes and display handoffs do not re-arm either choice.
        repeat(4) { assertFalse(state.takeMusic(true)); assertFalse(state.takeNotifications(true)) }
        state.arm()
        assertTrue(state.takeMusic(true))
        assertTrue(state.takeNotifications(true))
    }
    @Test fun legacyCollapsedSettingMigratesButExplicitStartupModeWins() {
        val old = JSONObject(MiniPlayerConfig.normalizedJson("""{"enabled":true,"mediaCollapsedDefault":true}"""))
        assertEquals(MiniPlayerConfig.START_COLLAPSED, MiniPlayerConfig.startupMode(old))
        val new = JSONObject(MiniPlayerConfig.normalizedJson("""{"enabled":true,"mediaCollapsedDefault":true,"startupMode":1}"""))
        assertEquals(MiniPlayerConfig.START_EXPANDED, MiniPlayerConfig.startupMode(new))
        assertFalse(new.getBoolean(MiniPlayerConfig.MEDIA_COLLAPSED_DEFAULT))
    }
    @Test fun automaticCoverOnlyBelongsToArtworkAndLyricsStartupViews() {
        for (mode in 0..2) for (view in 0..2) {
            val raw = JSONObject().put("enabled", true).put("startupMode", mode).put("startupMusicView", view).toString()
            assertEquals("$mode/$view", mode == 2 && view != 0, MiniPlayerConfig.automaticCoverAllowed(raw))
        }
        assertTrue(MiniPlayerConfig.automaticCoverAllowed("""{"enabled":false}"""))
    }
    @Test fun retiredFullSizeAndOldImportedValuesCannotBreakTheNewSelectors() {
        val config = JSONObject(MiniPlayerConfig.normalizedJson("""{"colorOsFullSize":true,"startupMode":99,"startupMusicView":-9}"""))
        assertFalse(config.getBoolean(MiniPlayerConfig.COLOROS_FULL_SIZE))
        assertEquals(2, MiniPlayerConfig.startupMode(config))
        assertEquals(0, MiniPlayerConfig.startupMusicView(config))
        assertEquals(config.toString(), MiniPlayerConfig.normalizedJson(config.toString()))
    }
    @Test fun landscapeHasIndependentDefaultsAndClaimsButAodDoesNotReapplyThem() {
        val cfg = JSONObject(MiniPlayerConfig.normalizedJson("""{"enabled":true,"startupMode":0,"startupMusicView":1}"""))
        assertEquals(0, MiniPlayerConfig.startupMode(cfg, false))
        assertEquals(1, MiniPlayerConfig.startupMode(cfg, true))
        assertEquals(2, MiniPlayerConfig.startupMusicView(cfg, true))
        assertTrue(MiniPlayerConfig.automaticCoverAllowed(cfg.toString(), true))
        assertFalse(MiniPlayerConfig.automaticCoverAllowed(cfg.toString(), false))
        val state = LockscreenEntryState()
        assertTrue(state.takeMusic(true, false)); assertTrue(state.takeNotifications(true, false))
        assertTrue(state.takeMusic(true, true)); assertTrue(state.takeNotifications(true, true))
        assertFalse(state.takeMusic(true, true)); assertFalse(state.takeNotifications(true, true))
        assertFalse(state.takeMusic(true, false))
        state.arm()
        assertTrue(state.takeMusic(true, true))
    }

}
