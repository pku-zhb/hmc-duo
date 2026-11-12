package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class NativeMaterialSourceBindingTest {
    @Test public void stableBindingValidatesOnceAndUnrelatedViewsNeverInvokeDiscovery() {
        NativeMaterialSourceBinding<Object> binding = new NativeMaterialSourceBinding<>();
        Object source = new Object(), parent = new Object(), owner = new Object(), root = new Object();
        AtomicInteger scans = new AtomicInteger();
        assertTrue(binding.bind(source, parent, owner, root, () -> { scans.incrementAndGet(); return true; }));
        for (int i = 0; i < 100; ++i) {
            assertTrue(binding.bind(source, parent, owner, root, () -> { scans.incrementAndGet(); return true; }));
            assertFalse(binding.isSource(new Object()));
        }
        assertEquals(1, scans.get());
    }

    @Test public void replacingMediaWithRejectedIslandDropsTheOldSource() {
        NativeMaterialSourceBinding<Object> binding = new NativeMaterialSourceBinding<>();
        Object source = new Object(), parent = new Object(), owner = new Object(), root = new Object();
        assertTrue(binding.bind(source, parent, owner, root, () -> true));
        Object island = new Object();
        assertFalse(binding.bind(island, parent, owner, root, () -> false));
        assertFalse(binding.isSource(source));
        assertFalse(binding.isSource(island));
        assertNull(binding.owner());
    }

    @Test public void reparentingEitherTheBackgroundOrItsMediaOwnerRequiresValidationAgain() {
        NativeMaterialSourceBinding<Object> binding = new NativeMaterialSourceBinding<>();
        Object source = new Object(), parent = new Object(), owner = new Object(), root = new Object();
        AtomicInteger scans = new AtomicInteger();
        assertTrue(binding.bind(source, parent, owner, root, () -> { scans.incrementAndGet(); return true; }));
        Object nextParent = new Object(), nextRoot = new Object();
        assertFalse(binding.matches(source, nextParent, owner, root));
        assertTrue(binding.isSource(source));
        assertTrue(binding.bind(source, nextParent, owner, root, () -> { scans.incrementAndGet(); return true; }));
        assertSame(source, binding.source());
        assertFalse(binding.matches(source, nextParent, owner, nextRoot));
        assertTrue(binding.bind(source, nextParent, owner, nextRoot, () -> { scans.incrementAndGet(); return true; }));
        assertSame(source, binding.source());
        assertEquals(3, scans.get());
    }

    @Test public void reparentingTheSameBackgroundIntoARejectedHostInvalidatesItsIdentity() {
        NativeMaterialSourceBinding<Object> binding = new NativeMaterialSourceBinding<>();
        Object source = new Object(), parent = new Object(), owner = new Object(), root = new Object();
        assertTrue(binding.bind(source, parent, owner, root, () -> true));
        assertTrue(binding.isSource(source));
        assertFalse(binding.bind(source, new Object(), owner, root, () -> false));
        assertFalse(binding.isSource(source));
        assertNull(binding.source());
        assertNull(binding.owner());
    }

    @Test public void missingOrDetachedSourceDoesNotLeaveAnEligibleCachedIdentity() {
        NativeMaterialSourceBinding<Object> binding = new NativeMaterialSourceBinding<>();
        Object source = new Object(), parent = new Object(), owner = new Object(), root = new Object();
        assertTrue(binding.bind(source, parent, owner, root, () -> true));
        assertFalse(binding.bind(source, null, owner, root, () -> { fail("no hierarchy to validate"); return true; }));
        assertFalse(binding.isSource(source));
        assertFalse(binding.isSource(null));
        assertTrue(binding.bind(source, parent, owner, root, () -> true));
        binding.clear();
        assertFalse(binding.matches(source, parent, owner, root));
    }
}
