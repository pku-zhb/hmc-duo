package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class ClockPoseSnapshotTest {
    private final Object window = new Object(), clock = new Object(), dateView = new Object();

    private ClockPoseSnapshot capture(float top, float unit, float date) {
        return ClockPoseSnapshot.capture(window, 4, clock, dateView, 2, 2, false, true,
                top, unit, date);
    }

    @Test public void aCompletePoseKeepsEveryCoordinateAndItsExactSourceDomain() {
        ClockPoseSnapshot pose = capture(282f, 452f, 166f);
        assertNotNull(pose);
        assertEquals(282f, pose.top, 0f);
        assertEquals(452f, pose.unit, 0f);
        assertEquals(166f, pose.date, 0f);
        assertTrue(pose.current(window, 4, clock, dateView, 2, 2, false, true));
    }

    @Test public void aFailedFreshCaptureCannotPoisonTheKnownValidEndpoint() {
        ClockPoseSnapshot committed = capture(282f, 452f, 166f);
        ClockPoseSnapshot failed = capture(Float.NaN, 505.6f, 166f);
        assertNull(failed);
        // The producer can publish only a complete replacement, not top=NaN with old unit/date.
        ClockPoseSnapshot published = failed != null ? failed : committed;
        assertSame(committed, published);
        assertEquals(282f, published.top, 0f);
        assertEquals(452f, published.unit, 0f);
        assertEquals(166f, published.date, 0f);
    }

    @Test public void nonFiniteCoordinatesAndNonPositiveUnitsCannotBePublished() {
        for (float top : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY})
            assertNull(capture(top, 452f, 166f));
        for (float unit : new float[]{Float.NaN, Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY, -1f, 0f}) assertNull(capture(282f, unit, 166f));
        assertNull(capture(282f, 452f, Float.NaN));
        assertNull(capture(282f, 452f, Float.POSITIVE_INFINITY));
    }

    @Test public void anchoredFacesCannotCombineANewClockWithAnUnknownDate() {
        assertNull(ClockPoseSnapshot.capture(window, 4, clock, null, 2, 2, false, true,
                282f, 452f, 166f));
        assertNull(ClockPoseSnapshot.capture(window, 4, clock, dateView, 2, 2, false, true,
                282f, 452f, Float.NaN));
    }

    @Test public void anUnanchoredFaceMayHaveNoDateWithoutInventingOne() {
        ClockPoseSnapshot pose = ClockPoseSnapshot.capture(window, 4, clock, null,
                2, 2, false, false, 282f, 452f, Float.NaN);
        assertNotNull(pose);
        assertTrue(Float.isNaN(pose.date));
        assertTrue(pose.current(window, 4, clock, null, 2, 2, false, false));
    }

    @Test public void anUnanchoredCompanionMayOwnItsDateWithoutASeparateDateY() {
        // The existing companion solver deliberately uses NaN for its independent date endpoint.
        ClockPoseSnapshot pose = ClockPoseSnapshot.capture(window, 4, clock, dateView,
                2, 2, false, false, 282f, 452f, Float.NaN);
        assertNotNull(pose);
        assertTrue(pose.current(window, 4, clock, dateView, 2, 2, false, false));
    }

    @Test public void anotherWindowRevisionOrNativeNodeCannotBorrowTheCoordinates() {
        ClockPoseSnapshot pose = capture(282f, 452f, 166f);
        assertFalse(pose.current(new Object(), 4, clock, dateView, 2, 2, false, true));
        assertFalse(pose.current(window, 5, clock, dateView, 2, 2, false, true));
        assertFalse(pose.current(window, 4, new Object(), dateView, 2, 2, false, true));
        assertFalse(pose.current(window, 4, clock, new Object(), 2, 2, false, true));
    }

    @Test public void anotherPageDestinationNavigationOrClockStyleNeedsItsOwnEndpoint() {
        ClockPoseSnapshot pose = capture(282f, 452f, 166f);
        assertFalse(pose.current(window, 4, clock, dateView, 1, 2, false, true));
        assertFalse(pose.current(window, 4, clock, dateView, 2, 1, false, true));
        assertFalse(pose.current(window, 4, clock, dateView, 2, 2, true, true));
        assertFalse(pose.current(window, 4, clock, dateView, 2, 2, false, false));
    }

    @Test public void missingWindowOrClockCannotBecomeASnapshot() {
        assertNull(ClockPoseSnapshot.capture(null, 4, clock, dateView, 2, 2, false, true,
                282f, 452f, 166f));
        assertNull(ClockPoseSnapshot.capture(window, 4, null, dateView, 2, 2, false, true,
                282f, 452f, 166f));
    }
}
