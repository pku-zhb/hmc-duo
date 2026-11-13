package com.zhuhuibin.hypermusiccover.duo

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class RailStatusOffsetSettingsTest {
    @Test fun defaultPreservesTheExistingAlignmentAndSignedValuesRoundTrip() {
        assertEquals(0f, ModuleBridge.State().railStatusOffsetYDp, 0f)
        for (offset in listOf(-80f, -24f, 0f, 18.5f, 24f, 80f)) {
            val json = JSONObject()
            SettingsBackup.writeRailStatusOffsetY(json, ModuleBridge.State(railStatusOffsetYDp = offset))
            assertEquals(offset, SettingsBackup.readRailStatusOffsetY(JSONObject(json.toString())))
            assertEquals(setOf("railStatusOffsetYDp"), json.keys().asSequence().toSet())
        }
    }

    @Test fun olderAndMalformedBackupsLeaveTheCurrentPositionUntouched() {
        for (input in listOf("{}", """{"railStatusOffsetYDp":null}""", """{"railStatusOffsetYDp":true}""",
            """{"railStatusOffsetYDp":"12"}""", """{"railStatusOffsetYDp":-81}""",
            """{"railStatusOffsetYDp":81}""", """{"railStatusOffsetYDp":1e1000}""")) {
            assertNull(input, SettingsBackup.readRailStatusOffsetY(JSONObject(input)))
        }
    }

    @Test fun backupCanonicalizationCannotExportAnInvalidOffset() {
        for ((input, expected) in listOf(Float.NaN to 0f, Float.POSITIVE_INFINITY to 0f,
            -100f to -80f, 100f to 80f)) {
            val json = JSONObject()
            SettingsBackup.writeRailStatusOffsetY(json, ModuleBridge.State(railStatusOffsetYDp = input))
            assertEquals(expected, SettingsBackup.readRailStatusOffsetY(json))
        }
    }
}
