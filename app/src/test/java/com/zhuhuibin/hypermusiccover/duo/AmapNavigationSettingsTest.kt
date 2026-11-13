package com.zhuhuibin.hypermusiccover.duo

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class AmapNavigationSettingsTest {
    @Test fun disablingAnArmedPagePreservesCurrentNavigationForReEnable() {
        val state = ImmersiveSceneAvailability()
        assertTrue(state.enabled())
        assertFalse(state.ready())
        state.setArmed(true)
        assertTrue(state.ready())
        state.setEnabled(false)
        assertFalse(state.ready())
        assertTrue(state.armed())
        state.setEnabled(true)
        assertTrue(state.ready())
    }

    @Test fun navigationEndingWhileDisabledCannotReOpenAStaleMap() {
        val state = ImmersiveSceneAvailability()
        state.setArmed(true)
        state.setEnabled(false)
        state.setArmed(false)
        state.setEnabled(true)
        assertFalse(state.ready())
        state.setArmed(true)
        assertTrue(state.ready())
    }

    @Test fun freshStateKeepsTheExistingDefaultAndBothChoicesRoundTrip() {
        assertTrue(ModuleBridge.State().amapNavigationMap)
        for (enabled in listOf(false, true)) {
            val json = JSONObject()
            SettingsBackup.writeAmapNavigationMap(json, ModuleBridge.State(amapNavigationMap = enabled))
            assertEquals(enabled, SettingsBackup.readAmapNavigationMap(JSONObject(json.toString())))
        }
    }

    @Test fun oldOrMalformedBackupsDoNotResetTheCurrentChoice() {
        for (input in listOf("{}", """{"amapNavigationMap":null}""", """{"amapNavigationMap":"false"}""", """{"amapNavigationMap":0}""")) {
            assertNull(SettingsBackup.readAmapNavigationMap(JSONObject(input)))
        }
    }
}
