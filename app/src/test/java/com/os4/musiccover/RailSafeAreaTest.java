package com.os4.musiccover;

import org.junit.Test;
import static org.junit.Assert.*;

public class RailSafeAreaTest {
    @Test public void observedInnerLandscapeCameraDoesNotExcludeTheEntireRightEdge() {
        FoldMusicLayout.Box safe = RailSafeArea.resolve(2364, 1672, 0, 140, 0, 0,
                new FoldMusicLayout.Box[]{new FoldMusicLayout.Box(2224, 0, 2364, 164)});
        assertEquals(164, safe.top, 0);
        assertEquals(2364, safe.right, 0);
        RightRailLayout.Plan p = RightRailLayout.plan(2364, 1672, 2.75f,
                safe.left, safe.top, 2364 - safe.right, 1672 - safe.bottom,
                new FoldMusicLayout.Box(2192, 1328, 2324, 1460),
                new FoldMusicLayout.Box(2192, 1478, 2324, 1610));
        assertNotNull(p);
        assertEquals(46 * 2.75f, p.pill.width(), .001f);
        assertTrue(p.companion.top > 164);
        assertTrue(p.pill.bottom < p.controlsY - p.controlsSpan / 2 - 66);
    }
    @Test public void outerTopNotchAndBottomBarsStillLeaveSafeMargins() {
        FoldMusicLayout.Box safe = RailSafeArea.resolve(1168, 1712, 0, 100, 0, 66,
                new FoldMusicLayout.Box[]{new FoldMusicLayout.Box(557, 0, 611, 126)});
        assertEquals(126, safe.top, 0);
        assertEquals(1646, safe.bottom, 0);
        assertEquals(1168, safe.right, 0);
    }
    @Test public void tallSideCutoutStillReservesTheAffectedEdge() {
        FoldMusicLayout.Box safe = RailSafeArea.resolve(800, 600, 0, 24, 0, 24,
                new FoldMusicLayout.Box[]{new FoldMusicLayout.Box(760, 120, 800, 450)});
        assertEquals(760, safe.right, 0);
        assertEquals(24, safe.top, 0);
    }
}
