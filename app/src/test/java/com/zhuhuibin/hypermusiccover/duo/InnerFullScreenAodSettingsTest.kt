package com.zhuhuibin.hypermusiccover.duo

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class InnerFullScreenAodSettingsTest {
    @Test fun experimentalOverrideIsOffByDefaultAndBothChoicesRoundTrip() {
        assertFalse(ModuleBridge.State().innerFullScreenAod)
        for (enabled in listOf(false, true)) {
            val json = JSONObject()
            SettingsBackup.writeInnerFullScreenAod(json, ModuleBridge.State(innerFullScreenAod = enabled))
            assertEquals(enabled, SettingsBackup.readInnerFullScreenAod(JSONObject(json.toString())))
        }
    }

    @Test fun oldAndMalformedBackupsCannotEnableOrResetTheOverride() {
        for (input in listOf("{}", """{"innerFullScreenAod":null}""",
            """{"innerFullScreenAod":"true"}""", """{"innerFullScreenAod":1}""")) {
            assertNull(input, SettingsBackup.readInnerFullScreenAod(JSONObject(input)))
        }
    }
}
