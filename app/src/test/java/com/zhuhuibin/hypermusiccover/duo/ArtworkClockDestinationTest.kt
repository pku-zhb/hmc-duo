package com.zhuhuibin.hypermusiccover.duo

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ArtworkClockDestinationTest {
    @Test fun legacyRowSwitchMigratesToTheSidebarTime() {
        assertEquals(ArtworkClockDestination.STATUS,
            SettingsBackup.readArtworkClockDestination(JSONObject("""{"artworkClockRow":true}""")))
        assertEquals(ArtworkClockDestination.STATUS,
            SettingsBackup.readArtworkClockDestination(JSONObject("""{"artworkClockRow":false}""")))
    }

    @Test fun everyLegacyDestinationMigratesToTheFixedSidebarTime() {
        for (mode in 0..2) for (legacy in listOf(true, false)) {
            val json = JSONObject().put("artworkClockRow", legacy).put("artworkClockDestination", mode)
            assertEquals(ArtworkClockDestination.STATUS, SettingsBackup.readArtworkClockDestination(json))
        }
    }

    @Test fun appOnlyAndMalformedBackupsDoNotInventModuleChoices() {
        assertNull(SettingsBackup.readArtworkClockDestination(JSONObject()))
        assertNull(SettingsBackup.readArtworkClockDestination(JSONObject("""{"artworkClockRow":"bad"}""")))
        assertNull(SettingsBackup.readArtworkClockDestination(JSONObject("""{"artworkClockDestination":"bad"}""")))
        assertEquals(ArtworkClockDestination.STATUS,
            SettingsBackup.readArtworkClockDestination(JSONObject("""{"artworkClockDestination":99}""")))
    }

    @Test fun closingSceneKeepsItsFirstFrameAndReversesFromTheCurrentOpacity() {
        val firstExit = ArtworkClockDestination.progress(1f, false, 0f)
        assertEquals(1f, firstExit, 0f)
        assertEquals(1f, ArtworkClockDestination.fade(firstExit), 0f)
        val interrupted = ArtworkClockDestination.progress(1f, false, .4f)
        assertEquals(interrupted, ArtworkClockDestination.progress(interrupted, true, 0f), 0f)
        assertEquals(1f, ArtworkClockDestination.progress(interrupted, true, 1f), 0f)
        assertEquals(0f, ArtworkClockDestination.fade(ArtworkClockDestination.progress(1f, false, 1f)), 0f)
    }

    @Test fun statusClockCrossfadeIsSymmetricAndCannotOverrun() {
        for (i in 0..100) {
            val p = i / 100f
            assertEquals(1f, ArtworkClockDestination.fade(p) + ArtworkClockDestination.fade(1f - p), .00001f)
        }
        assertEquals(0f, ArtworkClockDestination.fade(-1f), 0f)
        assertEquals(1f, ArtworkClockDestination.fade(2f), 0f)
    }
}
