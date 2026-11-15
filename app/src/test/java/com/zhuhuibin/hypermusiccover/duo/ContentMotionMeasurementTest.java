package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class ContentMotionMeasurementTest {
    private static final FoldMusicLayout.Box PLAYER = new FoldMusicLayout.Box(100, 1100, 2100, 1470);

    private static final class NativeFrame {
        final float scaleX, scaleY, originX, originY, layoutX, layoutY;
        NativeFrame(float scaleX, float scaleY, float translationX, float translationY,
                    float layoutX, float layoutY) {
            this.scaleX = scaleX; this.scaleY = scaleY;
            this.originX = layoutX + translationX; this.originY = layoutY + translationY;
            this.layoutX = layoutX; this.layoutY = layoutY;
        }

        ContentMotionMeasurement measurement() {
            return ContentMotionMeasurement.from(
                    new FoldMusicLayout.Box(originX, originY,
                            originX + scaleX * 100, originY + scaleY * 100),
                    ContentMotionSnapshot.logicalPlan(new FoldMusicLayout.Box(0, 0, 100, 100),
                            layoutX, layoutY));
        }

        float measuredY(float naturalY) { return originY + scaleY * (naturalY - layoutY); }

        FoldMusicLayout.Box measured(FoldMusicLayout.Box natural) {
            return new FoldMusicLayout.Box(originX + scaleX * (natural.left - layoutX),
                    measuredY(natural.top), originX + scaleX * (natural.right - layoutX),
                    measuredY(natural.bottom));
        }

        FoldMusicLayout.Box presented(FoldMusicLayout.Box logical) {
            return new FoldMusicLayout.Box(originX + scaleX * logical.left,
                    originY + scaleY * logical.top, originX + scaleX * logical.right,
                    originY + scaleY * logical.bottom);
        }
    }

    private static FoldMusicLayout.Columns build(NativeFrame nativeFrame, float width, float height,
                                                 FoldMusicLayout.Box naturalPlayer) {
        ContentMotionMeasurement measurement = nativeFrame.measurement();
        assertNotNull(measurement);
        return measurement.columns(width, height, 2f, 600, 100, 0, 0, 60,
                nativeFrame.measuredY(260), nativeFrame.measured(naturalPlayer), Float.NaN);
    }

    private static FoldMusicLayout.Columns natural(float width, float height, FoldMusicLayout.Box player) {
        return FoldMusicLayout.columns(width, height, 2f, 600, 100, 0, 0, 60,
                260, player, Float.NaN);
    }

    private static void same(FoldMusicLayout.Box expected, FoldMusicLayout.Box actual) {
        assertNotNull(expected); assertNotNull(actual);
        assertEquals(expected.left, actual.left, .003f);
        assertEquals(expected.top, actual.top, .003f);
        assertEquals(expected.right, actual.right, .003f);
        assertEquals(expected.bottom, actual.bottom, .003f);
    }

    private static void same(FoldMusicLayout.Columns expected, FoldMusicLayout.Columns actual) {
        assertNotNull(expected); assertNotNull(actual);
        same(expected.artworkRoom, actual.artworkRoom);
        same(expected.player, actual.player);
        same(expected.lyrics, actual.lyrics);
    }

    private static FoldMusicLayout.Box square(CoverCardStyle.Rect rect) {
        return new FoldMusicLayout.Box(rect.x, rect.y, rect.x + rect.side, rect.y + rect.side);
    }

    @Test public void firstBuildAtTranslationFiftyDoesNotApplyOneHundredPixelsToArtworkAndLyrics() {
        ContentMotionMeasurement translated = ContentMotionMeasurement.from(
                new FoldMusicLayout.Box(0, 50, 100, 150), new FoldMusicLayout.Box(0, 0, 100, 100));
        // These are the actual already-translated window inputs from the review reproduction.
        FoldMusicLayout.Columns built = translated.columns(2200, 1600, 2, 600,
                100, 0, 0, 60, 310, new FoldMusicLayout.Box(100, 1150, 2100, 1520), Float.NaN);
        same(natural(2200, 1600, PLAYER), built);
        CoverCardStyle.Rect art = FoldMusicLayout.artwork(built.artworkRoom, 1, 2);
        assertEquals(316, art.y, .001f);
        assertEquals(752, art.side, .001f);
        NativeFrame frame = new NativeFrame(1, 1, 0, 50, 0, 0);
        assertEquals(366, frame.presented(square(art)).top, .001f);
        assertEquals(366, frame.presented(built.lyrics).top, .001f);
    }

    @Test public void firstBuildUnderScaleAndTranslationKeepsOneNaturalParagraphAndOneNativePose() {
        FoldMusicLayout.Columns expected = natural(2200, 1600, PLAYER);
        for (float p : new float[]{0, .1f, .3f, .7f, 1}) {
            NativeFrame frame = new NativeFrame(1 - .05f * p, 1 - .05f * p,
                    45 * p, 50 * p, 20, 40);
            FoldMusicLayout.Columns built = build(frame, 2200, 1600, PLAYER);
            same(expected, built);
            assertEquals(expected.lyrics.width(), built.lyrics.width(), .001f);
            assertPresentedOnce(frame, expected, built);
        }
    }

    private static void assertPresentedOnce(NativeFrame frame, FoldMusicLayout.Columns expected,
                                            FoldMusicLayout.Columns built) {
        FoldMusicLayout.Box logicalLyrics = ContentMotionSnapshot.logicalPlan(built.lyrics,
                frame.layoutX, frame.layoutY);
        same(frame.measured(expected.lyrics), frame.presented(logicalLyrics));
        CoverCardStyle.Rect expectedArt = FoldMusicLayout.artwork(expected.artworkRoom, 1, 2);
        CoverCardStyle.Rect builtArt = FoldMusicLayout.artwork(built.artworkRoom, 1, 2);
        FoldMusicLayout.Box logicalArtwork = ContentMotionSnapshot.logicalPlan(square(builtArt),
                frame.layoutX, frame.layoutY);
        same(frame.measured(square(expectedArt)), frame.presented(logicalArtwork));
    }

    @Test public void rebuildingAfterWindowOwnerOrGeometryInvalidationUsesFreshNormalizedMeasurements() {
        Object window = new Object(), owner = new Object();
        ContentMotionSnapshot previous = new ContentMotionSnapshot(window, 4, owner,
                natural(2400, 1700, PLAYER).lyrics);
        Object[] nextWindows = {new Object(), window, window};
        Object[] nextOwners = {owner, new Object(), owner};
        long[] revisions = {4, 4, 5};
        for (int i = 0; i < revisions.length; i++) {
            assertFalse(previous.current(nextWindows[i], revisions[i], nextOwners[i]));
            NativeFrame current = new NativeFrame(.95f, .95f, 35, 50, 15, 25);
            FoldMusicLayout.Columns rebuilt = build(current, 2200, 1600, PLAYER);
            FoldMusicLayout.Columns expected = natural(2200, 1600, PLAYER);
            same(expected, rebuilt);
            assertPresentedOnce(current, expected, rebuilt);
            ContentMotionSnapshot replacement = new ContentMotionSnapshot(nextWindows[i], revisions[i],
                    nextOwners[i], ContentMotionSnapshot.logicalPlan(rebuilt.lyrics, 15, 25));
            assertTrue(replacement.current(nextWindows[i], revisions[i], nextOwners[i]));
        }
    }

    @Test public void aNativeChildContractionRemainsAfterRemovingOnlyItsCommonOwnerMotion() {
        FoldMusicLayout.Box compactPlayer = new FoldMusicLayout.Box(100, 1200, 2100, 1450);
        NativeFrame frame = new NativeFrame(.95f, .95f, 35, 50, 20, 40);
        FoldMusicLayout.Columns built = build(frame, 2200, 1600, compactPlayer);
        same(natural(2200, 1600, compactPlayer), built);
        assertEquals(1200, built.player.top, .001f);
        assertEquals(250, built.player.height(), .001f);
    }

    @Test public void eachAxisUsesItsOwnNativeScaleWithoutScalingNaturalDpOrInsets() {
        NativeFrame frame = new NativeFrame(.97f, .93f, 45, -30, 15, 25);
        FoldMusicLayout.Columns built = build(frame, 2200, 1600, PLAYER);
        FoldMusicLayout.Columns expected = natural(2200, 1600, PLAYER);
        same(expected, built);
        assertPresentedOnce(frame, expected, built);
    }

    @Test public void naturalClockSafetyIsCheckedAfterRemovingANegativeNativeOffset() {
        NativeFrame frame = new NativeFrame(.95f, .95f, 35, -300, 0, 0);
        assertTrue(frame.measuredY(260) < 0);
        FoldMusicLayout.Columns built = build(frame, 2200, 1600, PLAYER);
        FoldMusicLayout.Columns expected = natural(2200, 1600, PLAYER);
        same(expected, built);
        assertPresentedOnce(frame, expected, built);
    }

    @Test public void unavailableNativeAffineDoesNotInventACombinedPlan() {
        FoldMusicLayout.Box naturalUnit = new FoldMusicLayout.Box(0, 0, 100, 100);
        assertNull(ContentMotionMeasurement.from(null, naturalUnit));
        assertNull(ContentMotionMeasurement.from(new FoldMusicLayout.Box(0, 0, 0, 100), naturalUnit));
        assertNull(ContentMotionMeasurement.from(new FoldMusicLayout.Box(0, 0, Float.NaN, 100), naturalUnit));
        assertNull(ContentMotionMeasurement.from(naturalUnit, null));
        assertNull(new NativeFrame(.95f, .95f, 35, 50, 0, 0).measurement().columns(
                2200, 1600, 2, 600, 100, 0, 0, 60, Float.NaN, PLAYER, Float.NaN));
    }
}
