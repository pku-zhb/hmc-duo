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
}
