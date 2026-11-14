package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class LyricGeometryAnchorTest {
    @Test public void movingBandCarriesGeometryWithoutAddingFocusLag() {
        // The focus was still 6px short; native geometry moves its target by30px this pass.
        assertEquals(44f, LyricGeometryAnchor.rebase(14f, 20f, 50f), 0f);
        assertEquals(6f, 50f - LyricGeometryAnchor.rebase(14f, 20f, 50f), 0f);
    }
    @Test public void finalFrameAndReverseUseSameEndpoint() {
        float at = LyricGeometryAnchor.rebase(20f, 20f, 80f);
        assertEquals(80f, at, 0f);
        assertEquals(20f, LyricGeometryAnchor.rebase(at, 80f, 20f), 0f);
    }
    @Test public void unavailableBandDoesNotPublishNanIntoAnchor() {
        assertEquals(20f, LyricGeometryAnchor.rebase(20f, Float.NaN, 80f), 0f);
    }
}
