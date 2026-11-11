package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class AodWakePolicyTest {
    @Test public void directAodUnlockCannotReopenCoverThroughRepeatedDozeOrWakeCallbacks() {
        boolean away = true;
        for (boolean dozing : new boolean[]{true, true, false}) {
            if (AodWakePolicy.resetExit(true, dozing, away)) away = false;
            assertTrue(away);
            assertFalse(AodWakePolicy.enterCover(true, away));
        }
    }
    @Test public void aNewSleepCanRecoverFromThePreviousExit() {
        assertTrue(AodWakePolicy.resetExit(false, true, true));
        assertFalse(AodWakePolicy.resetExit(false, true, false));
        assertFalse(AodWakePolicy.resetExit(false, false, true));
    }
    @Test public void ordinaryWakeRetainsCoverButAnUnlockedDesktopDoesNot() {
        assertTrue(AodWakePolicy.enterCover(true, false));
        assertFalse(AodWakePolicy.enterCover(false, false));
    }
}
