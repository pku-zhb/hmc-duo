package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class NavigationAodPolicyTest {
    @Test public void pendingRemoteContentCanReserveTheClockOnlyWithAllCurrentSceneEvidence() {
        for (int bits = 0; bits < 32; bits++) {
            assertEquals("gate set " + bits, bits == 31, NavigationAodPolicy.prepare(
                    (bits & 1) != 0, (bits & 2) != 0, (bits & 4) != 0, (bits & 8) != 0, (bits & 16) != 0));
        }
    }

    @Test public void fullScreenNavigationKeepsItsClockEvenWithoutTheGenericSmallAodPreference() {
        assertTrue(NavigationAodPolicy.keepClock(true, true));
        assertFalse(NavigationAodPolicy.keepClock(true, false));
        assertFalse(NavigationAodPolicy.keepClock(false, true));
    }
}
