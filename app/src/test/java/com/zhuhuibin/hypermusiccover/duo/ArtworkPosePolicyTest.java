package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class ArtworkPosePolicyTest {
    private static void same(CoverCardStyle.Rect expected, CoverCardStyle.Rect actual) {
        assertNotNull(actual);
        assertEquals(expected.x, actual.x, .002f);
        assertEquals(expected.y, actual.y, .002f);
        assertEquals(expected.side, actual.side, .002f);
    }

    private static CoverCardStyle.Rect local(CoverCardStyle.Rect window, float scale, float x, float y) {
        return new CoverCardStyle.Rect((window.x - x) / scale, (window.y - y) / scale,
                window.side / scale);
    }

    private static CoverCardStyle.Rect window(CoverCardStyle.Rect local, float scale, float x, float y) {
        return new CoverCardStyle.Rect(x + local.x * scale, y + local.y * scale,
                local.side * scale);
    }

    @Test public void aHeldLogicalPoseReceivesNativeShrinkAndOffsetExactlyOnce() {
        CoverCardStyle.Rect committed = new CoverCardStyle.Rect(80, 510, 1100);
        float[] nativeWidths = {1100, 1060, 990, 1010, 1100};
        float[] nativeScales = {1, .99f, .97f, .955f, .95f};
        for (int frame = 0; frame < nativeWidths.length; frame++) {
            float scale = nativeScales[frame], x = frame * 3f, y = frame * 7f;
            CoverCardStyle.Rect expected = window(committed, scale, x, y);
            for (float consumerScale : new float[]{1f, scale, .93f}) {
                // A separate host, a host already carrying the same AOD affine, and a
                // differently scaled host must all draw the native-content pose once.
                CoverCardStyle.Rect mapped = local(expected, consumerScale, 20, 40);
                CoverCardStyle.Rect posed = ArtworkPosePolicy.fit(mapped, true, true,
                        nativeWidths[frame] / consumerScale);
                same(expected, window(posed, consumerScale, 20, 40));
            }
        }
    }

    @Test public void nativeCancellationAndReversalKeepTheSameCommittedEndpoint() {
        Object window = new Object();
        AodTransitionProgress nativeMotion = new AodTransitionProgress();
        CoverCardStyle.Rect committed = new CoverCardStyle.Rect(70, 430, 980);
        nativeMotion.begin(window, 5, true);
        for (float t : new float[]{0, .2f, .5f, .7f}) {
            nativeMotion.frame(window, 5, true, t);
            replay(committed, nativeMotion.snapshot(window, 5, false).fraction);
        }
        nativeMotion.cancel(window, 5);
        nativeMotion.begin(window, 5, false);
        for (float t : new float[]{0, .2f, .6f, 1}) {
            nativeMotion.frame(window, 5, false, t);
            replay(committed, nativeMotion.snapshot(window, 5, false).fraction);
        }
        nativeMotion.complete(window, 5, false);
        replay(committed, nativeMotion.snapshot(window, 5, false).fraction);
        // The first released lit frame uses the same already-fitted layout endpoint.
        same(committed, ArtworkPosePolicy.fit(committed, false, true, 980));
    }

    private static void replay(CoverCardStyle.Rect committed, float fraction) {
        float scale = 1f - .05f * fraction;
        float x = 35f * fraction, y = 60f * fraction;
        // Native compact-card layout has its own changing width; it is not an artwork endpoint.
        float nativeWidth = 980f - 150f * (float) Math.sin(Math.PI * fraction);
        CoverCardStyle.Rect expected = window(committed, scale, x, y);
        CoverCardStyle.Rect posed = ArtworkPosePolicy.fit(local(expected, scale, x, y),
                true, true, nativeWidth / scale);
        same(expected, window(posed, scale, x, y));
    }

    @Test public void losingTheTransientNativeCardCannotEraseAHeldFrame() {
        CoverCardStyle.Rect committed = new CoverCardStyle.Rect(100, 300, 900);
        for (float width : new float[]{Float.NaN, 0, -1, Float.POSITIVE_INFINITY})
            same(committed, ArtworkPosePolicy.fit(committed, true, true, width));
        assertNull(ArtworkPosePolicy.fit(null, true, true, 900));
    }

    @Test public void anOrdinaryPortraitLayoutStillFitsTheActualPlayerWidth() {
        CoverCardStyle.Rect requested = new CoverCardStyle.Rect(40, 300, 1400);
        CoverCardStyle.Rect fitted = ArtworkPosePolicy.fit(requested, false, true, 1000);
        assertEquals(1000, fitted.side, 0);
        assertEquals(requested.x + requested.side / 2, fitted.x + fitted.side / 2, 0);
        assertEquals(requested.y + requested.side / 2, fitted.y + fitted.side / 2, 0);
        assertNull(ArtworkPosePolicy.fit(requested, false, true, Float.NaN));
    }

    @Test public void outerAndLandscapeLayoutsDoNotAcquireThePortraitCap() {
        CoverCardStyle.Rect requested = new CoverCardStyle.Rect(40, 300, 1400);
        same(requested, ArtworkPosePolicy.fit(requested, false, false, 800));
    }

    @Test public void nativeRelayoutCannotCarryAMotionOwnedPoseAfterTheClockSpringLands() {
        assertFalse(ArtworkPosePolicy.carryLocal(true, false, true));
        assertFalse(ArtworkPosePolicy.carryLocal(false, false, false));
        assertFalse(ArtworkPosePolicy.carryLocal(true, true, false));
        // A normal lit notification drag still carries artwork with the player each frame.
        assertTrue(ArtworkPosePolicy.carryLocal(true, false, false));
    }

    @Test public void bothColumnsRetainTheirRelationshipUnderTheSameNativeAffine() {
        FoldMusicLayout.Columns columns = FoldMusicLayout.columns(2200, 1600, 2f, 600,
                100, 0, 100, 60, 260,
                new FoldMusicLayout.Box(100, 1100, 2100, 1470), 1550);
        assertNotNull(columns);
        CoverCardStyle.Rect art = FoldMusicLayout.artwork(columns.artworkRoom, 1, 2);
        float lyricCenter = (columns.lyrics.left + columns.lyrics.right) / 2;
        float litDistance = lyricCenter - (art.x + art.side / 2);
        for (float p : new float[]{0, .1f, .4f, .8f, 1, .8f, .3f, 0}) {
            float nativeScale = 1f - .05f * p, nativeX = 30f * p, nativeY = -50f * p;
            CoverCardStyle.Rect expected = window(art, nativeScale, nativeX, nativeY);
            // Artwork and lyrics can live under different consumers; the shared owner
            // determines their positions and scale, not either consumer's inverse width.
            CoverCardStyle.Rect drawn = window(ArtworkPosePolicy.fit(
                    local(expected, .92f, 4, 17), true, false, 500), .92f, 4, 17);
            float drawnLyricCenter = nativeX + nativeScale * lyricCenter;
            assertEquals(litDistance * nativeScale,
                    drawnLyricCenter - (drawn.x + drawn.side / 2), .002f);
            assertEquals(art.side * nativeScale, drawn.side, .002f);
        }
    }
}
