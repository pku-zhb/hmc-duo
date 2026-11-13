package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class RailEdgeCommitPolicyTest {
    @Test public void aNativeMaterialWriteDuringTheSwipeCannotWaitForTheNextPredraw() {
        assertTrue(RailEdgeCommitPolicy.maskNow(RailEdgeCommitPolicy.NATIVE, true, true));
        assertTrue(RailEdgeCommitPolicy.maskNow(RailEdgeCommitPolicy.MASK, true, true));
    }

    @Test public void cleanupAlwaysReturnsTheUnmaskedNativeMaterialEvenIfTheFingerHasNotReturned() {
        assertFalse(RailEdgeCommitPolicy.maskNow(RailEdgeCommitPolicy.RESTORE, true, true));
    }

    @Test public void stationaryUnsupportedOrUnknownWritesRemainNative() {
        assertFalse(RailEdgeCommitPolicy.maskNow(RailEdgeCommitPolicy.NATIVE, false, true));
        assertFalse(RailEdgeCommitPolicy.maskNow(RailEdgeCommitPolicy.NATIVE, true, false));
        assertFalse(RailEdgeCommitPolicy.maskNow(999, true, true));
    }
}
