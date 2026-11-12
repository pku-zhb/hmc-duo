package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class FoldMusicLayoutTest {
    private FoldMusicLayout.Box player(float density) {
        return new FoldMusicLayout.Box(16 * density, 350 * density, 469 * density, 495 * density);
    }
    private FoldMusicLayout.Columns columns(float w, float h, float d, int sw,
                                             float clock, FoldMusicLayout.Box player, float shortcuts) {
        return FoldMusicLayout.columns(w, h, d, sw, 0, 24 * d, 0, 24 * d,
                clock, player, shortcuts);
    }

    @Test public void outerScreensAndInnerPortraitKeepTheRegularLayout() {
        assertNull(columns(400, 850, 1, 400, 110, player(1), 760));
        assertNull(columns(850, 400, 1, 400, 80, player(1), 340));
        assertNull(columns(650, 850, 1, 650, 110, player(1), 760));
        assertNull(columns(700, 700, 1, 700, 110, player(1), 620));
        assertNull(columns(450, 600, 1, 700, 100, player(1), 520));
    }

    @Test public void mediaNeverLeavesItsNativeHeaderAndLiveRowsKeepTheirVerticalOrder() {
        FoldMusicLayout.Box nativePlayer = player(1);
        FoldMusicLayout.Columns c = columns(850, 650, 1, 650, 110, nativePlayer, 560);
        assertNotNull(c);
        assertSame(nativePlayer, c.player);
        assertEquals(350, c.player.top, 0);
        assertEquals(495, c.player.bottom, 0);
        assertEquals(453, c.player.width(), 0);
        assertEquals(16, c.player.top - c.artworkRoom.bottom, .001);
        assertEquals(c.player.left, c.artworkRoom.left, 0);
        assertEquals(c.player.right, c.artworkRoom.right, 0);
        assertTrue(c.lyrics.left >= c.player.right + 16);
        // A native live card starting after the header is not covered by a relocated media child.
        assertTrue(c.player.bottom <= 511);
        assertTrue(c.artworkRoom.bottom < 511);
    }

    @Test public void reservedRailDoesNotTurnAValidLandscapeWindowIntoPortrait() {
        assertTrue(FoldMusicLayout.wideWindow(750, 650, 1, 650));
        FoldMusicLayout.Box nativePlayer = new FoldMusicLayout.Box(11.25f, 350, 383, 495);
        FoldMusicLayout.Columns c = columns(675, 650, 1, 650, 110, nativePlayer, 560);
        assertNotNull(c);
        assertSame(nativePlayer, c.player);
        assertEquals(260f, c.lyrics.width(), .001f);
    }

    @Test public void sidebarFreedRoomExtendsLyricsWithoutMovingTheNativePlayer() {
        FoldMusicLayout.Columns a = columns(850, 650, 1, 650, 110, player(1), 560);
        FoldMusicLayout.Columns b = columns(850, 650, 1, 650, 110, player(1), Float.NaN);
        assertEquals(544, a.lyrics.bottom, 0);
        assertEquals(610, b.lyrics.bottom, 0);
        assertEquals(a.player.top, b.player.top, 0);
        assertEquals(a.player.bottom, b.player.bottom, 0);
        assertEquals(a.artworkRoom.bottom, b.artworkRoom.bottom, 0);
    }

    @Test public void coverNeverEntersTheNativeHeaderOrLyricsForAnyArtworkAspect() {
        FoldMusicLayout.Columns c = columns(850, 650, 1, 650, 110, player(1), 560);
        for (float aspect : new float[]{1, 16f / 9, 9f / 16}) {
            CoverCardStyle.Rect r = FoldMusicLayout.artwork(c.artworkRoom, aspect, 1);
            assertNotNull(r);
            CoverMorphMotion.Box drawn = CoverMorphMotion.cardBox(r.x, r.y, r.side, 1, aspect);
            assertTrue(drawn.x >= c.artworkRoom.left - .001);
            assertTrue(drawn.y >= c.artworkRoom.top - .001);
            assertTrue(drawn.x + drawn.w <= c.artworkRoom.right + .001);
            assertTrue(drawn.y + drawn.h <= c.artworkRoom.bottom + .001);
            assertTrue(drawn.y + drawn.h < c.player.top);
        }
    }

    @Test public void nativeHeaderBeforeTheOldBottomSlotUsesItsActualTop() {
        // Captured inner window: the old code moved a child 287 px below its clipping header.
        FoldMusicLayout.Box nativePlayer = new FoldMusicLayout.Box(31, 832, 1277, 1341);
        FoldMusicLayout.Columns c = FoldMusicLayout.columns(2157.75f, 1672, 2.75f, 608,
                0, 164, 0, 0, 358.1207f, nativePlayer, Float.NaN);
        assertNotNull(c);
        assertSame(nativePlayer, c.player);
        assertEquals(788, c.artworkRoom.bottom, .001);
        assertEquals(1321, c.lyrics.left, .001);
        assertEquals(2113.75f, c.lyrics.right, .001);
        assertEquals(1628, c.lyrics.bottom, .001);
    }

    @Test public void changingDensityDoesNotChangeTheDpArrangement() {
        FoldMusicLayout.Columns a = columns(850, 650, 1, 650, 110, player(1), 560);
        FoldMusicLayout.Columns b = columns(2210, 1690, 2.6f, 650, 286, player(2.6f), 1456);
        assertNotNull(b);
        assertEquals(a.player.width(), b.player.width() / 2.6f, .001);
        assertEquals(a.player.top, b.player.top / 2.6f, .001);
        assertEquals(a.lyrics.left, b.lyrics.left / 2.6f, .001);
        assertEquals(a.lyrics.height(), b.lyrics.height() / 2.6f, .001);
    }

    @Test public void insufficientRoomWaitsWithoutTryingToMoveOrResizeTheHeader() {
        assertNull(columns(850, 650, 1, 650, 400, player(1), 560));
        assertNull(columns(850, 650, 1, 650, Float.NaN, player(1), 560));
        assertNull(columns(850, 650, 1, 650, 110, null, 560));
        assertNull(columns(850, 650, 1, 650, 110, new FoldMusicLayout.Box(16, 350, 800, 495), 560));
        assertNull(columns(Float.POSITIVE_INFINITY, 650, 1, 650, 110, player(1), 560));
        assertNull(columns(850, 650, 0, 650, 110, player(1), 560));
        assertNull(FoldMusicLayout.artwork(new FoldMusicLayout.Box(0, 0, 80, 80), 1, 1));
    }

    @Test public void scrollingTheNativeHeaderCannotDisplaceItIntoLiveOrNotificationRows() {
        for (int top = 0; top < 650; top += 10) {
            FoldMusicLayout.Box header = new FoldMusicLayout.Box(16, top, 469, top + 145);
            FoldMusicLayout.Columns c = columns(850, 650, 1, 650, 110, header, 560);
            if (c == null) continue;
            assertSame(header, c.player);
            assertTrue(c.artworkRoom.bottom < top || c.artworkRoom.left > header.right);
            assertTrue(c.lyrics.left > header.right);
            assertEquals(top + 145, c.player.bottom, 0);
        }
    }
    @Test public void fullyExpandedNativeStackGetsBothScenesInTheFreeRightColumn() {
        FoldMusicLayout.Box header = new FoldMusicLayout.Box(16, 145, 469, 290);
        FoldMusicLayout.Columns c = columns(850, 650, 1, 650, 110, header, 560);
        assertNotNull(c);
        assertSame(header, c.player);
        assertTrue(c.artworkRoom.left > header.right);
        assertTrue(c.lyrics.left > header.right);
        assertTrue(c.artworkRoom.height() >= 120);
        assertTrue(c.lyrics.height() >= 160);
        assertEquals(16, c.lyrics.top - c.artworkRoom.bottom, .001);
        assertNotNull(FoldMusicLayout.artwork(c.artworkRoom, 1, 1));
    }

}
