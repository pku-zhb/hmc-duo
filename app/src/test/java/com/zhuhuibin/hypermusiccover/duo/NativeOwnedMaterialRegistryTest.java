package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class NativeOwnedMaterialRegistryTest {
    @Test public void interfaceOrProbeIdentityAloneNeverGrantsRegistration() {
        NativeOwnedMaterialRegistry<Object> registry = new NativeOwnedMaterialRegistry<>();
        Object probe = new Object();
        assertEquals(0, registry.role(probe));
        assertNull(registry.owner(probe));
        assertFalse(registry.claim(probe, 1));
        assertFalse(registry.register(probe, probe));
    }
    @Test public void eitherExactMemberClaimsTheSamePairWithoutClaimingOtherSurfaces() {
        NativeOwnedMaterialRegistry<Object> registry = new NativeOwnedMaterialRegistry<>();
        Object element = new Object(), container = new Object(), other = new Object(), otherContainer = new Object();
        assertTrue(registry.register(element, container));
        assertTrue(registry.register(other, otherContainer));
        assertSame(element, registry.owner(container));
        assertEquals(1, registry.role(element)); assertEquals(2, registry.role(container));
        assertFalse(registry.claim(container, 2));
        assertFalse(registry.claimed(element));
        assertTrue(registry.claim(container, 1));
        assertTrue(registry.claimed(element)); assertTrue(registry.claimed(container));
        assertFalse(registry.claimed(other)); assertFalse(registry.claimed(otherContainer));
        assertTrue(registry.register(element, container));
        assertTrue(registry.claimed(element));
    }
    @Test public void liveSiblingCannotReplaceTheOwnerOrEscapeItsProviderClaim() {
        NativeOwnedMaterialRegistry<Object> registry = new NativeOwnedMaterialRegistry<>();
        Object element = new Object(), sibling = new Object(), container = new Object();
        assertTrue(registry.register(element, container)); assertTrue(registry.claim(element, 1));
        assertFalse(registry.register(sibling, container));
        assertSame(element, registry.owner(container)); assertTrue(registry.claimed(container));
        assertEquals(0, registry.role(sibling)); assertNull(registry.owner(sibling));
        assertEquals(1, registry.owners().size());
    }
    @Test public void retiringAReparentedPairAllowsReuseWithoutTransferringItsClaim() {
        NativeOwnedMaterialRegistry<Object> registry = new NativeOwnedMaterialRegistry<>();
        Object element = new Object(), container = new Object(), next = new Object(), nextContainer = new Object();
        registry.register(element, container); registry.claim(element, 1);
        assertFalse(registry.register(element, nextContainer));
        registry.remove(element);
        assertEquals(0, registry.role(container)); assertFalse(registry.claimed(container));
        assertTrue(registry.register(next, container));
        assertTrue(registry.register(element, nextContainer));
        assertSame(next, registry.owner(container)); assertSame(element, registry.owner(nextContainer));
        assertFalse(registry.claimed(next)); assertFalse(registry.claimed(element));
    }
    @Test public void equalButDistinctObjectsCannotClaimAnExactRegisteredMember() {
        NativeOwnedMaterialRegistry<Object> registry = new NativeOwnedMaterialRegistry<>();
        Object element = new String("element"), impostor = new String("element"), container = new Object();
        assertTrue(registry.register(element, container));
        assertEquals(0, registry.role(impostor)); assertNull(registry.owner(impostor));
        assertFalse(registry.claim(impostor, 1)); assertFalse(registry.claimed(element));
    }
}
