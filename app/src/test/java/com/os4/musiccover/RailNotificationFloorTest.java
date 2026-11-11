package com.os4.musiccover;

import org.junit.Test;
import static org.junit.Assert.*;

public class RailNotificationFloorTest {
    @Test public void outerScreenRecoversFooterSpaceAndLeavesTheGestureAreaClear() {
        int floor = RailNotificationFloor.safeBottom(1712, 82, 2.75f);
        assertEquals(1597, floor);
        assertEquals(1597, RailNotificationFloor.extend(1270, floor));
        assertTrue(floor <= 1712 - 82);
    }
    @Test public void anAlreadyLowerNativeFooterIsNotMovedUp() {
        assertEquals(1640, RailNotificationFloor.extend(1640, 1597));
        assertEquals(1712, RailNotificationFloor.extend(1712, 1597));
    }
    @Test public void missingOrIncompatibleMeasurementsKeepTheNativeAnswer() {
        assertEquals(0, RailNotificationFloor.extend(0, 1597));
        assertEquals(-1, RailNotificationFloor.extend(-1, 1597));
        assertEquals(400, RailNotificationFloor.extend(400, 1597));
        assertEquals(1270, RailNotificationFloor.extend(1270, 0));
        assertEquals(0, RailNotificationFloor.safeBottom(0, 82, 2.75f));
        assertEquals(0, RailNotificationFloor.safeBottom(1712, 82, Float.NaN));
    }
    @Test public void differentDisplayDensitiesKeepTheSamePhysicalEdgeMargin() {
        assertEquals(806, RailNotificationFloor.safeBottom(850, 32, 1));
        assertEquals(1612, RailNotificationFloor.safeBottom(1700, 64, 2));
    }
    @Test public void handoffReplacesTheOtherDisplaysCachedAbsoluteFloor() {
        assertEquals(1679, RailNotificationFloor.handoff(2331, 1679, true));
        assertEquals(1639, RailNotificationFloor.handoff(1679, 1639, true));
        assertEquals(0, RailNotificationFloor.handoff(0, 1679, true));
        assertEquals(1700, RailNotificationFloor.handoff(1700, 1679, false));
    }
}
