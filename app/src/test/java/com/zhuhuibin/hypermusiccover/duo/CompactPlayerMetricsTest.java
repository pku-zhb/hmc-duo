package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class CompactPlayerMetricsTest {
    private CompactPlayerMetrics.Plan normal(int percent) {
        return CompactPlayerMetrics.plan(371, percent, 32, 22, 4, 32, 281, 105, 2f);
    }

    @Test public void defaultPreservesNativeGeometry() {
        CompactPlayerMetrics.Plan p = normal(100);
        assertEquals(371, p.height);
        assertEquals(105, p.artwork);
        assertEquals(32, p.top);
        assertEquals(22, p.action);
        assertEquals(4, p.seekTop);
        assertEquals(32, p.seekBottom);
    }

    @Test public void compactHeaderAndInterRowGapsProtectOuterBreathingRoom() {
        CompactPlayerMetrics.Plan p = normal(85);
        assertEquals(279, p.height);
        assertEquals(80, p.artwork);
        assertTrue(p.top >= 22);
        assertEquals(0, p.seekBottom);
        assertTrue(p.action + p.seekTop <= 3);
        assertEquals(371 - p.height, 105 - p.artwork + 32 - p.top + 22 - p.action
                + 4 - p.seekTop + 32 - p.seekBottom);
    }

    @Test public void seventyFivePercentKeepsPlaybackAndSeekRowsAndAReadableHeader() {
        CompactPlayerMetrics.Plan p = normal(75);
        assertEquals(279, p.height);
        assertEquals(80, p.artwork);
        assertTrue(p.top >= 8);
        assertEquals(0, p.seekBottom);
        assertTrue(p.action >= 0 && p.seekTop >= 0);
        // 50dp playback and 38dp seek remain unchanged; every saved pixel is accounted for.
        assertEquals(p.height, p.artwork + 100 + 76 + p.top + p.action + p.seekTop + p.seekBottom);
    }

    @Test public void aSmallUnknownNativeVariantRetainsItsRows() {
        CompactPlayerMetrics.Plan p = CompactPlayerMetrics.plan(276, 75, 32, 22, 4, 32, 281, 105, 2f);
        assertEquals(276, p.height);
        assertEquals(105, p.artwork);
        assertEquals(32, p.top);
    }

    @Test public void alreadyTightOrNegativeGapsAreNotEnlarged() {
        CompactPlayerMetrics.Plan p = CompactPlayerMetrics.plan(180, 75, 2, 1, -2, 3, 150, 40, 1f);
        assertEquals(179, p.height);
        assertEquals(-2, p.seekTop);
        assertEquals(2, p.top);
        assertEquals(3, p.seekBottom);
    }

    @Test public void unsupportedDimensionsStayNative() {
        assertEquals(-2, CompactPlayerMetrics.plan(-2, 75, 16, 11, 2, 16, 140, 52, 1f).height);
        assertEquals(185, CompactPlayerMetrics.plan(185, 75, 16, 11, 2, 16, 140, 52, 0f).height);
    }

    @Test public void heightAndArtworkAreMonotonicAndDoNotAccumulateCompression() {
        int previous = normal(75).height;
        int art = normal(75).artwork;
        for (int percent = 76; percent <= 100; ++percent) {
            CompactPlayerMetrics.Plan p = normal(percent);
            assertTrue(p.height >= previous);
            assertTrue(p.artwork >= art);
            assertEquals(p.height, normal(percent).height);
            previous = p.height; art = p.artwork;
        }
        assertEquals(normal(75).height, normal(-500).height);
        assertEquals(normal(100).height, normal(500).height);
    }

    @Test public void nativeAnimationAndFinalCompactHeightShareBothEndpoints() {
        for (int percent : new int[]{75, 85, 99, 100}) {
            int height = normal(percent).height;
            assertEquals(height, CompactPlayerMetrics.transitionHeight(371, 371, 276, height, 276));
            assertEquals(276, CompactPlayerMetrics.transitionHeight(276, 371, 276, height, 276));
            int previous = 276;
            for (int nativeFrame = 276; nativeFrame <= 371; ++nativeFrame) {
                int frame = CompactPlayerMetrics.transitionHeight(nativeFrame, 371, 276, height, 276);
                assertTrue(frame >= previous);
                previous = frame;
            }
        }
        assertEquals(0, CompactPlayerMetrics.transitionHeight(0, 371, 276, 316, 276));
    }

    private int aod(int percent) {
        return CompactPlayerMetrics.aodHeight(276, normal(percent), 105, 32, 22, 100, 100, 0, 2f);
    }

    @Test public void aodBudgetDropsHiddenSeekSpaceAndScalesItsOwnBottomGap() {
        assertEquals(276, aod(100));
        assertEquals(226, aod(85));
        assertEquals(226, aod(75));
        assertTrue(normal(75).height - aod(75) >= 50);
        int[] values = {75, 85, 100};
        for (int percent : values) {
            CompactPlayerMetrics.Plan p = normal(percent);
            int bottom = aod(percent) - p.top - p.artwork - p.action - 100;
            assertTrue(bottom >= 17 && bottom <= 23);
            assertTrue(aod(percent) < p.height);
        }
    }

    @Test public void aodUsesTheSameCompactEndpointAtEveryHeightSetting() {
        int previous = 0;
        for (int percent = 75; percent <= 100; ++percent) {
            CompactPlayerMetrics.Plan p = normal(percent);
            int end = aod(percent);
            assertTrue(end >= previous);
            assertEquals(end, CompactPlayerMetrics.transitionHeight(276, 371, 276, p.height, end));
            assertEquals(p.height, CompactPlayerMetrics.transitionHeight(371, 371, 276, p.height, end));
            previous = end;
        }
    }

    @Test public void unknownAodGeometryCannotInventAnUnsupportedHeight() {
        assertEquals(80, CompactPlayerMetrics.aodHeight(80, normal(75), 105, 32, 22, 100, 100, 0, 2f));
        assertEquals(276, CompactPlayerMetrics.aodHeight(276, normal(75), 105, 32, 22, 100, 100, 0, 0));
        assertEquals(276, CompactPlayerMetrics.aodHeight(276, null, 105, 32, 22, 100, 100, 0, 2f));
    }

    @Test public void onlyCompactAndNativePresetsExistInsideTheGeometryConsumer() {
        for (int percent : new int[]{-100, 75, 85, 87}) {
            assertEquals(normal(75).height, normal(percent).height);
            assertEquals(normal(75).artwork, normal(percent).artwork);
        }
        for (int percent : new int[]{88, 99, 100, 999})
            assertEquals(normal(100).height, normal(percent).height);
    }

    @Test public void largeTextRaisesSafetyHeightWithoutInterpolatingAPercentage() {
        CompactPlayerMetrics.Plan p = CompactPlayerMetrics.withHeaderFloor(normal(75), 90, 371, 105);
        assertEquals(90, p.artwork);
        assertEquals(289, p.height);
        assertEquals(normal(75).top, p.top);
        assertEquals(0, p.action + p.seekTop + p.seekBottom);
        assertEquals(105, CompactPlayerMetrics.withHeaderFloor(normal(100), 150, 371, 105).artwork);
    }

    @Test public void pixelRowsHaveMatchedVisibleGapsWithoutOverlappingTouchRows() {
        CompactPlayerMetrics.Plan p = normal(75);
        assertEquals(23, p.top);
        assertEquals(0, p.action + p.seekTop + p.seekBottom);
        int actionShift = CompactPlayerMetrics.actionContentShift(75, 10, 2);
        assertEquals(10, actionShift);
        for (int glyphHeight : new int[]{48, 64, 80}) for (int seekPixels : new int[]{12, 24, 32}) {
            int seekShift = CompactPlayerMetrics.seekContentShift(75, 76, seekPixels,
                    actionShift, 32, 2);
            int glyphTop = (100 - glyphHeight) / 2 + actionShift;
            int glyphBottom = glyphTop + glyphHeight;
            int seekTop = (76 - seekPixels) / 2 - seekShift;
            assertEquals(glyphTop, 100 + seekTop - glyphBottom);
            assertTrue(glyphTop >= 0 && glyphBottom <= 100);
            assertTrue(seekTop >= 0 && seekTop + seekPixels <= 76);
            // All three touch rectangles still fit in the shell and remain disjoint.
            assertEquals(p.height, p.top + p.artwork + 100 + 76);
        }
    }

    @Test public void aodMatchesItsVisibleBottomToTopForDifferentPlaybackGlyphSizes() {
        CompactPlayerMetrics.Plan p = normal(75);
        for (int glyphHeight : new int[]{48, 64, 80}) {
            int glyphBottom = (100 + glyphHeight) / 2 + 10;
            int shell = CompactPlayerMetrics.aodHeight(276, p, 105, 32, 22, 100, glyphBottom, 0, 2f);
            int lastPixel = p.top + p.artwork + p.action + glyphBottom;
            assertEquals(p.top, shell - lastPixel);
            assertTrue(shell >= p.top + p.artwork + 100);
        }
    }

    @Test public void nativeAndTightPixelRowsNeverLoseTheirPaddingOrTouchArea() {
        assertEquals(0, CompactPlayerMetrics.actionContentShift(100, 10, 2));
        assertEquals(0, CompactPlayerMetrics.seekContentShift(100, 76, 32, 0, 32, 2));
        assertEquals(2, CompactPlayerMetrics.actionContentShift(75, 2, 2));
        assertEquals(0, CompactPlayerMetrics.seekContentShift(75, 76, 32, 10, 32, 0));
        int shift = CompactPlayerMetrics.actionContentShift(75, 10, 2);
        assertEquals(20, (10 + shift) + (10 - shift));
        assertTrue(10 - shift >= 0);
    }

    @Test public void onlyInnerLandscapeUsesTheSeparateProfile() {
        assertFalse(CompactPlayerMetrics.innerLandscape(1000, 2300, 3f));
        assertFalse(CompactPlayerMetrics.innerLandscape(1800, 2200, 2.5f));
        assertTrue(CompactPlayerMetrics.innerLandscape(2200, 1800, 2.5f));
        assertFalse(CompactPlayerMetrics.innerLandscape(2200, 1000, 3f));
        assertFalse(CompactPlayerMetrics.innerLandscape(2200, 1800, 0f));
    }
}
