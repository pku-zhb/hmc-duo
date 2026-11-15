package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class FoldMusicLayoutTest {
    private FoldMusicLayout.Columns columns(float width, float height, float density,
                                             float top, float playerHeight) {
        return FoldMusicLayout.columns(width, height, density, 650,
                0, 24 * density, 0, 24 * density, 80 * density,
                new FoldMusicLayout.Box(31 * density, top, 484 * density, top + playerHeight), Float.NaN);
    }

    @Test public void combinedPageRequiresAnInnerLandscapeWindow() {
        assertFalse(FoldMusicLayout.wideWindow(400, 850, 1, 400));
        assertFalse(FoldMusicLayout.wideWindow(650, 850, 1, 650));
        assertFalse(FoldMusicLayout.wideWindow(700, 700, 1, 700));
        assertTrue(FoldMusicLayout.wideWindow(850, 650, 1, 650));
        assertTrue(FoldMusicLayout.wideWindow(2364, 1672, 2.75f, 608));
    }

    @Test public void sidebarExcludedAreaIsSplitEquallyRegardlessOfOldNativeWidth() {
        for (float oldWidth : new float[]{240, 453, 760}) {
            FoldMusicLayout.Columns c = FoldMusicLayout.columns(775, 650, 1, 650,
                    0, 24, 0, 24, 80, new FoldMusicLayout.Box(31, 350, 31 + oldWidth, 495), Float.NaN);
            assertNotNull(c);
            float allocatedColumn = (775 - 3 * 16) / 2f;
            assertEquals(allocatedColumn, c.lyrics.width(), .001f);
            assertEquals(16 + allocatedColumn / 2f, (c.player.left + c.player.right) / 2f, .001f);
            assertEquals(16 + allocatedColumn + 16, c.lyrics.left, .001f);
            assertTrue(c.player.right < c.lyrics.left);
            assertEquals(350, c.player.top, 0);
            assertEquals(495, c.player.bottom, 0);
        }
    }

    @Test public void playerAndSquareArtworkMatchInBothHeightPresets() {
        for (int playerHeight : new int[]{110, 145}) {
            FoldMusicLayout.Columns c = columns(775, 650, 1, 350, playerHeight);
            assertNotNull(c);
            CoverCardStyle.Rect art = FoldMusicLayout.artwork(c.artworkRoom, 1, 1);
            assertNotNull(art);
            assertEquals(c.player.width(), art.side, .001f);
            assertEquals(c.player.left, art.x, .001f);
            assertEquals(playerHeight, c.player.height(), 0);
            assertTrue(art.y + art.side <= c.player.top - 16);
        }
    }

    @Test public void capturedInnerLandscapeUsesBothColumnsWithoutBorrowingPlayerWidth() {
        FoldMusicLayout.Columns c = FoldMusicLayout.columns(2157.75f, 1672, 2.75f, 608,
                0, 66, 0, 0, 180, new FoldMusicLayout.Box(559, 832, 1805, 1341), Float.NaN);
        assertNotNull(c);
        assertEquals(1012.875f, c.lyrics.width(), .001f);
        assertEquals(531, c.player.width(), .001f);
        assertEquals(832, c.player.top, .001f);
        assertEquals(1341, c.player.bottom, .001f);
        assertEquals(788, c.artworkRoom.bottom, .001f);
        assertEquals(c.player.width(), FoldMusicLayout.artwork(c.artworkRoom, 1, 2.75f).side, .001f);
    }

    @Test public void fullHeightPlayerReplacesTheCapturedAodFrameAfterWakeMotionEnds() {
        FoldMusicLayout.Columns captured = capturedColumns(1247.1956f, 380.86096f, 954f, 1f);
        assertNotNull(captured);
        assertEquals(953.5048f, captured.player.width(), .001f);
        assertEquals(380.86096f, captured.player.height(), .001f);
        assertTrue(FoldMusicLayout.retainColumns(false, true, false));
        assertTrue(FoldMusicLayout.retainColumns(true, false, true));
        // The clock's held flag can still be true here. It is not a geometry-retention input.
        assertFalse(FoldMusicLayout.retainColumns(true, false, false));
        for (float top : new float[]{1128f, 1130f}) {
            FoldMusicLayout.Columns awake = capturedColumns(top, 511f, 954f, 1f);
            assertNotNull(awake);
            assertEquals(top, awake.player.top, .001f);
            assertEquals(511f, awake.player.height(), .001f);
            assertTrue(awake.player.width() < captured.player.width());
            CoverCardStyle.Rect artwork = FoldMusicLayout.artwork(awake.artworkRoom, 1f, 2.75f);
            assertEquals(44f, awake.player.top - artwork.y - artwork.side, .001f);
            assertEquals(awake.player.width(), artwork.side, .001f);
            assertEquals((awake.player.left + awake.player.right) / 2f,
                    artwork.x + artwork.side / 2f, .001f);
        }
    }

    @Test public void fullHeightFramesKeepOneLeadingInsetAndIgnoreThePreviouslyAssignedWidth() {
        for (float scale : new float[]{.5f, 1f, 1.5f}) {
            for (float top : new float[]{1128f, 1130f}) {
                for (float oldWidth : new float[]{841f, 954f, 1246f, 1805f, 841f}) {
                    FoldMusicLayout.Columns plan = capturedColumns(top, 511f, oldWidth, scale);
                    assertNotNull(plan);
                    CoverCardStyle.Rect artwork = FoldMusicLayout.artwork(plan.artworkRoom, 1f, 2.75f*scale);
                    assertEquals(575.1875f, (plan.player.left + plan.player.right) / (2f*scale), .001f);
                    assertEquals(plan.player.left, artwork.x, .001f);
                    assertEquals(plan.player.width(), artwork.side, .001f);
                    assertEquals(44f*scale, plan.player.top - artwork.y - artwork.side, .001f);
                    assertEquals(1117.375f, plan.lyrics.left / scale, .001f);
                }
            }
        }
    }

    @Test public void nativePlayerSafetyUsesTheWindowWhileLyricsKeepTheirOwnBottomAir() {
        FoldMusicLayout.Columns safe = columns(775f, 650f, 1f, 350f, 270f);
        assertNotNull(safe);
        assertTrue(safe.player.bottom > safe.lyrics.bottom);
        assertEquals(598f, safe.lyrics.bottom, .001f);
        assertEquals(620f, safe.player.bottom, .001f);
        assertNull(columns(775f, 650f, 1f, 350f, 277f));
        assertNull(FoldMusicLayout.columns(775f, 650f, 1f, 650, 0, 24, 0, 24, 80,
                new FoldMusicLayout.Box(31, 350, 484, 620), 600));
    }

    private FoldMusicLayout.Columns capturedColumns(float top, float nativeHeight, float oldWidth, float scale) {
        return FoldMusicLayout.columns(2157.75f*scale, 1672f*scale, 2.75f*scale, 608,
                33f*scale, 0, 0, 0, 172.6908f*scale,
                new FoldMusicLayout.Box(98f*scale, top*scale, (98f+oldWidth)*scale,
                        (top+nativeHeight)*scale), Float.NaN);
    }

    @Test public void nativePlayerTopKeepsTheArtworkOutOfItsTouchAndClippingFrame() {
        for (int top = 0; top < 650; top += 10) {
            FoldMusicLayout.Columns c = columns(775, 650, 1, top, 110);
            if (c == null) continue;
            assertEquals(top, c.player.top, 0);
            assertEquals(top + 110, c.player.bottom, 0);
            assertTrue(c.artworkRoom.bottom <= top - 16);
            assertTrue(c.lyrics.left > c.player.right);
        }
    }

    @Test public void artworkAspectsAlwaysStayInsideTheirAssignedColumn() {
        FoldMusicLayout.Columns c = columns(775, 650, 1, 350, 145);
        for (float aspect : new float[]{1, 16f / 9, 9f / 16}) {
            CoverCardStyle.Rect r = FoldMusicLayout.artwork(c.artworkRoom, aspect, 1);
            assertNotNull(r);
            CoverMorphMotion.Box drawn = CoverMorphMotion.cardBox(r.x, r.y, r.side, 1, aspect);
            assertTrue(drawn.x >= c.artworkRoom.left - .001);
            assertTrue(drawn.y >= c.artworkRoom.top - .001);
            assertTrue(drawn.x + drawn.w <= c.artworkRoom.right + .001);
            assertTrue(drawn.y + drawn.h <= c.artworkRoom.bottom + .001);
        }
    }

    @Test public void changingDensityDoesNotChangeTheDpArrangement() {
        FoldMusicLayout.Columns a = columns(775, 650, 1, 350, 145);
        FoldMusicLayout.Columns b = columns(2015, 1690, 2.6f, 910, 377);
        assertNotNull(b);
        assertEquals(a.player.left, b.player.left / 2.6f, .001f);
        assertEquals(a.player.width(), b.player.width() / 2.6f, .001f);
        assertEquals(a.lyrics.left, b.lyrics.left / 2.6f, .001f);
        assertEquals(a.lyrics.height(), b.lyrics.height() / 2.6f, .001f);
    }

    @Test public void missingOrInsufficientRoomDoesNotInventASecondVerticalStack() {
        assertNull(columns(775, 650, 1, 145, 145));
        assertNull(columns(500, 650, 1, 350, 145));
        assertNull(columns(775, 650, 1, 550, 145));
        assertNull(FoldMusicLayout.columns(775, 650, 1, 650, 0, 24, 0, 24, 80, null, Float.NaN));
        assertNull(columns(Float.NaN, 650, 1, 350, 145));
        assertNull(columns(775, 650, 0, 350, 145));
    }
}
