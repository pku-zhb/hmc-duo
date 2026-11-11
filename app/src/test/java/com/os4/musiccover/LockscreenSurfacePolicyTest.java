package com.os4.musiccover;

import org.junit.Test;
import static org.junit.Assert.*;

public class LockscreenSurfacePolicyTest {
    @Test public void fingerprintExitHidesOwnedLayersBeforeTheLockStateChanges() {
        assertTrue(LockscreenSurfacePolicy.visible(true, false));
        assertFalse(LockscreenSurfacePolicy.visible(true, true));
        assertFalse(LockscreenSurfacePolicy.visible(false, true));
        assertFalse(LockscreenSurfacePolicy.visible(false, false));
    }
    @Test public void directAodAndAwakeFingerprintUnlockCannotKeepTheButtonHold() {
        assertTrue(LockscreenSurfacePolicy.holdButtons(true, false, false, false));
        assertFalse(LockscreenSurfacePolicy.holdButtons(true, true, false, false));
        assertFalse(LockscreenSurfacePolicy.holdButtons(false, true, false, false));
    }
    @Test public void swipeToBouncerAndCustomAodStillBelongToNativeFades() {
        assertFalse(LockscreenSurfacePolicy.holdButtons(true, false, true, false));
        assertFalse(LockscreenSurfacePolicy.holdButtons(true, false, false, true));
    }
}
