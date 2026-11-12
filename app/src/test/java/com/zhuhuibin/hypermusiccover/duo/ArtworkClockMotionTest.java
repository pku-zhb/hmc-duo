package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class ArtworkClockMotionTest {
    private final ArtworkClockMotion.Pose nativeClock = new ArtworkClockMotion.Pose(318, 484, 166);
    private final ArtworkClockMotion.Pose dateRow = new ArtworkClockMotion.Pose(172, 64, 158);

    private static void same(ArtworkClockMotion.Pose a, ArtworkClockMotion.Pose b) {
        assertEquals(a.top, b.top, .001f);
        assertEquals(a.height, b.height, .001f);
        assertEquals(a.date, b.date, .001f);
    }

    @Test public void entryAndReturnVisitTheSameDrawnInkPosesInReverseOrder() {
        for (int frame = 0; frame <= 100; frame++) {
            float p = frame / 100f;
            same(ArtworkClockMotion.between(nativeClock, dateRow, p),
                    ArtworkClockMotion.between(dateRow, nativeClock, 1f - p));
        }
    }

    @Test public void aSecondTapReversesFromTheCurrentlyDrawnClockWithoutJumping() {
        for (float entered : new float[]{.01f, .25f, .55f, .9f}) {
            ArtworkClockMotion.Pose interrupted = ArtworkClockMotion.between(nativeClock, dateRow, entered);
            same(interrupted, ArtworkClockMotion.between(interrupted, nativeClock, 0));
            for (int frame = 0; frame <= 20; frame++) {
                float returnProgress = frame / 20f;
                same(ArtworkClockMotion.between(nativeClock, dateRow, entered * (1f - returnProgress)),
                        ArtworkClockMotion.between(interrupted, nativeClock, returnProgress));
            }
        }
    }

    @Test public void reversingAnUnfinishedReturnKeepsTheSamePath() {
        ArtworkClockMotion.Pose interrupted = ArtworkClockMotion.between(dateRow, nativeClock, .4f);
        for (int frame = 0; frame <= 20; frame++) {
            float p = frame / 20f;
            same(ArtworkClockMotion.between(nativeClock, dateRow, .6f + .4f * p),
                    ArtworkClockMotion.between(interrupted, dateRow, p));
        }
    }

    @Test public void nativeFontUnitsCannotChangeTheCapturedPixelHeight() {
        ArtworkClockMotion.Pose frame = ArtworkClockMotion.between(nativeClock, dateRow, .5f);
        for (float currentGlyphHeight : new float[]{300, 484, 650}) {
            float scale = frame.height / currentGlyphHeight;
            assertEquals(274f, currentGlyphHeight * scale, .001f);
        }
    }

    @Test public void changedNotificationsOrDisplayInvalidateTheOldNativeEndpoint() {
        assertTrue(ArtworkClockMotion.sameNativeLayout(7, 7, 318, 318.25f));
        assertFalse(ArtworkClockMotion.sameNativeLayout(7, 8, 318, 318));
        assertFalse(ArtworkClockMotion.sameNativeLayout(7, 7, 318, 320));
        assertFalse(ArtworkClockMotion.sameNativeLayout(7, 7, Float.NaN, 318));
        assertFalse(ArtworkClockMotion.sameNativeLayout(7, 7, 318, Float.POSITIVE_INFINITY));
    }

    @Test public void springOvershootCannotCarryTheDateRowPastEitherEndpoint() {
        same(nativeClock, ArtworkClockMotion.between(nativeClock, dateRow, -.2f));
        same(dateRow, ArtworkClockMotion.between(nativeClock, dateRow, 1.2f));
        same(dateRow, ArtworkClockMotion.between(nativeClock, dateRow, Float.NaN));
    }
}
