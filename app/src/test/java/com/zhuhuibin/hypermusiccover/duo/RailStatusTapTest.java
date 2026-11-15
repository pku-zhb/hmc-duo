package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class RailStatusTapTest {
    @Test public void shortStationaryTapsFireOnceOnlyInsideTheOriginalStatusStream() {
        RailStatusTap tap = new RailStatusTap();
        tap.start(true, 100, 200, 8, 1000);
        tap.move(104, 203, 1, 1000);
        assertTrue(tap.finish(true, 104, 203, 1, 1000, 1150, 500));
        assertFalse(tap.finish(true, 104, 203, 1, 1000, 1151, 500));
        tap.start(false, 100, 200, 8, 2000);
        assertFalse(tap.finish(true, 100, 200, 1, 2000, 2100, 500));
    }

    @Test public void dragsCannotBecomeTapsAfterReturningToTheCluster() {
        RailStatusTap tap = new RailStatusTap();
        tap.start(true, 100, 200, 8, 1000);
        tap.move(100, 212, 1, 1000);
        assertFalse(tap.finish(true, 100, 200, 1, 1000, 1200, 500));
    }

    @Test public void LongPressMultitouchCancellationAndSceneLossNeverOpenThePanel() {
        RailStatusTap tap = new RailStatusTap();
        tap.start(true, 100, 200, 8, 1000);
        assertFalse(tap.finish(true, 100, 200, 1, 1000, 1500, 500));
        tap.start(true, 100, 200, 8, 2000);
        tap.move(100, 200, 2, 2000);
        assertFalse(tap.finish(true, 100, 200, 1, 2000, 2100, 500));
        tap.start(true, 100, 200, 8, 3000);
        tap.cancel();
        assertFalse(tap.finish(true, 100, 200, 1, 3000, 3100, 500));
        tap.start(true, 100, 200, 8, 4000);
        assertFalse(tap.finish(false, 100, 200, 1, 4000, 4100, 500));
        tap.start(true, 100, 200, 8, 5000);
        assertFalse(tap.finish(true, 100, 200, 1, 5001, 5100, 500));
    }
}
