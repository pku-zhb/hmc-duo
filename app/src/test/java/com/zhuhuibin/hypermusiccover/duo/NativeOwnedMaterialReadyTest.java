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
}
