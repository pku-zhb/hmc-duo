package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class AodWakePolicyTest {
    @Test public void ordinaryWakeRetainsCoverButAnUnlockedDesktopDoesNot() {
        assertTrue(AodWakePolicy.enterCover(true, false));
        assertFalse(AodWakePolicy.enterCover(false, false));
    }
    @Test public void anExitBlocksLateWakeWhileTheKeyguardIsStillShowing() {
        assertFalse(AodWakePolicy.enterCover(true, true));
        assertFalse(AodWakePolicy.enterCover(false, true));
    }
}
