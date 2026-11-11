package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class RailColumnMotionTest {
    @Test public void capsuleFollowsTheActualMiduoColumnExactlyOnce() {
        float restX = 1061, restY = 1511, span = 150;
        // MiDuo's doze compensation is deliberately different from the root's .95 zoom.
        RailColumnMotion.Transform m = RailColumnMotion.follow(restX, restY, span,
                1048, 1490, span * .98f);
        assertNotNull(m);
        assertEquals(1048, restX * m.scale + m.x, .001f);
        assertEquals(1490, restY * m.scale + m.y, .001f);
        assertEquals(.98f, m.scale, .00001f);
    }
    @Test public void everyWakeFrameKeepsTheCapsuleAndButtonsOnOneHorizontalCenter() {
        for (float scale : new float[]{.95f, .96f, .98f, 1f}) {
            float x = 584 + (1061 - 584) * scale;
            RailColumnMotion.Transform m = RailColumnMotion.follow(1061, 1511, 150,
                    x, 1500, 150 * scale);
            assertNotNull(m);
            assertEquals(x, 1061 * m.scale + m.x, .001f);
        }
    }
    @Test public void incompleteOrImpossibleColumnMeasurementsAreRejected() {
        assertNull(RailColumnMotion.follow(1061, 1511, 0, 1048, 1490, 150));
        assertNull(RailColumnMotion.follow(1061, 1511, 150, Float.NaN, 1490, 150));
        assertNull(RailColumnMotion.follow(1061, 1511, 150, 1048, 1490, 30));
    }
}
