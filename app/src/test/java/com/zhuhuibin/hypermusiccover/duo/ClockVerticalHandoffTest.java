package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;

import static org.junit.Assert.*;

public class ClockVerticalHandoffTest {
    private static final float LIFT = -33f;

    @Test public void idleToContentHandoffKeepsTheDisplayedStartButRestoresNativeY() {
        ClockPropertyLease rail = new ClockPropertyLease();
        ClockPropertyLease pose = new ClockPropertyLease();
        float y = rail.offset(0f, LIFT);
        float displayedStart = y;

        // Capture the transition start first, then return the idle owner's baseline.
        y = rail.restore(y);
        assertEquals(LIFT, displayedStart, 0f);
        assertEquals(0f, y, 0f);
        y = pose.write(y, -100f);
        y = pose.restore(y);
        assertEquals(0f, y, 0f);
        assertEquals(LIFT, rail.offset(y, LIFT), 0f);
    }

    @Test public void heldAodAndRepeatedPageChangesNeverAccumulateAnIdleLift() {
        for (float nativeY : new float[]{0f, 17f, -8.5f}) {
            ClockPropertyLease rail = new ClockPropertyLease();
            ClockPropertyLease pose = new ClockPropertyLease();
            float y = nativeY;
            for (int page = 0; page < 20; page++) {
                y = rail.offset(y, LIFT);
                y = rail.restore(y);
                y = pose.write(y, -336.6902f);
                // The idle owner has no lease throughout a held AOD, including minute ticks.
                for (int frame = 0; frame < 60; frame++) {
                    assertFalse(rail.owned());
                    y = rail.restore(y);
                    y = pose.write(y, -336.6902f + frame / 10f);
                }
                y = pose.restore(y);
                assertEquals(nativeY, y, 0f);
            }
            assertEquals(nativeY + LIFT, rail.offset(y, LIFT), 0f);
        }
    }

    @Test public void nativeRewriteDuringContentReleaseBecomesTheNextIdleBaseline() {
        ClockPropertyLease rail = new ClockPropertyLease();
        ClockPropertyLease pose = new ClockPropertyLease();
        float y = rail.offset(0f, LIFT);
        y = rail.restore(y);
        pose.write(y, -100f);
        y = pose.restore(17f);
        assertEquals(17f, y, 0f);
        y = rail.offset(y, LIFT);
        assertEquals(17f + LIFT, y, 0f);
        assertEquals(17f, rail.restore(y), 0f);
    }

    @Test public void idleOffsetTracksNativeRelayoutAndChangedDensityWithoutAccumulating() {
        ClockPropertyLease rail = new ClockPropertyLease();
        float y = 7f;
        for (int frame = 0; frame < 120; frame++) y = rail.offset(y, LIFT);
        assertEquals(7f + LIFT, y, 0f);
        y = rail.offset(y, -36f);
        assertEquals(7f - 36f, y, 0f);
        y = rail.offset(18f, -36f);
        assertEquals(18f - 36f, y, 0f);
        assertEquals(18f, rail.restore(y), 0f);
    }

    @Test public void aMatchingNativeOffsetDoesNotAcquireAnIdleLease() {
        ClockPropertyLease rail = new ClockPropertyLease();
        assertEquals(7f, rail.offset(7f, 0f), 0f);
        assertFalse(rail.owned());
        assertEquals(18f, rail.restore(18f), 0f);
    }
}
