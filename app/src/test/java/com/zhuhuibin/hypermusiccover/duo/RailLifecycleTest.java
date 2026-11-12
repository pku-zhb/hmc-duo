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

    @Test public void columnPinSurvivesEveryWakeFrameWithoutAnyVisibleCapsule() {
        boolean pinned = RailLifecycle.holdColumnX(false, true, false, false, false, true);
        assertTrue(pinned);
        for (float scale : new float[]{.95f, .97f, .99f}) {
            pinned = RailLifecycle.holdColumnX(pinned, true, false, true, false,
                    RailLifecycle.freeze(true, true, false, scale));
            assertTrue(pinned);
        }
        pinned = RailLifecycle.holdColumnX(pinned, true, false, true, false,
                RailLifecycle.freeze(true, true, false, 1f));
        assertFalse(pinned);
        // An ordinary lock-screen animation must not imitate an AOD transition.
        assertFalse(RailLifecycle.holdColumnX(false, true, false, true, false, true));
    }

    @Test public void unlockDisableBouncerAndGeometryReplacementReleaseTheOldColumn() {
        assertFalse(RailLifecycle.holdColumnX(true, false, false, false, true, true));
        assertFalse(RailLifecycle.holdColumnX(true, true, true, false, true, true));
        // A new display may arm its own AOD anchor, independently of the discarded plan.
        assertTrue(RailLifecycle.holdColumnX(false, true, false, false, true, true));
    }
}
