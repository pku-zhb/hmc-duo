package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class ClockPoseSourceTest {
    private final Object window = new Object(), clock = new Object(), date = new Object();
    private final ClockPoseSource source = new ClockPoseSource();

    @Test public void aMeasuredPoseCanBeUsedOnlyByItsCurrentNativeNodes() {
        source.capture(window, 4, clock, date);
        assertTrue(source.matches(window, 4, clock, date));
        assertFalse(source.matches(window, 4, new Object(), date));
        assertFalse(source.matches(window, 4, clock, new Object()));
        assertFalse(source.matches(window, 4, clock, null));
    }

    @Test public void newWindowsAndGeometryRevisionsCannotBorrowAnEarlierPose() {
        source.capture(window, 4, clock, date);
        assertFalse(source.matches(new Object(), 4, clock, date));
        assertFalse(source.matches(window, 5, clock, date));
        assertFalse(source.matches(null, 4, clock, date));
        assertFalse(source.matches(window, 4, null, date));
    }

    @Test public void aValidUnanchoredClockDoesNotRequireAnInventedDate() {
        source.capture(window, 4, clock, null);
        assertTrue(source.matches(window, 4, clock, null));
        assertFalse(source.matches(window, 4, clock, date));
    }

    @Test public void rebindingCapturesTheNewNodesWithoutExtendingTheOldDomain() {
        Object replacementClock = new Object(), replacementDate = new Object();
        source.capture(window, 4, clock, date);
        source.capture(window, 4, replacementClock, replacementDate);
        assertFalse(source.matches(window, 4, clock, date));
        assertTrue(source.matches(window, 4, replacementClock, replacementDate));
    }
}
