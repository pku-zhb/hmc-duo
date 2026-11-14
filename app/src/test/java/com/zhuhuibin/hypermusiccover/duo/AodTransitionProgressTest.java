package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class AodTransitionProgressTest {
    private final Object window = new Object();
    @Test public void coldScenePublishesEndpointWithoutAnimationCallback() {
        AodTransitionProgress state = new AodTransitionProgress();
        state.scene(window, 1, true);
        AodTransitionProgress.Snapshot value = state.snapshot(window, 1, false);
        assertEquals(1f, value.fraction, 0f); assertFalse(value.running);
    }
    @Test public void directionLocalNativeFractionBecomesAbsoluteBothWays() {
        AodTransitionProgress state = new AodTransitionProgress();
        state.begin(window, 1, true); state.frame(window, 1, true, .25f);
        assertEquals(.25f, state.snapshot(window, 1, false).fraction, 0f);
        state.complete(window, 1, true);
        state.begin(window, 1, false); state.frame(window, 1, false, .25f);
        assertEquals(.75f, state.snapshot(window, 1, true).fraction, 0f);
        state.complete(window, 1, false);
        assertEquals(0f, state.snapshot(window, 1, true).fraction, 0f);
    }
    @Test public void dozeStateCannotCutAnActiveNativeTransition() {
        AodTransitionProgress state = new AodTransitionProgress();
        state.begin(window, 1, true); state.frame(window, 1, true, .4f);
        state.scene(window, 1, true);
        assertEquals(.4f, state.snapshot(window, 1, true).fraction, 0f);
        assertTrue(state.snapshot(window, 1, true).running);
    }
    @Test public void plannedNativeWakeDoesNotPublishItsEndpointBeforeOnBegin() {
        AodTransitionProgress state = new AodTransitionProgress();
        state.scene(window, 1, true);
        state.scene(window, 1, false, true);
        assertEquals(1f, state.snapshot(window, 1, false).fraction, 0f);
        assertTrue(state.snapshot(window, 1, false).running);
        state.begin(window, 1, false); state.frame(window, 1, false, .25f);
        assertEquals(.75f, state.snapshot(window, 1, false).fraction, 0f);
    }
    @Test public void reverseStartsFromActualCancelledFrame() {
        AodTransitionProgress state = new AodTransitionProgress();
        state.begin(window, 1, true); state.frame(window, 1, true, .4f);
        state.cancel(window, 1); state.begin(window, 1, false);
        assertEquals(.4f, state.snapshot(window, 1, false).fraction, 0f);
        state.frame(window, 1, false, .5f);
        assertEquals(.2f, state.snapshot(window, 1, false).fraction, .00001f);
    }
    @Test public void newWindowAndRevisionCannotBorrowPreviousFraction() {
        AodTransitionProgress state = new AodTransitionProgress();
        state.begin(window, 1, true); state.frame(window, 1, true, .7f);
        assertEquals(0f, state.snapshot(new Object(), 1, false).fraction, 0f);
        assertEquals(1f, state.snapshot(window, 2, true).fraction, 0f);
        state.scene(window, 2, true);
        assertFalse(state.snapshot(window, 2, true).running);
    }
    @Test public void invalidFrameDoesNotPoisonLastNativePose() {
        AodTransitionProgress state = new AodTransitionProgress();
        state.begin(window, 1, true); state.frame(window, 1, true, .4f);
        state.frame(window, 1, true, Float.NaN);
        assertEquals(.4f, state.snapshot(window, 1, true).fraction, 0f);
    }
    @Test public void lateOldFrameCannotRestartCompletedDirectionWithoutNewBegin() {
        AodTransitionProgress state = new AodTransitionProgress();
        state.begin(window, 1, true); state.complete(window, 1, true);
        state.frame(window, 1, true, .2f);
        assertEquals(1f, state.snapshot(window, 1, true).fraction, 0f);
        assertFalse(state.snapshot(window, 1, true).running);
        state.begin(window, 1, true); state.frame(window, 1, true, .2f);
        assertEquals(.2f, state.snapshot(window, 1, true).fraction, 0f);
    }
}
