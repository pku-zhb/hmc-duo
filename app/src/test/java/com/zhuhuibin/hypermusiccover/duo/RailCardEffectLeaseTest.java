package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class RailCardEffectLeaseTest {
    @Test public void aMaskedLowerCardCannotAdoptOrRestoreItsNeighbour() {
        Object lowerNative = new Object(), upperNative = new Object(), mask = new Object();
        RailCardEffectLease<Object> lower = new RailCardEffectLease<>(lowerNative);
        RailCardEffectLease<Object> upper = new RailCardEffectLease<>(upperNative);
        lower.committed(lowerNative, mask, true);
        assertTrue(lower.owns(mask));
        assertFalse(upper.owns(mask));
        assertSame(lowerNative, lower.nativeRead(mask));
        assertSame(upperNative, upper.base);
        assertFalse(upper.masked);
    }

    @Test public void anotherEffectWriterSeesAndUpdatesItsOwnNativeBaseline() {
        Object original = new Object(), firstMask = new Object(), nativeBlur = new Object(), newMask = new Object();
        RailCardEffectLease<Object> lease = new RailCardEffectLease<>(original);
        lease.committed(original, firstMask, true);
        assertSame(original, lease.nativeRead(firstMask));
        lease.committed(nativeBlur, newMask, true);
        assertSame(nativeBlur, lease.base);
        assertSame(nativeBlur, lease.nativeRead(newMask));
        assertTrue(lease.owns(newMask));
        assertFalse(lease.owns(firstMask));
        // A newer unobserved value must never be overwritten during cleanup.
        assertFalse(lease.owns(new Object()));
    }

    @Test public void handingBackTheSameOutputCannotRecursivelyCaptureTheTemporaryMask() {
        Object original = new Object(), mask = new Object();
        RailCardEffectLease<Object> lease = new RailCardEffectLease<>(original);
        lease.committed(original, mask, true);
        Object nativeInput = lease.nativeInput(mask);
        assertSame(original, nativeInput);
        lease.committed(nativeInput, nativeInput, false);
        assertSame(original, lease.base);
        assertSame(original, lease.written);
        assertFalse(lease.masked);
    }

    @Test public void skippedOrFailedNativeBridgeCannotCommitAViewEntry() {
        Object original = new Object(), mask = new Object(), attempted = new Object();
        RailCardEffectLease<Object> lease = new RailCardEffectLease<>(original);
        lease.committed(original, mask, true);
        // View hooks may replace/skip a write or throw before RenderNode. Preparing the
        // input is read-only; only a successful native bridge completion can commit it.
        assertSame(attempted, lease.nativeInput(attempted));
        assertSame(original, lease.base);
        assertTrue(lease.owns(mask));
    }

    @Test public void outerLastViewReentryFollowsActualNativeCommitOrder() {
        Object first = new Object(), outer = new Object(), inner = new Object();
        RailCardEffectLease<Object> lease = new RailCardEffectLease<>(first);
        NativeMaterialCommitScope<Object> scopes = new NativeMaterialCommitScope<>();
        Object node = new Object();
        NativeMaterialCommitScope.Frame<Object> outerView = scopes.enter(outer, node, 0, 0, false);
        NativeMaterialCommitScope.Frame<Object> innerView = scopes.enter(inner, node, 0, 0, false);
        // A downstream View hook reenters before the outer original body reaches RN.
        Object actualNodeEffect = scopes.matching(node).target;
        lease.committed(actualNodeEffect, actualNodeEffect, false);
        scopes.leave(innerView);
        assertSame(inner, lease.written);
        // The outer native write happens LAST even though its View setter entered first.
        actualNodeEffect = scopes.matching(node).target;
        lease.committed(actualNodeEffect, actualNodeEffect, false);
        scopes.leave(outerView);
        assertSame(outer, actualNodeEffect);
        assertSame(actualNodeEffect, lease.base);
        assertSame(actualNodeEffect, lease.written);
        assertFalse(lease.masked);
    }

    @Test public void downstreamArgumentReplacementIsTheNativeBaseline() {
        Object original = new Object(), requested = new Object(), replacement = new Object();
        RailCardEffectLease<Object> lease = new RailCardEffectLease<>(original);
        assertSame(requested, lease.nativeInput(requested));
        // Only the replacement reaches the verified RenderNode setter.
        lease.committed(replacement, replacement, false);
        assertSame(replacement, lease.base);
        assertSame(replacement, lease.written);
    }

    @Test public void nativeNullEffectsAreRealBaselinesAndRestoreCleanly() {
        Object mask = new Object();
        RailCardEffectLease<Object> lease = new RailCardEffectLease<>(null);
        lease.committed(null, mask, true);
        assertNull(lease.nativeRead(mask));
        lease.committed(null, null, false);
        assertNull(lease.base);
        assertNull(lease.written);
        assertFalse(lease.masked);
    }
}
