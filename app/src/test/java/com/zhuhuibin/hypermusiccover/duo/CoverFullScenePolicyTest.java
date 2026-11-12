package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class CoverFullScenePolicyTest {
    @Test public void squareArtworkFillsWidthInsteadOfFittingBetweenClockAndPlayer() {
        CoverFullScenePolicy.Band band = CoverFullScenePolicy.band(1200, 2608, 600, 600, .2f);
        assertEquals(0f, band.left, .001f);
        assertEquals(1200f, band.width, .001f);
        assertEquals(1200f, band.height, .001f);
        assertEquals(281.6f, band.top, .001f);
    }

    @Test public void panoramaKeepsItsUncroppedSourceAspect() {
        CoverFullScenePolicy.Band band = CoverFullScenePolicy.band(1200, 2608, 3000, 500, .5f);
        assertEquals(200f, band.height, .001f);
        assertEquals(1204f, band.top, .001f);
    }

    @Test public void landscapeViewportClipsTallBandRatherThanShrinkingItIntoACard() {
        CoverFullScenePolicy.Band band = CoverFullScenePolicy.band(2200, 1800, 600, 900, .5f);
        assertEquals(3300f, band.height, .001f);
        assertEquals(-750f, band.top, .001f);
    }

    @Test public void invalidSizesAndBiasCannotBreakComposition() {
        assertNull(CoverFullScenePolicy.band(0, 200, 100, 100, .5f));
        assertNull(CoverFullScenePolicy.band(100, 200, 0, 100, .5f));
        assertEquals(50f, CoverFullScenePolicy.band(100, 200, 100, 100, Float.NaN).top, 0f);
        assertEquals(0f, CoverFullScenePolicy.band(100, 200, 100, 100, -1f).top, 0f);
        assertEquals(100f, CoverFullScenePolicy.band(100, 200, 100, 100, 2f).top, 0f);
    }

    @Test public void onlyExplicitCombinedPageOverridesFullScreenPresentation() {
        assertTrue(CoverFullScenePolicy.fullScene(CoverCardStyle.FULL, false));
        assertFalse(CoverFullScenePolicy.fullScene(CoverCardStyle.FULL, true));
        assertFalse(CoverFullScenePolicy.fullScene(CoverCardStyle.CARD, false));
    }

    @Test public void featherUsesScreenPixelsAcrossPreparedBitmapSizes() {
        assertEquals(120, CoverFullScenePolicy.feather(600, 600, 1200));
        assertEquals(40, CoverFullScenePolicy.feather(600, 160, 1200));
    }
}
