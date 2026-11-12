package com.zhuhuibin.hypermusiccover.duo

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackdropBackupTest {
    @Test fun legacyInactiveBackgroundSwitchesRestoreTheNativeWallpaper() {
        val legacy = JSONObject()
            .put("hidePlayerBackground", false)
            .put("glowDrift", true)
            .put("meshBackdrop", true)
        assertEquals(mapOf(
            "hidePlayerBackground" to true,
            "glowDrift" to false,
            "meshBackdrop" to false,
        ), SettingsBackup.readBackdropSettings(legacy))
    }

    @Test fun evenOneLegacyBackgroundKeyNormalizesTheWholeMode() {
        assertEquals(mapOf(
            "hidePlayerBackground" to true,
            "glowDrift" to false,
            "meshBackdrop" to false,
        ), SettingsBackup.readBackdropSettings(JSONObject().put("meshBackdrop", true)))
    }

    @Test fun appOnlyAndUnrelatedModuleBackupsLeaveTheBackgroundUntouched() {
        for (raw in listOf("{}", "{\"themeMode\":\"Dark\"}", "{\"coverStyle\":1}",
            "{\"backdropSchema\":1}")) {
            assertTrue(SettingsBackup.readBackdropSettings(JSONObject(raw)).isEmpty())
        }
    }

    @Test fun currentSchemaRestoresOnlyValidPresentValues() {
        val current = JSONObject()
            .put("backdropSchema", 1)
            .put("hidePlayerBackground", false)
            .put("glowDrift", "invalid")
            .put("meshBackdrop", true)
        assertEquals(mapOf("hidePlayerBackground" to false, "meshBackdrop" to true),
            SettingsBackup.readBackdropSettings(current))
    }
}
