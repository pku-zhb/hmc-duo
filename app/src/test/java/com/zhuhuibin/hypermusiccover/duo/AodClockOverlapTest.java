package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class AodClockOverlapTest {
    private static boolean overlapsWithAlpha(float ownAlpha, float ancestorAlpha, float transitionAlpha) {
        float drawn = NativeMediaControlPolicy.effectiveAlpha(1f, ownAlpha, 1f);
        drawn = NativeMediaControlPolicy.effectiveAlpha(drawn, ancestorAlpha, transitionAlpha);
        return AodArtworkPolicy.clockCanOcclude(drawn)
                && AodArtworkPolicy.clockOverlapsArtwork(100, 100, 500, 900, 100, 450, 500, 850);
    }

    @Test public void statusReplacementDoesNotLetItsHiddenNativeFaceCollapseArtwork() {
        // The native face remains VISIBLE and measurable at full size, but is alpha-hidden.
        assertFalse(overlapsWithAlpha(0, 1, 1));
        assertTrue(overlapsWithAlpha(1, 1, 1));
    }

    @Test public void aHiddenAncestorOrTransitionCannotCauseAClockCollision() {
        assertFalse(overlapsWithAlpha(1, 0, 1));
        assertFalse(overlapsWithAlpha(1, 1, 0));
        assertFalse(overlapsWithAlpha(.1f, .1f, .5f));
    }

    @Test public void onlyVisiblyOverlappingGlyphsCanCompactArtworkDuringTheFade() {
        assertFalse(overlapsWithAlpha(.01f, 1, 1));
        assertFalse(overlapsWithAlpha(.001f, 1, 1));
        assertTrue(overlapsWithAlpha(.5f, .95f, 1));
    }

    @Test public void invalidAlphaNeverCreatesAnInvisibleCollision() {
        assertFalse(overlapsWithAlpha(Float.NaN, 1, 1));
        assertFalse(overlapsWithAlpha(1, Float.POSITIVE_INFINITY, 1));
        assertFalse(overlapsWithAlpha(1, 1, Float.NaN));
        assertFalse(AodArtworkPolicy.clockCanOcclude(Float.NaN));
        assertFalse(AodArtworkPolicy.clockCanOcclude(Float.POSITIVE_INFINITY));
    }

    @Test public void separatedClockKeepsBigArtwork() {
        assertFalse(AodArtworkPolicy.clockOverlapsArtwork(100, 100, 500, 400, 100, 450, 500, 850));
    }
    @Test public void overlappingClockCollapsesArtwork() {
        assertTrue(AodArtworkPolicy.clockOverlapsArtwork(100, 100, 500, 600, 100, 450, 500, 850));
    }
    @Test public void longClockBesideArtworkDoesNotCollapseIt() {
        assertFalse(AodArtworkPolicy.clockOverlapsArtwork(0, 100, 80, 800, 100, 450, 500, 850));
    }
    @Test public void touchingOrSubpixelEdgesDoNotCollapseArtwork() {
        assertFalse(AodArtworkPolicy.clockOverlapsArtwork(100, 100, 500, 450, 100, 450, 500, 850));
        assertFalse(AodArtworkPolicy.clockOverlapsArtwork(100, 100, 500, 450.5f, 100, 450, 500, 850));
    }
    @Test public void invalidClockMeasurementDoesNotCollapseArtwork() {
        assertFalse(AodArtworkPolicy.clockOverlapsArtwork(100, Float.NaN, 500, 600, 100, 450, 500, 850));
        assertFalse(AodArtworkPolicy.clockOverlapsArtwork(500, 100, 100, 600, 100, 450, 500, 850));
    }
}
