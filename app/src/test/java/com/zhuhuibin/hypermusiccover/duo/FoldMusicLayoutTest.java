package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class FoldMusicLayoutTest {
    private FoldMusicLayout.Columns columns(float w, float h, float d, int sw,
                                             float clock, float player, float shortcuts) {
        return FoldMusicLayout.columns(w, h, d, sw, 0, 24 * d, 0, 24 * d,
                clock, player, shortcuts);
    }

    @Test public void outerScreensAndInnerPortraitKeepTheRegularLayout() {
        assertNull(columns(400, 850, 1, 400, 120, 140, 760));
        assertNull(columns(850, 400, 1, 400, 80, 100, 340));
        assertNull(columns(650, 850, 1, 650, 120, 140, 760));
        assertNull(columns(700, 700, 1, 700, 120, 140, 620));
        // A wide display in a narrow/multi-window host must not use the physical display width.
        assertNull(columns(450, 600, 1, 700, 100, 100, 520));
    }

    @Test public void measuredInnerLandscapeHasSeparateArtworkPlayerAndLyricRegions() {
        FoldMusicLayout.Columns c = columns(850, 650, 1, 650, 110, 145, 560);
        assertNotNull(c);
        assertTrue(c.artworkRoom.left >= 16);
        assertEquals(16, c.player.top - c.artworkRoom.bottom, 0.001);
        assertTrue(c.lyrics.left - c.player.right >= 16);
        assertEquals((c.artworkRoom.left + c.artworkRoom.right) / 2,
                (c.player.left + c.player.right) / 2, .001);
        assertEquals(c.artworkRoom.width(), c.player.width(), .001);
        assertEquals(c.artworkRoom.top, c.lyrics.top, 0);
        assertEquals(c.player.bottom, c.lyrics.bottom, 0);
        assertTrue(c.player.bottom <= 544);
        assertTrue(c.lyrics.right <= 834);
        assertTrue(c.lyrics.height() > c.artworkRoom.height());
    }

    @Test public void sidebarSceneCanUseTheBottomRoomFreedByRelocatedButtons() {
        FoldMusicLayout.Columns c = columns(850, 650, 1, 650, 110, 145, Float.NaN);
        assertNotNull(c);
        assertEquals(610, c.player.bottom, .001);
        assertEquals(c.player.bottom, c.lyrics.bottom, .001);
        assertTrue(c.player.width() >= 240);
    }

    @Test public void coverUsesTheEntireLeftRoomWithoutEnteringTheControlsOrLyrics() {
        FoldMusicLayout.Columns c = columns(850, 650, 1, 650, 110, 145, 560);
        for (float aspect : new float[]{1, 16f / 9, 9f / 16}) {
            CoverCardStyle.Rect r = FoldMusicLayout.artwork(c.artworkRoom, aspect, 1);
            assertNotNull(r);
            CoverMorphMotion.Box drawn = CoverMorphMotion.cardBox(r.x, r.y, r.side, 1, aspect);
            assertTrue(drawn.x >= c.artworkRoom.left - 0.001);
            assertTrue(drawn.y >= c.artworkRoom.top - 0.001);
            assertTrue(drawn.x + drawn.w <= c.artworkRoom.right + 0.001);
            assertTrue(drawn.y + drawn.h <= c.artworkRoom.bottom + 0.001);
            assertEquals((c.artworkRoom.left + c.artworkRoom.right) / 2,
                    drawn.cx(), 0.001);
            assertEquals((c.artworkRoom.top + c.artworkRoom.bottom) / 2,
                    drawn.cy(), 0.001);
        }
    }

    @Test public void cutoutsAndNavigationInsetsBoundBothColumns() {
        FoldMusicLayout.Columns c = FoldMusicLayout.columns(950, 700, 1, 700,
                70, 40, 20, 36, 130, 140, Float.NaN);
        assertNotNull(c);
        assertEquals(86, c.artworkRoom.left, 0);
        assertTrue(c.player.left >= 86);
        assertEquals(914, c.lyrics.right, 0);
        assertEquals(648, c.player.bottom, 0);
        assertEquals(146, c.lyrics.top, 0);
    }

    @Test public void changingDensityDoesNotChangeTheDpArrangement() {
        FoldMusicLayout.Columns a = columns(850, 650, 1, 650, 110, 145, 560);
        FoldMusicLayout.Columns b = columns(2210, 1690, 2.6f, 650, 286, 377, 1456);
        assertNotNull(b);
        assertEquals(a.player.width(), b.player.width() / 2.6f, 0.001);
        assertEquals(a.player.top, b.player.top / 2.6f, 0.001);
        assertEquals(a.lyrics.left, b.lyrics.left / 2.6f, 0.001);
        assertEquals(a.lyrics.height(), b.lyrics.height() / 2.6f, 0.001);
    }

    @Test public void insufficientRoomAndUnknownGeometryNeverProduceAnOverlappingLayout() {
        assertNull(columns(850, 650, 1, 650, 400, 145, 560));
        assertNull(columns(850, 650, 1, 650, Float.NaN, 145, 560));
        assertNull(columns(850, 650, 1, 650, 110, 0, 560));
        assertNull(columns(850, 650, 1, 650, 110, 400, 560));
        assertNull(columns(Float.POSITIVE_INFINITY, 650, 1, 650, 110, 145, 560));
        assertNull(columns(850, 650, 0, 650, 110, 145, 560));
        assertNull(FoldMusicLayout.columns(850, 650, 1, 650,
                -1, 0, 0, 0, 110, 145, 560));
        assertNull(FoldMusicLayout.artwork(new FoldMusicLayout.Box(0, 0, 80, 80), 1, 1));
    }

    @Test public void fittingWindowsKeepAllRegionsInBoundsAcrossAspectRatios() {
        for (int w = 650; w <= 1200; w += 25) {
            for (int h = 450; h <= 900; h += 25) {
                FoldMusicLayout.Columns c = columns(w, h, 1, Math.min(w, h),
                        90, 130, h - 80);
                if (c == null) continue;
                assertTrue(c.player.left >= 16);
                assertTrue(c.lyrics.right <= w - 16);
                assertTrue(c.player.top > c.artworkRoom.bottom);
                assertTrue(c.player.right < c.lyrics.left);
                assertTrue(c.player.bottom <= h - 96);
                assertTrue(c.artworkRoom.top > 90);
                assertTrue(c.lyrics.width() >= 260);
            }
        }
    }
}
