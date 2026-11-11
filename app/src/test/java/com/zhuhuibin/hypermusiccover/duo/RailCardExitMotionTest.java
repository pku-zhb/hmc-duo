package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class RailCardExitMotionTest {
    @Test public void cardTopLeavesBelowTheScreenRegardlessOfItsStartingPosition() {
        for (float top : new float[]{300, 800, 1400})
            assertTrue(top + RailCardExitMotion.distance(1712, top, 44) > 1712);
    }
    @Test public void exitMovesOnlyDownAndFadesWithoutChangingTheFrame() {
        float last = -1;
        for (int i = 0; i <= 100; i++) {
            float t = i / 100f;
            float progress = RailCardExitMotion.progress(t);
            assertTrue(progress >= last && progress <= 1);
            assertTrue(RailCardExitMotion.alpha(t) >= 0 && RailCardExitMotion.alpha(t) <= 1);
            last = progress;
        }
        assertEquals(0, RailCardExitMotion.progress(0), 0);
        assertEquals(1, RailCardExitMotion.progress(1), 0);
        assertEquals(1, RailCardExitMotion.alpha(0), 0);
        assertEquals(0, RailCardExitMotion.alpha(1), 0);
    }
}
