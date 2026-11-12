package com.zhuhuibin.hypermusiccover.duo;

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
    @Test public void fullScreenAodKeepsStandaloneShortcutsWithoutAnyCapsule() {
        assertTrue(LockscreenSurfacePolicy.startButtonHold(true, true, false, true, false));
        assertTrue(LockscreenSurfacePolicy.startButtonHold(true, true, true, true, false));
        assertFalse(LockscreenSurfacePolicy.startButtonHold(true, false, true, true, false));
    }
    @Test public void railCanHoldTheSleepFadeBeforeDozingArrives() {
        assertTrue(LockscreenSurfacePolicy.startButtonHold(true, true, false, false, true));
        assertFalse(LockscreenSurfacePolicy.startButtonHold(true, true, false, false, false));
        // An ordinary awake screen or unprepared/custom AOD does not start a new hold.
        assertFalse(LockscreenSurfacePolicy.startButtonHold(true, true, true, false, false));
    }
    @Test public void horizontalLayoutKeepsItsExistingAodContentGate() {
        assertFalse(LockscreenSurfacePolicy.startButtonHold(false, true, false, true, false));
        assertTrue(LockscreenSurfacePolicy.startButtonHold(false, true, true, true, false));
        assertFalse(LockscreenSurfacePolicy.startButtonHold(false, true, true, false, true));
    }
    @Test public void aSleepThatNeverEntersAodCannotLeaveAnIndefiniteHold() {
        LockscreenSurfacePolicy.AodEntry entry = new LockscreenSurfacePolicy.AodEntry();
        entry.prepare(true, 100);
        assertTrue(entry.active(100));
        assertTrue(entry.active(2099));
        assertFalse(entry.active(2100));
        entry.prepare(false, 3000);
        assertFalse(entry.active(3001));
    }
    @Test public void actualCustomAodOverridesAPreparedFullScreenTransition() {
        LockscreenSurfacePolicy.AodEntry entry = new LockscreenSurfacePolicy.AodEntry();
        entry.prepare(true, 100);
        entry.observe(true, false, false);
        assertFalse(entry.active(200));
        assertFalse(LockscreenSurfacePolicy.holdButtons(true, false, false, true));
        assertFalse(LockscreenSurfacePolicy.startButtonHold(true, true, false, false, entry.active(200)));
    }
    @Test public void unlockBouncerAndUserDragCancelThePreparedSleepFade() {
        LockscreenSurfacePolicy.AodEntry entry = new LockscreenSurfacePolicy.AodEntry();
        entry.prepare(true, 100);
        entry.observe(false, true, false);
        assertFalse(entry.active(200));
        entry.prepare(true, 300);
        entry.observe(false, false, true);
        assertFalse(entry.active(400));
        entry.prepare(true, 500);
        entry.cancel();
        assertFalse(entry.active(600));
    }
}
