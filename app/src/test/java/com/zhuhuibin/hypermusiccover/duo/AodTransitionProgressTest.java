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
    @Test public void delayedNativeWakeIntentCannotPublishAnEndpointBeforeOnBegin() {
        AodTransitionProgress state = new AodTransitionProgress();
        state.complete(window, 1, true);
        // Recorded failure: display intent sees mWillDoAodAnim=false before onBegin.
        state.scene(window, 1, false, false, true);
        assertEquals(1f, state.snapshot(window, 1, false).fraction, 0f);
        assertTrue(state.snapshot(window, 1, false).running);
        state.begin(window, 1, false);
        state.frame(window, 1, false, .014f);
        assertEquals(.986f, state.snapshot(window, 1, false).fraction, .00001f);
    }

    @Test public void delayedWakeKeepsTheStillPresenterUntilTheNativeFramesCoverIt() {
        AodTransitionProgress state = new AodTransitionProgress();
        ArtworkBackdropState.Handoff handoff = new ArtworkBackdropState.Handoff();
        state.complete(window, 1, true);
        handoff.select(72, ArtworkBackdropState.STILL, window, 9, 1, 9);
        handoff.presented(72, ArtworkBackdropState.STILL, window, 9, 1, 9, 1f);
        // wake1 t=152ms/250ms: no native run was reported as planned yet.
        state.scene(window, 1, false, false, true);
        handoff.select(73, ArtworkBackdropState.MESH, window, 9, 1, 9);
        float alpha = AodBackdropAlpha.value(false, true, false,
                state.snapshot(window, 1, false).fraction, 1f);
        assertEquals(0f, alpha, 0f);
        assertFalse(handoff.coveredBy(ArtworkBackdropState.MESH));
        assertTrue(handoff.retains(ArtworkBackdropState.STILL));
        // Its first real native frame is .986 absolute, so no wallpaper may be exposed.
        state.begin(window, 1, false);
        for (float nativeLocal : new float[]{.014f, .1f, .5f, .99f}) {
            state.frame(window, 1, false, nativeLocal);
            alpha = AodBackdropAlpha.value(false, true, false,
                    state.snapshot(window, 1, false).fraction, 1f);
            handoff.presented(73, ArtworkBackdropState.MESH, window, 9, 1, 9, alpha);
            assertFalse(handoff.coveredBy(ArtworkBackdropState.MESH));
            assertEquals(1f, AodBackdropAlpha.value(handoff.retains(ArtworkBackdropState.STILL),
                    true, true, state.snapshot(window, 1, false).fraction, 0f), 0f);
        }
        state.complete(window, 1, false);
        alpha = AodBackdropAlpha.value(false, true, false,
                state.snapshot(window, 1, false).fraction, 0f);
        handoff.presented(73, ArtworkBackdropState.MESH, window, 9, 1, 9, alpha);
        assertTrue(handoff.coveredBy(ArtworkBackdropState.MESH));
        assertFalse(handoff.retains(ArtworkBackdropState.STILL));
    }

    @Test public void aBoundNativeResetStillCompletesWithoutAnimationFrames() {
        AodTransitionProgress state = new AodTransitionProgress();
        state.scene(window, 1, true, false, true);
        assertEquals(1f, state.snapshot(window, 1, true).fraction, 0f);
        assertFalse(state.snapshot(window, 1, true).running);
        state.scene(window, 1, false, false, true);
        assertEquals(1f, state.snapshot(window, 1, false).fraction, 0f);
        state.complete(window, 1, false);
        assertEquals(0f, state.snapshot(window, 1, false).fraction, 0f);
        assertFalse(state.snapshot(window, 1, false).running);
    }

    @Test public void completionBeforeDisplayIntentDoesNotCreateAnotherPendingTransition() {
        AodTransitionProgress state = new AodTransitionProgress();
        state.complete(window, 1, true);
        state.complete(window, 1, false);
        state.scene(window, 1, false, false, true);
        assertEquals(0f, state.snapshot(window, 1, false).fraction, 0f);
        assertFalse(state.snapshot(window, 1, false).running);
    }

    @Test public void unavailableNativeSourceAndNewWindowsKeepImmediateSceneEndpoints() {
        AodTransitionProgress state = new AodTransitionProgress();
        state.scene(window, 1, true, false, false);
        state.scene(window, 1, false, false, false);
        assertEquals(0f, state.snapshot(window, 1, false).fraction, 0f);
        assertFalse(state.snapshot(window, 1, false).running);
        state.complete(window, 1, true);
        Object replacement = new Object();
        state.scene(replacement, 1, false, false, true);
        assertEquals(0f, state.snapshot(replacement, 1, false).fraction, 0f);
        assertFalse(state.snapshot(replacement, 1, false).running);
    }

    @Test public void sameDirectionSceneAfterCancelKeepsTheActualNativeFraction() {
        AodTransitionProgress state = new AodTransitionProgress();
        state.begin(window, 1, true);
        state.frame(window, 1, true, .4f);
        state.cancel(window, 1);
        state.scene(window, 1, true, false, true);
        assertEquals(.4f, state.snapshot(window, 1, true).fraction, 0f);
        assertFalse(state.snapshot(window, 1, true).running);
        state.begin(window, 1, true);
        state.frame(window, 1, true, .5f);
        assertEquals(.7f, state.snapshot(window, 1, true).fraction, .00001f);
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
