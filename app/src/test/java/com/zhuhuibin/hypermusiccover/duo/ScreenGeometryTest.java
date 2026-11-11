package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class ScreenGeometryTest {
    @Test public void mi18FoldHandoffMustNotKeepTheObservedOuterSizeOnTheInnerScreen() {
        ScreenGeometry outer = new ScreenGeometry(1168, 1712, 440, 1);
        ScreenGeometry inner = outer.update(1672, 2364, 440);
        ScreenGeometry landscape = inner.update(2364, 1672, 440);
        assertEquals(1672, inner.width); assertEquals(2364, inner.height);
        assertEquals(2364, landscape.width); assertEquals(1672, landscape.height);
        assertEquals(3, landscape.revision);
        assertSame(landscape, landscape.update(2364, 1672, 440));
    }
    @Test public void repeatedPredrawDoesNotRequestMoreArtwork() {
        ScreenGeometry g = new ScreenGeometry(1200, 2608, 480, 1);
        assertSame(g, g.update(1200, 2608, 480));
    }

    @Test public void foldRotationAndDensityChangesInvalidateTheOldComposition() {
        ScreenGeometry outer = new ScreenGeometry(1200, 2608, 480, 1);
        ScreenGeometry inner = outer.update(2238, 1672, 420);
        ScreenGeometry rotated = inner.update(1672, 2238, 420);
        ScreenGeometry density = rotated.update(1672, 2238, 440);
        assertEquals(2, inner.revision);
        assertEquals(3, rotated.revision);
        assertEquals(4, density.revision);
        // A worker holding the old snapshot still sees a consistent pair.
        assertEquals(1200, outer.width);
        assertEquals(2608, outer.height);
        assertEquals(2238, inner.width);
        assertEquals(1672, inner.height);
    }

    @Test public void zeroSizeDuringDisplayHandoffCannotEraseTheLastValidGeometry() {
        ScreenGeometry g = new ScreenGeometry(2238, 1672, 420, 2);
        assertSame(g, g.update(0, 0, 420));
        assertSame(g, g.update(-1, 1672, 420));
        assertSame(g, g.update(2238, 1672, 0));
    }
}
