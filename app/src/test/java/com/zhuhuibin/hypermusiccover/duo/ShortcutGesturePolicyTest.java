package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class ShortcutGesturePolicyTest {
    @Test public void visibleWakeRampAcceptsShortcutsBeforeFullOpacity() {
        for (float alpha : new float[]{.1f, .25f, .5f, .97f, 1f})
            assertTrue(ShortcutGesturePolicy.shortcutVisible(alpha));
        for (float alpha : new float[]{0f, .01f, .099f, Float.NaN, Float.POSITIVE_INFINITY})
            assertFalse(ShortcutGesturePolicy.shortcutVisible(alpha));
    }

    // Contract from the on-device ShortcutMoveController: left consumes positive X,
    // right consumes negative X, after paging slop; release activation remains native.
    private float nativeDistance(boolean left, float start, float x, float slop) {
        return left ? Math.max(0, x - slop - start) : Math.max(0, start - x - slop);
    }
    @Test public void inwardDragProducesTheSameProgressForBothRelocatedButtons() {
        for (float delta : new float[]{0, 8, 30, 100, 250}) {
            float projected = ShortcutGesturePolicy.nativeX(true, true, 1000, 1000-delta);
            assertEquals(nativeDistance(false, 1000, 1000-delta, 24),
                    nativeDistance(true, 1000, projected, 24), .001f);
        }
    }
    @Test public void outwardAndReturnedDragsDoNotAccumulateActivationDistance() {
        for (float x : new float[]{1000, 1010, 1100})
            assertEquals(0, nativeDistance(true, 1000,
                    ShortcutGesturePolicy.nativeX(true, true, 1000, x), 24), 0);
    }
    @Test public void cameraAndOriginalHorizontalLayoutKeepTheirOwnAxes() {
        assertEquals(800, ShortcutGesturePolicy.nativeX(true, false, 1000, 800), 0);
        assertEquals(200, ShortcutGesturePolicy.nativeX(false, true, 100, 200), 0);
    }
    @Test public void nearEitherRimIsForgivenWithoutStealingItsNeighbourOrDistantTouches() {
        assertTrue(ShortcutGesturePolicy.shortcutHit(-29, 0, 24, 1, 65));
        assertFalse(ShortcutGesturePolicy.shortcutHit(-33, 0, 24, 1, 65));
        assertFalse(ShortcutGesturePolicy.shortcutHit(0, 29, 24, 1, 25));
        assertFalse(ShortcutGesturePolicy.shortcutHit(Float.NaN, 0, 24, 1, 65));
        assertTrue(ShortcutGesturePolicy.shortcutHit(-58, 0, 48, 2, 130));
    }
    @Test public void theSamePointCannotBelongToBothAdjacentShortcuts() {
        for (int x = -40; x <= 40; x++) for (int y = -40; y <= 100; y++) {
            float toTorch = (float) Math.hypot(x, y);
            float toCamera = (float) Math.hypot(x, y - 60);
            boolean torch = ShortcutGesturePolicy.shortcutHit(x, y, 24, 1, toCamera);
            boolean camera = ShortcutGesturePolicy.shortcutHit(x, y - 60, 24, 1, toTorch);
            assertFalse(torch && camera);
        }
    }
}
