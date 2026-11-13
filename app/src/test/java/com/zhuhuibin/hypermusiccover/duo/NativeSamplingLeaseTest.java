package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class NativeSamplingLeaseTest {
    @Test public void aLocalArtworkLeaseRestoresTheActualNativeDomain() {
        NativeSamplingLease lease = new NativeSamplingLease();
        lease.bind(0, true, 0);
        assertTrue(lease.needsApply(true));
        assertFalse(lease.target(true).passWindow);
        assertEquals(1, lease.target(true).mode);
        assertEquals(1, lease.target(true).blurType);
        lease.committed(true);
        assertFalse(lease.needsApply(true));
        assertTrue(lease.needsApply(false));
        assertTrue(lease.target(false).passWindow);
        assertEquals(0, lease.target(false).mode);
        assertEquals(0, lease.target(false).blurType);
        lease.committed(false);
        assertFalse(lease.needsApply(false));
    }

    @Test public void laterNativeUpdatesReplaceOnlyTheirOwnBaselineProperty() {
        NativeSamplingLease lease = new NativeSamplingLease();
        lease.bind(0, true, 0);
        lease.committed(true);
        lease.nativeBlurType(3);
        assertTrue(lease.needsApply(true));
        assertTrue(lease.nativeFlags().passWindow);
        assertEquals(3, lease.nativeFlags().blurType);
        assertEquals(0, lease.nativeFlags().mode);
        assertFalse(lease.target(true).passWindow);
        assertEquals(1, lease.target(true).blurType);
        lease.committed(true);
        lease.nativePassWindow(false);
        lease.nativeMode(2);
        assertFalse(lease.target(false).passWindow);
        assertEquals(3, lease.target(false).blurType);
        assertEquals(2, lease.target(false).mode);
    }

    @Test public void aReplacementSourceNeverInheritsThePreviousOverride() {
        NativeSamplingLease lease = new NativeSamplingLease();
        lease.bind(0, true, 0);
        lease.committed(true);
        lease.clear();
        assertFalse(lease.needsApply(false));
        lease.bind(2, false, 4);
        assertFalse(lease.local());
        assertFalse(lease.nativeFlags().passWindow);
        assertEquals(4, lease.nativeFlags().blurType);
        assertEquals(2, lease.nativeFlags().mode);
    }
}
