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
}
