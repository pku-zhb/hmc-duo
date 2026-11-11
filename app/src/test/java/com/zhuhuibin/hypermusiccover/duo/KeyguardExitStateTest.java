package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class KeyguardExitStateTest {
    @Test public void repeatedShowingReportsCannotReopenCoverDuringDirectUnlock() {
        KeyguardExitState state = new KeyguardExitState();
        assertFalse(state.showing(true));
        assertTrue(state.exit(true));
        for (int i = 0; i < 4; i++) {
            assertTrue(state.showing(true));
            assertFalse(AodWakePolicy.enterCover(true, state.showing(true)));
        }
    }
    @Test public void exitBeforeTheFirstShowingReportIsStillAnExit() {
        KeyguardExitState state = new KeyguardExitState();
        assertTrue(state.exit(true));
        assertTrue(state.showing(true));
    }
    @Test public void aRealHiddenThenShownTransitionStartsANewSession() {
        KeyguardExitState state = new KeyguardExitState();
        state.showing(true); state.exit(true);
        assertTrue(state.showing(false));
        assertFalse(state.showing(true));
    }
    @Test public void explicitCancellationCanResumeTheStillVisibleLockScreen() {
        KeyguardExitState state = new KeyguardExitState();
        state.showing(true); state.exit(true);
        assertFalse(state.exit(false));
        assertFalse(state.showing(true));
    }
    @Test public void nativeFlagCleanupAfterUnlockDoesNotBecomeASecondEntry() {
        KeyguardExitState state = new KeyguardExitState();
        state.showing(true); state.exit(true); state.showing(false);
        assertTrue(state.exit(false));
        assertFalse(AodWakePolicy.enterCover(true, state.exit(false)));
    }
    @Test public void explicitSleepCanResetAnExitWithoutAnIntermediateHiddenFrame() {
        KeyguardExitState state = new KeyguardExitState();
        state.showing(true); state.exit(true);
        assertFalse(state.sleep());
        assertFalse(state.showing(true));
    }
}
