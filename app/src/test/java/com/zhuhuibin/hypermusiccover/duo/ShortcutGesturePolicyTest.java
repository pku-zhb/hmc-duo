package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class ShortcutGesturePolicyTest {
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
}
