package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class FoldDateLayoutTest {
    @Test public void bothInnerRotationsUseTheSameHigherLineWithoutOuterCameraClearance() {
        for (float density : new float[]{1, 1.5f, 2.75f, 3}) {
            assertEquals(24 * density, FoldDateLayout.line(608 * density, 860 * density,
                    density, 60 * density), .001f);
            assertEquals(24 * density, FoldDateLayout.line(860 * density, 608 * density,
                    density, 60 * density), .001f);
        }
    }
    @Test public void outerClockKeepsItsAcceptedCutoutClearance() {
        assertEquals(64, FoldDateLayout.line(425, 700, 1, 46), 0);
        assertEquals(88, FoldDateLayout.line(425, 700, 1, 80), 0);
        assertFalse(FoldDateLayout.inner(425, 700, 1));
    }
    @Test public void invalidDisplayDoesNotPublishADateEndpoint() {
        assertTrue(Float.isNaN(FoldDateLayout.line(Float.NaN, 700, 1, 46)));
        assertTrue(Float.isNaN(FoldDateLayout.line(425, 700, 0, 46)));
    }
}
