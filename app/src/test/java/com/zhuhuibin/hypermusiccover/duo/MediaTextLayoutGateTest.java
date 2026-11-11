package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class MediaTextLayoutGateTest {
    @Test public void oneTextLayoutPassPrecedesTheFirstDrawAfterAnInsetChange() {
        MediaTextLayoutGate gate = new MediaTextLayoutGate();
        assertTrue(gate.allow(false));
        assertFalse(gate.allow(true));
        assertTrue(gate.allow(false));
    }
    @Test public void repeatedOemPaddingWritesCannotBlockTheWindowIndefinitely() {
        MediaTextLayoutGate gate = new MediaTextLayoutGate();
        assertFalse(gate.allow(true));
        for (int i = 0; i < 100; i++) assertTrue(gate.allow(true));
    }
    @Test public void aLaterVisibilityChangeGetsItsOwnSingleLayoutPass() {
        MediaTextLayoutGate gate = new MediaTextLayoutGate();
        assertFalse(gate.allow(true));
        assertTrue(gate.allow(false));
        assertFalse(gate.allow(true));
        assertTrue(gate.allow(true));
    }
}
