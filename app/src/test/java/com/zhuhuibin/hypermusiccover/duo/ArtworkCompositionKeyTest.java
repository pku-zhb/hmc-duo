package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;

import static org.junit.Assert.*;

public class ArtworkCompositionKeyTest {
    private final Object window = new Object();

    private ArtworkCompositionKey key(Object window, long revision, int width, int height,
                                      int mode, boolean card, long source, int blur, float bias) {
        return new ArtworkCompositionKey(window, revision, width, height, mode, card, source, blur, bias);
    }

    private ArtworkCompositionKey baseline() { return key(window, 3, 1000, 1600, 1, true, 8, 24, .5f); }

    @Test public void onlyARealWindowMeasuredBoundsAndRetainedSourceAreValid() {
        assertTrue(baseline().valid());
        assertTrue(key(window, 0, 1, 1, 0, false, 0, 0, 0f).valid());
        assertFalse(key(null, 3, 1000, 1600, 1, true, 8, 24, .5f).valid());
        assertFalse(key(window, 3, 0, 1600, 1, true, 8, 24, .5f).valid());
        assertFalse(key(window, 3, 1000, -1, 1, true, 8, 24, .5f).valid());
        assertFalse(key(window, 3, 1000, 1600, 1, true, -1, 24, .5f).valid());
    }

    @Test public void identicalCompositionInputsMatchWithoutObjectReuse() {
        assertTrue(baseline().same(baseline()));
        assertFalse(baseline().same(null));
    }

    @Test public void aWindowWithEqualContentIsStillADifferentDrawingDomain() {
        Object a = new String("window"), b = new String("window");
        assertEquals(a, b);
        assertFalse(key(a, 3, 1000, 1600, 1, true, 8, 24, .5f)
                .same(key(b, 3, 1000, 1600, 1, true, 8, 24, .5f)));
    }

    @Test public void revisionAndActualDimensionsBothInvalidateComposition() {
        ArtworkCompositionKey key = baseline();
        assertFalse(key.same(key(window, 4, 1000, 1600, 1, true, 8, 24, .5f)));
        assertFalse(key.same(key(window, 3, 999, 1600, 1, true, 8, 24, .5f)));
        assertFalse(key.same(key(window, 3, 1000, 1599, 1, true, 8, 24, .5f)));
    }

    @Test public void coverModeAndCardCompositionAreIndependentInputs() {
        ArtworkCompositionKey key = baseline();
        assertFalse(key.same(key(window, 3, 1000, 1600, 2, true, 8, 24, .5f)));
        assertFalse(key.same(key(window, 3, 1000, 1600, 1, false, 8, 24, .5f)));
    }

    @Test public void replacingTheSourceDoesNotMasqueradeAsAGeometryChange() {
        ArtworkCompositionKey nextSource = key(window, 3, 1000, 1600, 1, true, 9, 24, .5f);
        assertFalse(baseline().same(nextSource));
        assertTrue(nextSource.sameGeometry(window, 3, 1000, 1600, 1, true, 24, .5f));
    }

    @Test public void blurAndEvenSmallBiasChangesInvalidateComposition() {
        ArtworkCompositionKey key = baseline();
        assertFalse(key.same(key(window, 3, 1000, 1600, 1, true, 8, 25, .5f)));
        assertFalse(key.same(key(window, 3, 1000, 1600, 1, true, 8, 24, Math.nextUp(.5f))));
        assertFalse(key(window, 3, 1000, 1600, 1, true, 8, 24, 0f)
                .same(key(window, 3, 1000, 1600, 1, true, 8, 24, -0f)));
    }
}
