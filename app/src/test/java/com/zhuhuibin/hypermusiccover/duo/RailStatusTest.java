package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class RailStatusTest {
    @Test public void referenceClusterAlignsWithCapsulesAndClearsTheFirstBubble() {
        RightRailLayout.Plan rail = RightRailLayout.plan(1168, 1712, 2.75f, 0, 126, 0, 0, 0);
        FoldMusicLayout.Box status = RailStatusLayout.place((rail.pill.left+rail.pill.right)/2, 126, 2.75f);
        assertEquals(962.75f, status.left, .001f);
        assertEquals(159f, status.top, .001f);
        assertEquals(205f, status.width(), .001f);
        assertTrue(status.right <= 1168);
        assertTrue(rail.strip.top-status.bottom >= 10*2.75f);
    }

    @Test public void statusAndCapsuleReservationsScaleAcrossDisplays() {
        for (float d : new float[]{1, 1.5f, 2.75f, 3}) {
            for (float[] window : new float[][]{{425, 700}, {650, 850}, {850, 650}, {850, 500}}) {
                RightRailLayout.Plan rail = RightRailLayout.plan(window[0]*d, window[1]*d, d,
                        0, 46*d, 10*d, 24*d, 0);
                assertNotNull(rail);
                FoldMusicLayout.Box status = RailStatusLayout.place((rail.pill.left+rail.pill.right)/2, 46*d, d);
                assertTrue(status.right <= window[0]*d-10*d);
                assertTrue(status.bottom+10*d <= rail.strip.top);
                assertTrue(rail.companion.top >= rail.strip.top);
            }
        }
    }

    @Test public void invalidGeometryIsNotDrawn() {
        assertNull(RailStatusLayout.place(Float.NaN, 126, 2.75f));
        assertNull(RailStatusLayout.place(1000, -1, 2.75f));
        assertNull(RailStatusLayout.place(1000, 126, 0));
    }

    @Test public void batteryScalesAndUnknownValuesDoNotBecomeFull() {
        assertEquals(53, RailStatusValues.batteryPercent(53, 100));
        assertEquals(53, RailStatusValues.batteryPercent(106, 200));
        assertEquals(0, RailStatusValues.batteryPercent(0, 100));
        assertEquals(100, RailStatusValues.batteryPercent(120, 100));
        assertEquals(-1, RailStatusValues.batteryPercent(-1, 100));
        assertEquals(-1, RailStatusValues.batteryPercent(80, 0));
    }

    @Test public void lossOfServiceCannotLeaveOldFullBars() {
        assertEquals(0, RailStatusValues.signalLevel(4, false));
        assertEquals(-1, RailStatusValues.signalLevel(-1, true));
        assertEquals(2, RailStatusValues.signalLevel(2, true));
        assertEquals(4, RailStatusValues.signalLevel(8, true));
    }

    @Test public void missingWifiDoesNotDrawAConnectedWedge() {
        assertEquals(-1, RailStatusValues.wifiLevel(false, -40));
        assertEquals(-1, RailStatusValues.wifiLevel(true, -127));
        assertEquals(-1, RailStatusValues.wifiLevel(true, 10));
        assertEquals(0, RailStatusValues.wifiLevel(true, -90));
        assertEquals(1, RailStatusValues.wifiLevel(true, -70));
        assertEquals(2, RailStatusValues.wifiLevel(true, -50));
    }
}
