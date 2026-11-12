package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class CompactClockRowLayoutTest {
    @Test public void artworkAndLyricsUseOuterDisplayButNeverInnerDisplay() {
        assertTrue(CompactClockRowLayout.requested(true, 1168, 1712, 2.75f, true, false, false));
        assertFalse(CompactClockRowLayout.requested(true, 2488, 2224, 2.75f, true, false, false));
        assertFalse(CompactClockRowLayout.requested(true, 2224, 2488, 2.75f, true, false, false));
    }

    @Test public void systemAndPlayerPagesRetainTheirNativeClock() {
        assertFalse(CompactClockRowLayout.requested(true, 400, 800, 1, false, false, false));
        assertFalse(CompactClockRowLayout.requested(true, 400, 800, 1, true, true, false));
        assertFalse(CompactClockRowLayout.requested(true, 400, 800, 1, true, false, true));
        assertFalse(CompactClockRowLayout.requested(false, 400, 800, 1, true, false, false));
    }

    @Test public void unknownScreenDoesNotCollapseTheClock() {
        assertFalse(CompactClockRowLayout.requested(true, 0, 800, 1, true, false, false));
        assertFalse(CompactClockRowLayout.requested(true, 400, 800, 0, true, false, false));
        assertFalse(CompactClockRowLayout.requested(true, Float.NaN, 800, 1, true, false, false));
    }

    @Test public void timeDefaultsBeforeDateWithinContentBounds() {
        CompactClockRowLayout.Row row = CompactClockRowLayout.place(16, 324, 50, 120, 8, true);
        assertNotNull(row);
        assertEquals(1, row.scale, 0.001f);
        assertEquals(81, row.timeLeft, 0.001f);
        assertEquals(139, row.dateLeft, 0.001f);
    }

    @Test public void placementOptionMovesTimeAfterDateWithoutChangingRowWidth() {
        CompactClockRowLayout.Row row = CompactClockRowLayout.place(16, 324, 50, 120, 8, false);
        assertEquals(81, row.dateLeft, 0.001f);
        assertEquals(209, row.timeLeft, 0.001f);
    }

    @Test public void longDateFitsWithoutTruncatingOrTouchingTheRail() {
        CompactClockRowLayout.Row row = CompactClockRowLayout.place(16, 284, 60, 360, 8, true);
        assertTrue(row.scale < 1);
        assertEquals(16, row.timeLeft, 0.001f);
        assertEquals(284, row.dateLeft + 360 * row.scale, 0.001f);
        assertEquals(8 * row.scale, row.dateLeft - row.timeLeft - row.timeWidth, 0.001f);
    }

    @Test public void replacementTimeAppearsOnlyAsTheNativeFaceArrives() {
        assertEquals(0, CompactClockRowLayout.alpha(0), 0.001f);
        assertEquals(0, CompactClockRowLayout.alpha(0.55f), 0.001f);
        assertEquals(0.5f, CompactClockRowLayout.alpha(0.775f), 0.001f);
        assertEquals(1, CompactClockRowLayout.alpha(1), 0.001f);
    }

    @Test public void reversingMidFlightStartsAtTheSameProgress() {
        assertEquals(0.6f, CompactClockRowLayout.progress(0.6f, false, 0), 0.001f);
        assertEquals(0.3f, CompactClockRowLayout.progress(0.6f, false, 0.5f), 0.001f);
        assertEquals(0, CompactClockRowLayout.progress(0.6f, false, 1), 0.001f);
        assertEquals(0.8f, CompactClockRowLayout.progress(0.6f, true, 0.5f), 0.001f);
        assertEquals(1, CompactClockRowLayout.progress(0.6f, true, 1.1f), 0.001f);
    }

    @Test public void incompleteDateGeometryKeepsTheOriginalFace() {
        assertNull(CompactClockRowLayout.place(100, 100, 50, 120, 8, true));
        assertNull(CompactClockRowLayout.place(0, 300, 50, 0, 8, true));
        assertNull(CompactClockRowLayout.place(0, 300, Float.NaN, 120, 8, true));
    }
}
