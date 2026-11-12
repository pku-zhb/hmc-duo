package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class GlassProfileTest {
    @Test public void originalAndConsumerArraysCannotMutateTheCapturedNativeProfile() {
        float[] nativeValues = new float[42]; nativeValues[34] = .48f;
        GlassProfile p = GlassProfile.capture(nativeValues, 32, true);
        nativeValues[34] = 9;
        float[] target = p.forScene(32, true);
        assertEquals(.48f, target[34], 0);
        target[34] = 12;
        assertEquals(.48f, p.forScene(32, true)[34], 0);
    }
    @Test public void AodAndNightProfilesAreNeverAppliedToAnotherScene() {
        GlassProfile p = GlassProfile.capture(new float[42], 32, false);
        assertNull(p.forScene(32, true));
        assertNull(p.forScene(16, false));
        assertNotNull(p.forScene(32, false));
    }
    @Test public void UnknownOrInvalidVendorArraysKeepTheExistingMaterial() {
        assertNull(GlassProfile.capture(null, 32, true));
        assertNull(GlassProfile.capture(new float[41], 32, true));
        assertNull(GlassProfile.capture(new float[43], 32, true));
        float[] invalid = new float[42]; invalid[36] = Float.NaN;
        assertNull(GlassProfile.capture(invalid, 32, true));
    }
    @Test public void onlyChangedFinalValuesTriggerAnotherMaterialRefresh() {
        float[] values = new float[42]; values[36] = 1;
        GlassProfile p = GlassProfile.capture(values, 32, true);
        assertTrue(p.same(GlassProfile.capture(values, 32, true)));
        values[36] = .5f;
        assertFalse(p.same(GlassProfile.capture(values, 32, true)));
        assertFalse(p.same(GlassProfile.capture(values, 16, true)));
    }
    @Test public void nativeBlurRadiiKeepZeroAndTheirSceneWithoutSharingAnArray() {
        GlassProfile.Blur p = GlassProfile.Blur.capture(0, 90, 32, true);
        assertNotNull(p);
        int[] values = p.forScene(32, true);
        assertEquals(0, values[0]); assertEquals(90, values[1]);
        values[1] = 999;
        assertEquals(90, p.forScene(32, true)[1]);
        assertNull(p.forScene(32, false));
        assertNull(GlassProfile.Blur.capture(-1, 90, 32, true));
        assertTrue(p.same(GlassProfile.Blur.capture(0, 90, 32, true)));
    }
    @Test public void committedBackgroundPaletteIsDeepCopiedAndSceneBound() {
        int[] colors = {0x44556677}, modes = {101};
        GlassProfile.Background state = GlassProfile.Background.capture(30, .75f, colors, modes, 32, true);
        colors[0] = 0; modes[0] = 0;
        assertArrayEquals(new int[]{0x44556677}, state.colors());
        assertArrayEquals(new int[]{101}, state.modes());
        state.colors()[0] = 4;
        assertEquals(0x44556677, state.colors()[0]);
        assertTrue(state.forScene(32, true));
        assertFalse(state.forScene(16, true));
        assertFalse(state.forScene(32, false));
        assertTrue(state.same(GlassProfile.Background.capture(30, .75f, new int[]{0x44556677}, new int[]{101}, 32, true)));
        assertFalse(state.same(GlassProfile.Background.capture(31, .75f, new int[]{0x44556677}, new int[]{101}, 32, true)));
    }
    @Test public void anEmptyCommittedPaletteIsAValidClearButInvalidStateIsRejected() {
        assertNotNull(GlassProfile.Background.capture(0, 0f, new int[0], new int[0], 16, false));
        assertNull(GlassProfile.Background.capture(0, Float.NaN, new int[0], new int[0], 16, false));
        assertNull(GlassProfile.Background.capture(-1, 1f, new int[0], new int[0], 16, false));
        assertNull(GlassProfile.Background.capture(0, 1f, new int[]{1}, new int[0], 16, false));
    }
}
