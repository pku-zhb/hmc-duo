package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class NativeTranslationLeaseTest {
    @Test public void skippedTinyRecalculationDoesNotLoseTheActualWriteToken() {
        NativeTranslationLease lease = new NativeTranslationLease(12f);
        lease.wrote(-500f);
        // A newly desired -499.9 was not written because it was within the draw tolerance.
        assertEquals(12f, lease.restore(-500f), 0f);
    }
    @Test public void equalNativeWriteOnExitRevokesOwnershipBeforeRestoration() {
        NativeTranslationLease lease = new NativeTranslationLease(12f);
        lease.wrote(0f);
        lease.nativeWrite(0f);
        assertEquals(0f, lease.restore(0f), 0f);
    }
    @Test public void nativeChangesBecomeTheNewBaselineWhileOverridden() {
        NativeTranslationLease lease = new NativeTranslationLease(0f);
        lease.wrote(-500f);
        lease.nativeWrite(24f);
        lease.wrote(-500f);
        assertEquals(24f, lease.restore(-500f), 0f);
        assertEquals(40f, lease.restore(40f), 0f);
    }
    @Test public void releasingAnInactiveLeaseNeverRewritesNativeState() {
        NativeTranslationLease lease = new NativeTranslationLease(12f);
        lease.wrote(-500f);
        lease.release();
        assertEquals(-500f, lease.restore(-500f), 0f);
    }
}
