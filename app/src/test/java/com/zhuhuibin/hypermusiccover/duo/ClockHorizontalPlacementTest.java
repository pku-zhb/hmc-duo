package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class ClockHorizontalPlacementTest {
    @Test public void outerClockInkFitsTheReservedContentInsteadOfBeingClipped() {
        float left = 138.59003f, right = 1029.3958f, pivot = 584, center = 953.75f / 2;
        float tx = ClockHorizontalPlacement.translation(left, right, pivot, 1, 0, center);
        assertEquals(-107.1179f, tx, .001f);
        assertTrue(left + tx >= 0);
        assertTrue(right + tx <= 953.75f);
        assertEquals(center, (left + right) / 2 + tx, .001f);
    }

    @Test public void collapsedAndMirroredClocksUseTheirActualPivotAndScale() {
        for (float scale : new float[]{1, .18f, -1}) {
            float tx = ClockHorizontalPlacement.translation(138.59f, 1029.3958f, 584, scale, 12, 470);
            float center = 12 + 584 + ((138.59f + 1029.3958f) / 2 - 584) * scale + tx;
            assertEquals(470, center, .001f);
        }
    }

    @Test public void clockLayersWithDifferentParentsLandAtTheSameScreenCenter() {
        float hour = ClockHorizontalPlacement.translation(138.59f, 1029.3958f, 584, 1, 0, 476.875f);
        float minute = ClockHorizontalPlacement.translation(138.59f, 1029.3958f, 584, 1, 0, 456.875f);
        assertEquals(hour, 20 + minute, .001f);
        assertEquals(0, ClockHorizontalPlacement.translation(0, 400, 200, 1, 0, 200), 0);
    }

    @Test public void coverScaleAndNativeHandoffShareTheBiasedCenterUnderAncestorZoom() {
        float railCenter = 953.75f / 2 + ClockHorizontalPlacement.opticalOffset(1168, 2520, 3);
        float parentLeft = 21, ancestorScale = .95f, viewLeft = 12, pivot = 584;
        float centerInParent = (railCenter - parentLeft) / ancestorScale;
        float inkCenter = (138.59f + 1029.3958f) / 2;
        for (float scale : new float[]{.18f, .45f, .9f, 1f}) {
            float tx = ClockHorizontalPlacement.translation(138.59f, 1029.3958f, pivot,
                    scale, viewLeft, centerInParent);
            float drawnCenter = parentLeft + ancestorScale *
                    (viewLeft + pivot + (inkCenter - pivot) * scale + tx);
            assertEquals(railCenter, drawnCenter, .001f);
        }
    }

    @Test public void opticalOffsetIsEightDpOnlyOnTheOuterPortraitScreen() {
        assertEquals(8, ClockHorizontalPlacement.opticalOffset(390, 844, 1), 0);
        assertEquals(24, ClockHorizontalPlacement.opticalOffset(1170, 2532, 3), 0);
        assertEquals(0, ClockHorizontalPlacement.opticalOffset(1800, 2400, 3), 0);
        assertEquals(0, ClockHorizontalPlacement.opticalOffset(2520, 1168, 3), 0);
        assertEquals(0, ClockHorizontalPlacement.opticalOffset(500, 390, 1), 0);
        assertEquals(0, ClockHorizontalPlacement.opticalOffset(0, 844, 1), 0);
        assertEquals(0, ClockHorizontalPlacement.opticalOffset(390, 844, Float.NaN), 0);
    }

    @Test public void unavailableGlyphMeasurementsDoNotWriteTransforms() {
        assertTrue(Float.isNaN(ClockHorizontalPlacement.translation(Float.NaN, 3, 0, 1, 0, 4)));
        assertTrue(Float.isNaN(ClockHorizontalPlacement.translation(3, 3, 0, 1, 0, 4)));
        assertTrue(Float.isNaN(ClockHorizontalPlacement.translation(0, 3, 0, 0, 0, 4)));
    }
}
