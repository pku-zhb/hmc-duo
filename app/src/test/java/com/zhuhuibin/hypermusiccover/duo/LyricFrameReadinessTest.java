package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class LyricFrameReadinessTest {
    @Test public void anUnpublishedPlanWaitsInsteadOfLayingOutAtFullWindowWidth() {
        LyricFrameReadiness state = new LyricFrameReadiness();
        assertTrue(state.pending(1, 1168, 1712));
    }

    @Test public void measuredInsufficientRoomAllowsTheExistingSingleColumnFallback() {
        LyricFrameReadiness state = new LyricFrameReadiness();
        // Evaluation is separate from whether the shared plan produced any columns.
        assertNull(FoldMusicLayout.columns(775, 650, 1, 650, 0, 24, 0, 24, 80,
                new FoldMusicLayout.Box(0, 145, 400, 290), Float.NaN));
        state.measured(1, 775, 650);
        assertFalse(state.pending(1, 775, 650));
    }

    @Test public void rotationRecreationAndInvalidGeometryCannotReuseAFormerDecision() {
        LyricFrameReadiness state = new LyricFrameReadiness();
        state.measured(1, 1168, 1712);
        assertTrue(state.pending(2, 1168, 1712));
        assertTrue(state.pending(1, 1712, 1168));
        state.reset();
        assertTrue(state.pending(1, 1168, 1712));
        state.measured(1, 0, 1712);
        assertTrue(state.pending(1, 0, 1712));
    }
}
