package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;

import static org.junit.Assert.*;

public class ClockPropertyLeaseTest {
    @Test public void matchingNativeValueDoesNotAcquireOwnership() {
        ClockPropertyLease lease = new ClockPropertyLease();
        assertEquals(12f, lease.write(12f, 12f), 0f);
        assertFalse(lease.owned());
        assertEquals(18f, lease.restore(18f), 0f);
    }

    @Test public void firstCustomizationRestoresTheActualNativeBaseline() {
        ClockPropertyLease lease = new ClockPropertyLease();
        assertEquals(-42f, lease.write(7f, -42f), 0f);
        assertTrue(lease.owned());
        assertEquals(7f, lease.restore(-42f), 0f);
        assertFalse(lease.owned());
    }

    @Test public void repeatedFramesDoNotAdoptOurOwnOutput() {
        ClockPropertyLease lease = new ClockPropertyLease();
        float current = 7f;
        for (int frame = 0; frame < 100; frame++) current = lease.write(current, -42f);
        assertEquals(7f, lease.restore(current), 0f);
    }

    @Test public void unchangedOwnedOutputKeepsTheOriginalBaseline() {
        ClockPropertyLease lease = new ClockPropertyLease();
        lease.write(.95f, .4f);
        lease.write(.4f, .4f);
        assertTrue(lease.owned());
        assertEquals(.95f, lease.restore(.4f), 0f);
    }

    @Test public void nativeRewriteImmediatelyBeforeReleaseIsPreserved() {
        ClockPropertyLease lease = new ClockPropertyLease();
        lease.write(7f, -42f);
        assertEquals(18f, lease.restore(18f), 0f);
    }

    @Test public void nativeRewriteBeforeAnotherFrameBecomesTheNewBaseline() {
        ClockPropertyLease lease = new ClockPropertyLease();
        lease.write(7f, -42f);
        assertEquals(-30f, lease.write(18f, -30f), 0f);
        assertEquals(18f, lease.restore(-30f), 0f);
    }

    @Test public void anotherOwnerMatchingOurNewTargetStillReplacesTheOldBaseline() {
        ClockPropertyLease lease = new ClockPropertyLease();
        lease.write(7f, -42f);
        // Native layout now chooses the same value as our next target.
        lease.write(18f, 18f);
        assertEquals(18f, lease.restore(18f), 0f);
    }

    @Test public void releaseCannotReplayAnOldBaselineTwice() {
        ClockPropertyLease lease = new ClockPropertyLease();
        lease.write(7f, -42f);
        assertEquals(7f, lease.restore(-42f), 0f);
        assertEquals(-42f, lease.restore(-42f), 0f);
    }

    @Test public void aNewLeaseAfterReleaseCapturesTheNewNativeValue() {
        ClockPropertyLease lease = new ClockPropertyLease();
        lease.write(7f, -42f);
        lease.restore(-42f);
        lease.write(18f, -30f);
        assertEquals(18f, lease.restore(-30f), 0f);
    }

    @Test public void evenOneUlpOfForeignMovementIsNotRolledBack() {
        ClockPropertyLease lease = new ClockPropertyLease();
        lease.write(.95f, .4f);
        float nativeValue = Math.nextUp(.4f);
        assertEquals(nativeValue, lease.restore(nativeValue), 0f);
    }

    @Test public void separatePropertiesReleaseIndependently() {
        ClockPropertyLease x = new ClockPropertyLease();
        ClockPropertyLease scale = new ClockPropertyLease();
        x.write(7f, -42f);
        scale.write(.95f, .4f);
        assertEquals(18f, x.restore(18f), 0f);
        assertEquals(.95f, scale.restore(.4f), 0f);
    }
}
