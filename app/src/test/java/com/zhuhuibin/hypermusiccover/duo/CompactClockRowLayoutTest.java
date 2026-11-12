package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class CompactClockRowLayoutTest {
    @Test public void artworkAndLyricsHonorTheSamePreferenceOnOuterAndInnerLandscape() {
        assertTrue(CompactClockRowLayout.requested(true, 1168, 1712, 2.75f, true, false, false));
        assertTrue(CompactClockRowLayout.requested(true, 2488, 2224, 2.75f, true, false, false));
        assertFalse(CompactClockRowLayout.requested(true, 2224, 2488, 2.75f, true, false, false));
    }

    @Test public void orientationChangesUseTheCurrentDisplayRatherThanThePreviousScreen() {
        float[][] windows = {{1168,1712}, {2488,2224}, {2224,2488}, {2488,2224}, {1168,1712}};
        boolean[] expected = {true, true, false, true, true};
        for (int i = 0; i < windows.length; i++) {
            assertEquals(expected[i], CompactClockRowLayout.supportsDisplay(
                    windows[i][0], windows[i][1], 2.75f));
        }
    }

    @Test public void innerLandscapeStillHonorsSceneAndUserChoice() {
        assertFalse(CompactClockRowLayout.requested(false, 2488, 2224, 2.75f, true, false, false));
        assertFalse(CompactClockRowLayout.requested(true, 2488, 2224, 2.75f, false, false, false));
        assertFalse(CompactClockRowLayout.requested(true, 2488, 2224, 2.75f, true, true, false));
        assertFalse(CompactClockRowLayout.requested(true, 2488, 2224, 2.75f, true, false, true));
    }

    @Test public void systemAndPlayerPagesRetainTheirNativeClock() {
        assertFalse(CompactClockRowLayout.requested(true, 400, 800, 1, false, false, false));
        assertFalse(CompactClockRowLayout.requested(true, 400, 800, 1, true, true, false));
        assertFalse(CompactClockRowLayout.requested(true, 400, 800, 1, true, false, true));
        assertFalse(CompactClockRowLayout.requested(false, 400, 800, 1, true, false, false));
    }

    @Test public void unknownScreenDoesNotCollapseTheClock() {
        assertFalse(CompactClockRowLayout.requested(true, 0, 800, 1, true, false, false));
        assertFalse(CompactClockRowLayout.requested(true, 400, 800, 0, true, false, false));
        assertFalse(CompactClockRowLayout.requested(true, Float.NaN, 800, 1, true, false, false));
        assertFalse(CompactClockRowLayout.supportsDisplay(Float.POSITIVE_INFINITY, 800, 1));
        assertFalse(CompactClockRowLayout.supportsDisplay(2488, 2224, Float.NaN));
    }

    @Test public void timeDefaultsBeforeDateWithinContentBounds() {
        CompactClockRowLayout.Row row = CompactClockRowLayout.place(16, 324, 50, 120, 8, true);
        assertNotNull(row);
        assertEquals(1, row.scale, 0.001f);
        assertEquals(81, row.timeLeft, 0.001f);
        assertEquals(139, row.dateLeft, 0.001f);
    }

    @Test public void placementOptionMovesTimeAfterDateWithoutChangingRowWidth() {
        CompactClockRowLayout.Row row = CompactClockRowLayout.place(16, 324, 50, 120, 8, false);
        assertEquals(81, row.dateLeft, 0.001f);
        assertEquals(209, row.timeLeft, 0.001f);
    }

    @Test public void longDateFitsWithoutTruncatingOrTouchingTheRail() {
        CompactClockRowLayout.Row row = CompactClockRowLayout.place(16, 284, 60, 360, 8, true);
        assertTrue(row.scale < 1);
        assertEquals(16, row.timeLeft, 0.001f);
        assertEquals(284, row.dateLeft + 360 * row.scale, 0.001f);
        assertEquals(8 * row.scale, row.dateLeft - row.timeLeft - row.timeWidth, 0.001f);
    }

    @Test public void innerLandscapeDateRowRemainsTogetherAndInsideTheContentColumn() {
        float density = 2.75f;
        float left = 16 * density, right = 800 * density;
        for (boolean before : new boolean[]{true, false}) {
            CompactClockRowLayout.Row row = CompactClockRowLayout.place(left, right, 164, 580,
                    8 * density, before);
            assertNotNull(row);
            assertEquals(1, row.scale, .001f);
            assertTrue(row.timeLeft >= left && row.timeLeft + row.timeWidth <= right);
            assertTrue(row.dateLeft >= left && row.dateLeft + 580 * row.scale <= right);
            float gap = before ? row.dateLeft - row.timeLeft - row.timeWidth
                    : row.timeLeft - row.dateLeft - 580 * row.scale;
            assertEquals(8 * density, gap, .001f);
        }
    }

    @Test public void replacementTimeAppearsOnlyAsTheNativeFaceArrives() {
        assertEquals(0, CompactClockRowLayout.alpha(0), 0.001f);
        assertEquals(0, CompactClockRowLayout.alpha(0.55f), 0.001f);
        assertEquals(0.5f, CompactClockRowLayout.alpha(0.775f), 0.001f);
        assertEquals(1, CompactClockRowLayout.alpha(1), 0.001f);
    }

    @Test public void reversingMidFlightStartsAtTheSameProgress() {
        assertEquals(0.6f, CompactClockRowLayout.progress(0.6f, false, 0), 0.001f);
        assertEquals(0.3f, CompactClockRowLayout.progress(0.6f, false, 0.5f), 0.001f);
        assertEquals(0, CompactClockRowLayout.progress(0.6f, false, 1), 0.001f);
        assertEquals(0.8f, CompactClockRowLayout.progress(0.6f, true, 0.5f), 0.001f);
        assertEquals(1, CompactClockRowLayout.progress(0.6f, true, 1.1f), 0.001f);
    }

    @Test public void incompleteDateGeometryKeepsTheOriginalFace() {
        assertNull(CompactClockRowLayout.place(100, 100, 50, 120, 8, true));
        assertNull(CompactClockRowLayout.place(0, 300, 50, 0, 8, true));
        assertNull(CompactClockRowLayout.place(0, 300, Float.NaN, 120, 8, true));
    }

    @Test public void classicCanvasDateUsesItsOwnCenteredBaselineWithoutAnAndroidLayout() {
        CompactClockRowLayout.DateInk ink = CompactClockRowLayout.classicDate(
                580, 74, 0, 0, 300, 240, 40, 40, 40, 0, false, -58, 16);
        assertNotNull(ink);
        assertEquals(0, ink.left, 0.001f);
        assertEquals(580, ink.width, 0.001f);
        assertEquals(58, ink.baseline, 0.001f);
    }

    @Test public void classicForegroundDateRemainsValidWhenTextViewBodyIsEmpty() {
        CompactClockRowLayout.DateInk ink = CompactClockRowLayout.classicDate(
                580, 74, 10, 10, 300, 0, 0, 0, 0, 0, false, -58, 16);
        assertNotNull(ink);
        assertEquals(10, ink.left, 0.001f);
        assertEquals(300, ink.width, 0.001f);
    }

    @Test public void classicDateIncludesIconExtentAndTheTrailingNativeRectangle() {
        CompactClockRowLayout.DateInk ink = CompactClockRowLayout.classicDate(
                580, 74, 12, 12, 100, 180, 44, 44, 50, 24, false, -58, 16);
        assertEquals(12, ink.left, 0.001f);
        assertEquals(348, ink.width, 0.001f);
        CompactClockRowLayout.DateInk iconOnlyTail = CompactClockRowLayout.classicDate(
                580, 74, 12, 12, 100, 0, 44, 44, 50, 0, false, -58, 16);
        assertEquals(150, iconOnlyTail.width, 0.001f);
    }

    @Test public void rtlClassicDateKeepsNativeRightAnchoredForegroundAndAllDrawnParts() {
        CompactClockRowLayout.DateInk ink = CompactClockRowLayout.classicDate(
                580, 74, 12, 12, 100, 180, 44, 44, 50, 24, true, -58, 16);
        assertEquals(232, ink.left, 0.001f);
        assertEquals(348, ink.width, 0.001f);
        assertEquals(58, ink.baseline, 0.001f);
    }

    @Test public void wholeClassicDateFitsBesideTimeWithinTheObservedOuterScreenBounds() {
        CompactClockRowLayout.DateInk ink = CompactClockRowLayout.classicDate(
                580, 74, 0, 0, 300, 240, 40, 40, 40, 0, false, -58, 16);
        CompactClockRowLayout.Row left = CompactClockRowLayout.place(
                16 * 2.75f, 961.75f - 16 * 2.75f, 164, ink.width, 8 * 2.75f, true);
        CompactClockRowLayout.Row right = CompactClockRowLayout.place(
                16 * 2.75f, 961.75f - 16 * 2.75f, 164, ink.width, 8 * 2.75f, false);
        assertNotNull(left);
        assertNotNull(right);
        assertEquals(1, left.scale, 0.001f);
        assertEquals(left.timeLeft, right.dateLeft, 0.001f);
        assertEquals(22, left.dateLeft - left.timeLeft - left.timeWidth, 0.001f);
        assertEquals(22, right.timeLeft - right.dateLeft - ink.width, 0.001f);
        assertTrue(left.dateLeft + ink.width <= 917.75f);
        assertTrue(right.timeLeft + right.timeWidth <= 917.75f);
    }

    @Test public void unmeasuredOrBlankClassicDateCannotHideTheNativeClock() {
        assertNull(CompactClockRowLayout.classicDate(
                580, 0, 0, 0, 300, 240, 40, 40, 40, 0, false, -58, 16));
        assertNull(CompactClockRowLayout.classicDate(
                580, 74, 0, 0, 0, 0, 40, 40, 40, 0, false, -58, 16));
        assertNull(CompactClockRowLayout.classicDate(
                580, 74, 0, 0, Float.NaN, 240, 40, 40, 40, 0, false, -58, 16));
    }
}
