package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class ContentMotionSnapshotTest {
    @Test public void aReplacedNativeAffineOwnerCannotBorrowAnOldLogicalPose() {
        Object window = new Object(), owner = new Object();
        ContentMotionSnapshot frame = new ContentMotionSnapshot(window, 3, owner,
                new FoldMusicLayout.Box(20, 300, 1020, 1300));
        assertTrue(frame.current(window, 3, owner));
        assertFalse(frame.current(new Object(), 3, owner));
        assertFalse(frame.current(window, 4, owner));
        assertFalse(frame.current(window, 3, new Object()));
        assertFalse(frame.current(window, 3, null));
    }

    @Test public void aColumnPlanUsesLayoutOffsetsWithoutInvertingTheNativeAnimation() {
        FoldMusicLayout.Box windowPlan = new FoldMusicLayout.Box(1100, 300, 2100, 1400);
        // Layout offsets are the complete input. A native scale/translation must not
        // enter the conversion, otherwise retained typography reflows at every AOD frame.
        FoldMusicLayout.Box logical = ContentMotionSnapshot.logicalPlan(windowPlan, 20, 40);
        assertEquals(1000f, logical.width(), 0f);
        assertEquals(1100f, logical.height(), 0f);
        assertEquals(1080f, logical.left, 0f);
        assertEquals(260f, logical.top, 0f);
        assertNull(ContentMotionSnapshot.logicalPlan(windowPlan, Float.NaN, 0));
        assertNull(ContentMotionSnapshot.logicalPlan(null, 0, 0));
    }
}
