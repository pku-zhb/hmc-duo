package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class ClockReplacementStateTest {
    private final Object window = new Object();
    private final ClockReplacementState state = new ClockReplacementState();

    @Test public void desktopToHeldAodCanAcquireALateStatusDestinationWithoutAnotherEntry() {
        state.settle(true);
        assertTrue(state.target());
        assertEquals(1f, state.progress(), 0f);
        assertEquals(0f, state.candidate(true, false, window, null), 0f);
        // Full AOD holds its transition progress rather than entering/advancing again.
        assertEquals(1f, state.candidate(true, true, window, window), 0f);
        assertTrue(state.target());
    }

    @Test public void aHeldPoseDoesNotImplyThatItsStatusDestinationHasCommitted() {
        // Native pose caching can bootstrap held AOD after the first date measurement failed.
        state.hold(true);
        assertEquals(0f, state.presented(window, 4), 0f);
        assertEquals(0f, state.candidate(true, false, window, null), 0f);
        float alpha = state.candidate(true, true, window, window);
        state.present(window, 4, alpha);
        assertEquals(1f, state.presented(window, 4), 0f);
    }

    @Test public void fullAodConsumesThePageTargetWhenItsLitTransitionHadNotAdvanced() {
        state.begin(true);
        assertEquals(0f, state.progress(), 0f);
        state.hold(true);
        assertEquals(1f, state.candidate(true, true, window, window), 0f);
    }

    @Test public void readyResourcesCannotAcquireTimeWithoutASceneOwner() {
        state.hold(false);
        assertEquals(0f, state.candidate(true, true, window, window), 0f);
        state.hold(true);
        assertEquals(0f, state.candidate(false, true, window, window), 0f);
        assertEquals(0f, state.presented(window, 4), 0f);
    }

    @Test public void desktopAndLitEntryReachTheSameHeldAodPresentation() {
        ClockReplacementState direct = new ClockReplacementState();
        direct.settle(true);
        assertEquals(0f, direct.candidate(true, false, window, null), 0f);
        state.begin(true);
        state.advance(false, .35f);
        state.advance(true, 1f);
        assertEquals(state.candidate(true, true, window, window),
                direct.candidate(true, true, window, window), 0f);
    }

    @Test public void destinationReadinessDoesNotRestartAnInProgressTransition() {
        state.begin(true);
        state.advance(false, .5f);
        assertEquals(0f, state.candidate(true, false, window, null), 0f);
        assertEquals(.5f, state.progress(), 0f);
        assertEquals(.5f, state.candidate(true, true, window, window), 0f);
        state.advance(false, .75f);
        assertEquals(.84375f, state.candidate(true, true, window, window), 0f);
    }

    @Test public void resourceArrivalDoesNotPublishBeforeTheNativeInverseAlphaCommit() {
        state.settle(true);
        assertEquals(1f, state.candidate(true, true, window, window), 0f);
        assertEquals(0f, state.presented(window, 4), 0f);
        float committed = state.candidate(true, true, window, window);
        float nativeAlpha = 1f - committed;
        state.present(window, 4, committed);
        assertEquals(1f, nativeAlpha + state.presented(window, 4), 0f);
    }

    @Test public void aNewWindowCannotUseTheOldDestinationsReadinessOrCommit() {
        Object replacement = new Object();
        state.settle(true);
        state.present(window, 4, 1f);
        assertEquals(0f, state.candidate(true, true, replacement, window), 0f);
        assertEquals(0f, state.presented(replacement, 4), 0f);
        // The page remains intended and can commit when this new window actually binds.
        assertTrue(state.target());
        float alpha = state.candidate(true, true, replacement, replacement);
        state.present(replacement, 4, alpha);
        assertEquals(1f, state.presented(replacement, 4), 0f);
        assertEquals(0f, state.presented(window, 4), 0f);
    }

    @Test public void aGeometryRevisionCannotReuseAnOldCommittedPresentation() {
        state.settle(true);
        state.present(window, 4, 1f);
        assertEquals(0f, state.presented(window, 5), 0f);
        state.present(window, 5, state.candidate(true, true, window, window));
        assertEquals(1f, state.presented(window, 5), 0f);
    }

    @Test public void unsupportedOrCustomAodKeepsNativeTimeWithoutErasingPageIntent() {
        state.settle(true);
        assertEquals(0f, state.candidate(false, true, window, window), 0f);
        assertTrue(state.target());
        assertEquals(1f, state.progress(), 0f);
        assertEquals(1f, state.candidate(true, true, window, window), 0f);
    }

    @Test public void anUnrequestedPageDoesNotAcquireEvenWithAReadyDestination() {
        state.settle(false);
        assertFalse(state.moving());
        assertEquals(0f, state.candidate(true, true, window, window), 0f);
    }

    @Test public void reversingThePageKeepsTheExistingProgressAndNativeInverse() {
        state.settle(true);
        state.begin(false);
        state.advance(false, .5f);
        assertTrue(state.moving());
        assertEquals(.5f, state.candidate(true, true, window, window), 0f);
        state.advance(true, 1f);
        assertFalse(state.moving());
        assertEquals(0f, state.candidate(true, true, window, window), 0f);
    }

    @Test public void releasingAWindowCannotRepublishItsPriorCommit() {
        state.settle(true);
        state.present(window, 4, 1f);
        state.releasePresentation();
        assertEquals(0f, state.presented(window, 4), 0f);
        assertTrue(state.target());
        assertEquals(1f, state.candidate(true, true, window, window), 0f);
        state.clear();
        assertFalse(state.moving());
        assertEquals(0f, state.candidate(true, true, window, window), 0f);
    }
}
