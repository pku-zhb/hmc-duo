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

    @Test fun allEightCategorySelectionsRoundTripIncludingNoneAndAll() {
        for (mask in 0..7) {
            val cfg = JSONObject(MiniPlayerConfig.normalizedJson(JSONObject()
                .put("enabled", true).put("collapsedCategories", mask).put("startupMusicView", 1).toString()))
            assertEquals(mask, MiniPlayerConfig.collapsedCategories(cfg))
            assertEquals(mask and 1 == 0, MiniPlayerConfig.automaticCoverAllowed(cfg.toString()))
            assertEquals(cfg.toString(), MiniPlayerConfig.normalizedJson(cfg.toString()))
        }
    }
    @Test fun legacyLayoutsMigrateWithoutLosingTheirExpandedMusicPage() {
        for ((mode, mask) in listOf(0 to 7, 1 to 0, 2 to 6)) {
            val cfg = JSONObject(MiniPlayerConfig.normalizedJson("""{"startupMode":$mode,"startupMusicView":2}"""))
            assertEquals(mask, MiniPlayerConfig.collapsedCategories(cfg))
            assertEquals(if (mode == 1) 0 else 2, MiniPlayerConfig.startupMusicView(cfg))
        }
        val legacy = JSONObject(MiniPlayerConfig.normalizedJson("""{"mediaCollapsedDefault":true}"""))
        assertEquals(7, MiniPlayerConfig.collapsedCategories(legacy))
    }
    @Test fun rememberedModeDoesNotRunTheCustomAutomaticCoverChoice() {
        val cfg = """{"enabled":true,"keepLastLockscreen":true,"collapsedCategories":0,"startupMusicView":2}"""
        assertFalse(MiniPlayerConfig.automaticCoverAllowed(cfg))
        assertTrue(MiniPlayerConfig.keepLastLockscreen(JSONObject(MiniPlayerConfig.normalizedJson(cfg))))
    }
    @Test fun snapshotsPreserveIndividualLiveCardsAndUseTheCategoryForNewCards() {
        val original = LockscreenSnapshot(6, 2, setOf("delivery", "calendar"), setOf("delivery"), "calendar")
        val copy = LockscreenSnapshot.decode(original.encode())!!
        assertEquals(original, copy)
        assertFalse(copy.collapseFocus("delivery"))
        assertTrue(copy.collapseFocus("calendar"))
        assertTrue(copy.collapseFocus("new-timer"))
        assertFalse(copy.copy(collapsed = 0).collapseFocus("new-timer"))
        assertNull(LockscreenSnapshot.decode("corrupt"))
    }
    @Test fun aLockSessionFreezesItsRestoreSnapshotAndClaimsLateLiveCardsOnce() {
        val state = LockscreenEntryState()
        val first = LockscreenSnapshot(4, 0)
        val later = LockscreenSnapshot(7, 2)
        assertEquals(first, state.snapshot(false) { first })
        assertEquals(first, state.snapshot(false) { later })
        assertTrue(state.takeFocus("calendar")); assertFalse(state.takeFocus("calendar"))
        assertTrue(state.takeFocus("delivery")); assertTrue(state.takeFocus("calendar", true))
        assertFalse(state.takeSelection(false, false)); assertTrue(state.takeSelection(true, false))
        assertFalse(state.takeSelection(true, false))
        state.arm()
        assertEquals(later, state.snapshot(false) { later })
        assertTrue(state.takeFocus("calendar"))
    }
    @Test fun retiredDateStatusCannotBeReactivatedByAnImportedBackup() {
        val cfg = JSONObject(MiniPlayerConfig.normalizedJson("""{"statusAtDate":true}"""))
        assertFalse(cfg.has("statusAtDate"))
    }

    @Test fun notificationVisibilityOptionsAreIndependentAndSurviveSettingsRoundTrips() {
        val defaults = JSONObject(MiniPlayerConfig.defaultJson())
        assertFalse(defaults.getBoolean(MiniPlayerConfig.SHOW_ALL_LOCKSCREEN_NOTIFICATIONS))
        assertFalse(defaults.getBoolean(MiniPlayerConfig.RETAIN_LOCKSCREEN_NOTIFICATIONS))
        for (show in listOf(false, true)) for (retain in listOf(false, true)) {
            val raw = JSONObject().put(MiniPlayerConfig.ENABLED, false)
                .put(MiniPlayerConfig.SHOW_ALL_LOCKSCREEN_NOTIFICATIONS, show)
                .put(MiniPlayerConfig.RETAIN_LOCKSCREEN_NOTIFICATIONS, retain).toString()
            val first = MiniPlayerConfig.normalizedJson(raw)
            val second = JSONObject(MiniPlayerConfig.normalizedJson(first))
            assertEquals(show, second.getBoolean(MiniPlayerConfig.SHOW_ALL_LOCKSCREEN_NOTIFICATIONS))
            assertEquals(retain, second.getBoolean(MiniPlayerConfig.RETAIN_LOCKSCREEN_NOTIFICATIONS))
        }
    }

}
