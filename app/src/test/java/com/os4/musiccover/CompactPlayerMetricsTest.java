package com.os4.musiccover;

import org.junit.Test;
import static org.junit.Assert.*;

public class CompactPlayerMetricsTest {
    @Test public void progressRowShrinksWithoutChangingItsVisibleTrack() {
        int height = CompactPlayerMetrics.cap(38, 32, 1);
        int padding = CompactPlayerMetrics.trackPadding(38, 16, height);
        assertEquals(32, height);
        assertEquals(38 - 2 * 16, height - 2 * padding);
        assertEquals(3, CompactPlayerMetrics.gap(6));
    }
    @Test public void buttonsKeepTheirGlyphAndFortyEightDpTouchHeight() {
        int height = CompactPlayerMetrics.cap(50, 48, 1);
        assertEquals(48, height);
        assertEquals(40, height - 2 * CompactPlayerMetrics.trackPadding(50, 5, height));
    }
    @Test public void alreadyCompactAndWrapContentDimensionsAreNeverExpanded() {
        assertEquals(40, CompactPlayerMetrics.cap(40, 44, 1));
        assertEquals(-2, CompactPlayerMetrics.cap(-2, 44, 1));
        assertEquals(120, CompactPlayerMetrics.cardMinimum(120, 1));
        assertEquals(138, CompactPlayerMetrics.cardMinimum(160, 1));
        assertEquals(0, CompactPlayerMetrics.cardMinimum(0, 1));
    }
    @Test public void physicalSizesFollowDensity() {
        assertEquals(121, CompactPlayerMetrics.cap(144, 44, 2.75f));
        assertEquals(132, CompactPlayerMetrics.cap(138, 48, 2.75f));
    }
}
