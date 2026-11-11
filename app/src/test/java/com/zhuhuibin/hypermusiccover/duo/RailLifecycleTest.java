package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class RailLifecycleTest {
    @Test public void aodAndWakeKeepTheObservedPlanWhileTheKeyguardIsLocked() {
        // Clock/shortcut visibility and screen-on change during AOD and wake; none is a
        // reason to surrender a previously measured rail in the same display geometry.
        boolean eligible = RailLifecycle.eligible(true, true, false);
        assertTrue(RailLifecycle.retain(eligible, true, 7, 7));
    }

    @Test public void unlockDisableAndBouncerReleaseTheRail() {
        assertFalse(RailLifecycle.eligible(true, false, false));
        assertFalse(RailLifecycle.eligible(false, true, false));
        assertFalse(RailLifecycle.eligible(true, true, true));
        assertFalse(RailLifecycle.retain(false, true, 7, 7));
    }

    @Test public void displayHandoffAndAnUnobservedPlanCannotReuseOldCoordinates() {
        assertFalse(RailLifecycle.retain(true, true, 7, 8));
        assertFalse(RailLifecycle.retain(true, false, -1, 7));
    }
    @Test public void aodAndItsWakeDoNotRecaptureAlreadyAnimatedButtonCoordinates() {
        assertTrue(RailLifecycle.freeze(true, false, false, 1f));
        assertTrue(RailLifecycle.freeze(true, true, true, .95f));
        assertTrue(RailLifecycle.freeze(true, true, false, .97f));
        assertFalse(RailLifecycle.freeze(true, true, false, 1f));
        assertFalse(RailLifecycle.freeze(false, false, true, .95f));
    }
}
