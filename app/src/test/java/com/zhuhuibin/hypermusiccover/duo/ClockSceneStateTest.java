package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class ClockSceneStateTest {
    @Test public void temporaryMeasurementFailureKeepsTheSameWindowLease() {
        Object window = new Object();
        ClockSceneState state = new ClockSceneState();
        state.update(1, window, 4, 2, true);
        state.beginPass(); state.measured(true, false); state.committed();
        state.beginPass(); state.measured(false, false);
        assertTrue(state.requested);
        assertFalse(state.ready);
        assertTrue(state.retainLease());
        assertEquals(1, state.retainedMisses);
        assertEquals(0, state.passCommits);
    }

    @Test public void aNewPageRetainsTheOldPoseUntilItsOwnCommit() {
        Object window = new Object();
        ClockSceneState state = new ClockSceneState();
        state.update(1, window, 4, 2, true); state.committed();
        state.update(2, window, 4, 3, true);
        assertTrue(state.retainLease());
        assertFalse(state.currentCommit());
        state.beginPass(); state.measured(true, true); state.committed();
        assertTrue(state.currentCommit());
        assertTrue(state.cached);
    }

    @Test public void displayRevisionAndWindowIdentityInvalidateTheLease() {
        Object window = new Object();
        ClockSceneState state = new ClockSceneState();
        state.update(1, window, 4, 2, true); state.committed();
        state.update(2, window, 5, 2, true);
        assertFalse(state.retainLease());
        state.committed();
        state.update(3, new Object(), 5, 2, true);
        assertFalse(state.retainLease());
    }

    @Test public void nativeSceneOrExplicitReleaseCannotRetainThePreviousLease() {
        Object window = new Object();
        ClockSceneState state = new ClockSceneState();
        state.update(1, window, 4, 2, true); state.committed();
        state.update(2, window, 4, 0, false);
        assertFalse(state.retainLease());
        state.update(3, window, 4, 2, true); state.released();
        assertFalse(state.retainLease());
    }
    @Test public void aColdDateRowWaitsForItsOwnPlanBeforeLatchingAnyAodEndpoint() {
        ClockSceneState state = new ClockSceneState();
        state.update(1, new Object(), 4, 2, true);
        assertFalse(state.needsAodDestinationCommit(true, false, false, true, false, false));
        assertTrue(state.needsAodDestinationCommit(true, false, false, true, true, false));
    }

    @Test public void aHeldNativePoseDoesNotConsumeALateDateRowDestination() {
        ClockSceneState state = new ClockSceneState();
        state.update(1, new Object(), 4, 2, true);
        state.committed();
        assertFalse(state.needsAodDestinationCommit(true, false, true, true, false, false));
        assertTrue(state.needsAodDestinationCommit(true, false, true, true, true, false));
        // After the full row endpoint was applied, later pre-draws do not enter again.
        assertFalse(state.needsAodDestinationCommit(true, false, true, true, true, true));
    }

    @Test public void aWarmLitDateRowNeedsNoSecondEntryDuringFullAod() {
        ClockSceneState state = new ClockSceneState();
        state.update(1, new Object(), 4, 2, true);
        state.committed();
        assertFalse(state.needsAodDestinationCommit(true, false, true, true, true, true));
    }

    @Test public void newWindowOrRevisionCannotReuseAnEarlierRowsPresentationProof() {
        Object window = new Object();
        ClockSceneState state = new ClockSceneState();
        state.update(1, window, 4, 2, true); state.committed();
        state.update(2, window, 5, 2, true);
        assertTrue(state.needsAodDestinationCommit(true, false, true, true, true, true));
        state.committed();
        state.update(3, new Object(), 5, 2, true);
        assertFalse(state.needsAodDestinationCommit(true, false, true, true, false, false));
        assertTrue(state.needsAodDestinationCommit(true, false, true, true, true, true));
    }

    @Test public void statusDoesNotDependOnDateRowCoordinatesOrReenterWhenAlreadyHeld() {
        ClockSceneState state = new ClockSceneState();
        state.update(1, new Object(), 4, 2, true);
        assertTrue(state.needsAodDestinationCommit(true, false, false, false, false, false));
        state.committed();
        assertFalse(state.needsAodDestinationCommit(true, false, true, false, false, false));
    }

    @Test public void wakeNativeAndMissingWindowCannotAcquireAnAodDestination() {
        ClockSceneState state = new ClockSceneState();
        Object window = new Object();
        state.update(1, window, 4, 2, false);
        assertFalse(state.needsAodDestinationCommit(true, false, false, true, true, false));
        state.update(2, window, 4, 2, true);
        assertFalse(state.needsAodDestinationCommit(false, false, false, true, true, false));
        assertFalse(state.needsAodDestinationCommit(true, true, false, true, true, false));
        state.update(3, null, 4, 2, true);
        assertFalse(state.needsAodDestinationCommit(true, false, false, true, true, false));
    }
    @Test public void theLiveInvalidHeldStateRecoversOnceWhenMeasurementIsReady() {
        Object window = new Object(), clock = new Object(), date = new Object();
        ClockSceneState state = new ClockSceneState();
        state.update(29, window, 1, 2, true);
        state.beginPass(); state.measured(true, false);
        // Exact failed-device shape: held=true, top=NaN, STATUS ready, no current commit/lease.
        ClockPoseSnapshot invalid = ClockPoseSnapshot.capture(window, 1, clock, date,
                2, 2, false, true, Float.NaN, 505.6f, 166f);
        boolean held = state.holdsPose(true, invalid != null, false);
        assertFalse(held);
        assertTrue(state.needsAodDestinationCommit(true, false, held, false, false, false));
        ClockPoseSnapshot ready = ClockPoseSnapshot.capture(window, 1, clock, date,
                2, 2, false, true, 282f, 505.6f, 166f);
        assertNotNull(ready);
        state.committed();
        assertTrue(state.holdsPose(true, true, true));
        for (int frame = 0; frame < 4; frame++) {
            state.beginPass(); state.measured(true, false);
            assertFalse(state.needsAodDestinationCommit(true, false,
                    state.holdsPose(true, true, true), false, false, false));
        }
        assertEquals(1L, state.commits);
    }

    @Test public void finiteCoordinatesWithoutACurrentCommitLeaseCannotLatchHeld() {
        ClockSceneState state = new ClockSceneState();
        state.update(1, new Object(), 4, 2, true);
        assertFalse(state.holdsPose(true, true, true));
        assertTrue(state.needsAodDestinationCommit(true, false, false, false, false, false));
        state.committed();
        assertFalse(state.holdsPose(true, false, true));
        assertFalse(state.holdsPose(true, true, false));
        assertFalse(state.holdsPose(false, true, true));
        assertTrue(state.holdsPose(true, true, true));
    }

    @Test public void aNewDomainCannotLatchAPreviousFiniteEndpointEvenWhenRawHeldIsTrue() {
        Object window = new Object();
        ClockSceneState state = new ClockSceneState();
        state.update(1, window, 4, 2, true); state.committed();
        state.update(2, new Object(), 4, 2, true);
        assertFalse(state.holdsPose(true, true, true));
        assertTrue(state.needsAodDestinationCommit(true, false,
                state.holdsPose(true, true, true), false, false, false));
    }

    @Test public void invalidHeldStillWaitsForRowCoordinatesAndCannotAcquireWithoutAnOwner() {
        ClockSceneState state = new ClockSceneState();
        Object window = new Object();
        state.update(1, window, 4, 2, true);
        boolean held = state.holdsPose(true, false, false);
        assertFalse(state.needsAodDestinationCommit(true, false, held, true, false, false));
        assertTrue(state.needsAodDestinationCommit(true, false, held, true, true, false));
        state.update(2, window, 4, 2, false);
        assertFalse(state.holdsPose(true, true, true));
        assertFalse(state.needsAodDestinationCommit(true, false, false, false, false, false));
    }
}
