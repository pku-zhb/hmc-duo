package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class ChargingHintPolicyTest {
    @Test public void enabledSettingHidesOnlyChargingContentWhileLocked() {
        assertTrue(ChargingHintPolicy.hide(true, true, 3, true, false));
        assertFalse(ChargingHintPolicy.hide(false, true, 3, true, false));
        assertFalse(ChargingHintPolicy.hide(true, false, 3, true, false));
    }
    @Test public void nativeDeletionPassesThroughWithoutReenteringHideIndication() {
        assertFalse(ChargingHintPolicy.hide(true, true, 3, false, false));
    }
    @Test public void batteryProtectionAndOtherNativeIndicationsStayVisible() {
        assertFalse(ChargingHintPolicy.hide(true, true, 3, true, true));
        for (int type : new int[]{1, 2, 5, 13, 16})
            assertFalse(ChargingHintPolicy.hide(true, true, type, true, false));
    }
}
