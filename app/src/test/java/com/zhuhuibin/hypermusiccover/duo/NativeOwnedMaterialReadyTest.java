package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class NativeOwnedMaterialReadyTest {
    @Test public void parentSizeBeforeChildLayoutDoesNotConsumeFirstReadyApplication() {
        NativeOwnedMaterialReady gate = new NativeOwnedMaterialReady();
        gate.applied(false); // Registered while detached or at 0x0.
        assertFalse(gate.update(false)); // Parent sized, child still 0x0.
        assertTrue(gate.update(true)); // Child's native layout completes.
        gate.applied(true);
        assertFalse(gate.update(true));
    }
    @Test public void lateHostVisibilityAndDetachRequireExactlyOneFreshApplication() {
        NativeOwnedMaterialReady gate = new NativeOwnedMaterialReady();
        assertFalse(gate.update(false)); // Attached/measured, clock sibling still invisible.
        assertTrue(gate.update(true)); // Native global-layout visibility event, before draw.
        assertFalse(gate.update(true));
        gate.applied(false); // Detach, unlock, or reparent.
        assertTrue(gate.update(true));
        gate.applied(false); // Failed reapply does not consume readiness.
        assertTrue(gate.update(true));
    }
    @Test public void unchangedSceneNotificationsNeverReapplyAndUnblockingIsNotDropped() {
        NativeOwnedMaterialReady gate = new NativeOwnedMaterialReady();
        gate.applied(true);
        for (int i = 0; i < 1000; ++i) assertFalse(gate.update(true));
        assertFalse(gate.update(false)); // Explicit control-center/editor blocking event.
        for (int i = 0; i < 1000; ++i) assertFalse(gate.update(false));
        assertTrue(gate.update(true)); // Explicit collapsed/closed event, with no layout required.
        assertFalse(gate.update(true)); // Duplicate native keyguard notification.
        gate.applied(false); // A forced settings refresh failed: recovery must remain possible.
        assertTrue(gate.update(true));
    }
    @Test public void coalescedBlockThenReadyStillRequiresOneCurrentRecipe() {
        NativeOwnedMaterialReady gate = new NativeOwnedMaterialReady();
        gate.applied(true);
        gate.applied(false); // Preserve a queued blocked event even if it has already closed.
        assertTrue(gate.update(true));
        assertFalse(gate.update(true));
    }
}
