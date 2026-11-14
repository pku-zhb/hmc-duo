package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;

import static org.junit.Assert.*;

public class NativeArtworkEffectStateTest {
    private final NativeArtworkEffectState<Object> state = new NativeArtworkEffectState<>();

    private void apply(Object effect) {
        assertTrue(state.write(effect));
        state.observe(effect);
    }

    @Test public void anUnknownNativeEffectCannotBeReplacedOrAssumedNull() {
        assertFalse(state.known());
        assertFalse(state.write(new Object()));
        assertFalse(state.write(null));
        assertFalse(state.owns());
        assertNull(state.release());
        assertFalse(state.known());
    }

    @Test public void anObservedNativeNullCanBeRestoredAfterOurBlur() {
        state.observe(null);
        assertTrue(state.known());
        Object blur = new Object();
        apply(blur);
        assertTrue(state.owns());
        assertSame(blur, state.current());
        assertNull(state.release());
        assertFalse(state.owns());
        // The runtime applies the returned value through the real setter.
        state.observe(null);
        assertNull(state.current());
    }

    @Test public void aForeignEffectInstalledAfterOurBlurSurvivesRelease() {
        Object nativeEffect = new Object(), blur = new Object(), foreign = new Object();
        state.observe(nativeEffect);
        apply(blur);
        state.observe(foreign);
        assertFalse(state.owns());
        assertSame(foreign, state.release());
    }

    @Test public void anotherCustomizationRestoresTheLatestForeignEffect() {
        Object nativeEffect = new Object(), blur = new Object();
        Object foreign = new Object(), strongerBlur = new Object();
        state.observe(nativeEffect);
        apply(blur);
        state.observe(foreign);
        apply(strongerBlur);
        assertSame(foreign, state.release());
    }

    @Test public void requestingTheUnchangedNativeEffectDoesNotAcquireIt() {
        Object nativeEffect = new Object();
        state.observe(nativeEffect);
        assertFalse(state.write(nativeEffect));
        assertFalse(state.owns());
        assertSame(nativeEffect, state.release());
    }

    @Test public void requestingOurUnchangedEffectDoesNotOverwriteItsBaseline() {
        Object nativeEffect = new Object(), blur = new Object();
        state.observe(nativeEffect);
        apply(blur);
        assertFalse(state.write(blur));
        assertTrue(state.owns());
        assertSame(nativeEffect, state.release());
    }

    @Test public void repeatedReleaseCannotRestoreAnOlderEffectAgain() {
        Object nativeEffect = new Object(), blur = new Object(), foreign = new Object();
        state.observe(nativeEffect);
        apply(blur);
        assertSame(nativeEffect, state.release());
        state.observe(foreign);
        assertSame(foreign, state.release());
        assertFalse(state.owns());
    }

    @Test public void effectIdentityMattersEvenWhenObjectsCompareEqual() {
        Object nativeEffect = new String("same"), blur = new String("same");
        assertEquals(nativeEffect, blur);
        state.observe(nativeEffect);
        apply(blur);
        assertSame(nativeEffect, state.release());
    }

    @Test public void anUnappliedReservationDoesNotClaimTheCurrentNativeEffect() {
        Object nativeEffect = new Object(), blur = new Object();
        state.observe(nativeEffect);
        assertTrue(state.write(blur));
        assertFalse(state.owns());
        assertSame(nativeEffect, state.current());
        assertSame(nativeEffect, state.release());
    }

    @Test public void aNativeClearAfterOurBlurIsNotReplacedByAnOlderEffect() {
        Object nativeEffect = new Object();
        state.observe(nativeEffect);
        apply(new Object());
        state.observe(null);
        assertTrue(state.known());
        assertFalse(state.owns());
        assertNull(state.release());
    }
}
