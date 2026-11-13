package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class CompactPlayerMetricsTest {
    private CompactPlayerMetrics.Plan normal(int percent) {
        // Retained OEM normal layout: 185dp shell; 52.5dp artwork, 50dp actions,
        // 38dp seek touch area. Density 2 preserves the half-dp native artwork.
        return CompactPlayerMetrics.plan(370, percent, 32, 22, 4, 32, 281, 2f);
    }

    @Test public void defaultPreservesNativeHeightAndEveryGap() {
        CompactPlayerMetrics.Plan p = normal(100);
        assertEquals(370, p.height);
        assertEquals(32, p.top);
        assertEquals(22, p.action);
        assertEquals(4, p.seekTop);
        assertEquals(32, p.seekBottom);
    }

    @Test public void minimumHeightComesOnlyFromAvailableWhitespace() {
        CompactPlayerMetrics.Plan p = normal(85);
        assertEquals(315, p.height);
        assertEquals(370 - p.height, 32 - p.top + 22 - p.action + 4 - p.seekTop + 32 - p.seekBottom);
        assertTrue(p.top >= 8);
        assertTrue(p.action >= 4);
        assertTrue(p.seekTop >= 4);
        assertTrue(p.seekBottom >= 8);
        assertTrue(p.height >= 281 + p.top + p.action + p.seekTop + p.seekBottom - 1);
    }

    @Test public void aSmallNativeAodVariantDoesNotSqueezeItsControlsToMeetTheSlider() {
        CompactPlayerMetrics.Plan p = CompactPlayerMetrics.plan(276, 85, 32, 22, 4, 32, 281, 2f);
        assertEquals(276, p.height);
        assertEquals(32, p.top);
        assertEquals(22, p.action);
    }

    @Test public void alreadyTightOrNegativeNativeGapsAreNotEnlarged() {
        CompactPlayerMetrics.Plan p = CompactPlayerMetrics.plan(180, 85, 2, 1, -2, 3, 150, 1f);
        assertEquals(180, p.height);
        assertEquals(2, p.top);
        assertEquals(1, p.action);
        assertEquals(-2, p.seekTop);
        assertEquals(3, p.seekBottom);
    }

    @Test public void unsupportedBackgroundHeightOrDensityStaysNative() {
        assertEquals(-2, CompactPlayerMetrics.plan(-2, 85, 16, 11, 2, 16, 140, 1f).height);
        assertEquals(185, CompactPlayerMetrics.plan(185, 85, 16, 11, 2, 16, 140, 0f).height);
    }

    @Test public void heightReducesMonotonicallyWithoutCumulativeCompression() {
        int previous = normal(85).height;
        for (int percent = 86; percent <= 100; ++percent) {
            CompactPlayerMetrics.Plan p = normal(percent);
            assertTrue(p.height >= previous);
            assertEquals(p.height, normal(percent).height);
            previous = p.height;
        }
        assertEquals(normal(85).height, normal(-500).height);
        assertEquals(normal(100).height, normal(500).height);
    }

    @Test public void onlyInnerLandscapeUsesTheSeparateProfile() {
        assertFalse(CompactPlayerMetrics.innerLandscape(1000, 2300, 3f));
        assertFalse(CompactPlayerMetrics.innerLandscape(1800, 2200, 2.5f));
        assertTrue(CompactPlayerMetrics.innerLandscape(2200, 1800, 2.5f));
        assertFalse(CompactPlayerMetrics.innerLandscape(2200, 1000, 3f));
        assertFalse(CompactPlayerMetrics.innerLandscape(2200, 1800, 0f));
    }
}
