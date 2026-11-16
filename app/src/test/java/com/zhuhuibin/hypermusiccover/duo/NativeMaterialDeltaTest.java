package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class NativeMaterialDeltaTest {
    private static GlassProfile.Background background(int radius, float scale, int color, int night, boolean awake) {
        return GlassProfile.Background.capture(radius, scale, new int[]{color}, new int[]{3}, night, awake);
    }
    @Test public void identicalNativeFramesNeverInvalidateAnyChannel() {
        float[] values = new float[42];
        GlassProfile glass = GlassProfile.capture(values, 16, true);
        GlassProfile.Blur blur = GlassProfile.Blur.capture(12, 36, 16, true);
        GlassProfile.Background background = background(48, 0.5f, 0xff777777, 16, true);
        for (int i = 0; i < 1000; ++i) assertEquals(0, NativeMaterialDelta.between(
                GlassProfile.capture(values, 16, true), GlassProfile.Blur.capture(12, 36, 16, true),
                background(48, 0.5f, 0xff777777, 16, true), glass, blur, background));
    }
    @Test public void paletteOnlyAodFramesNeverReplayStaticContainerChannels() {
        GlassProfile glass = GlassProfile.capture(new float[42], 16, true);
        GlassProfile.Blur blur = GlassProfile.Blur.capture(12, 36, 16, true);
        GlassProfile.Background previous = background(48, 0.5f, 0xff777777, 16, true);
        for (int i = 0; i < 100; ++i) {
            GlassProfile.Background next = background(48, 0.5f, 0xff777778 + i, 16, true);
            assertEquals(NativeMaterialDelta.PALETTE, NativeMaterialDelta.between(glass, blur, next, glass, blur, previous));
            previous = next;
        }
    }
    @Test public void independentRealChangesAndNewSceneAreNotDropped() {
        GlassProfile.Background previous = background(48, 0.5f, 7, 16, true);
        assertEquals(NativeMaterialDelta.RADIUS, NativeMaterialDelta.between(null, null,
                background(60, 0.5f, 7, 16, true), null, null, previous));
        assertEquals(NativeMaterialDelta.SCALE, NativeMaterialDelta.between(null, null,
                background(48, 0.75f, 7, 16, true), null, null, previous));
        assertEquals(NativeMaterialDelta.PALETTE | NativeMaterialDelta.RADIUS | NativeMaterialDelta.SCALE,
                NativeMaterialDelta.between(null, null, background(48, 0.5f, 7, 32, false), null, null, previous));
        assertEquals(NativeMaterialDelta.GLASS | NativeMaterialDelta.BLUR | NativeMaterialDelta.PALETTE,
                NativeMaterialDelta.between(GlassProfile.capture(new float[42], 16, true),
                        GlassProfile.Blur.capture(1, 2, 16, true), previous, null, null, previous));
    }
    @Test public void recoveredGlassProofPublishesPreviouslyWithheldPaletteEvenWhenValuesMatch() {
        GlassProfile.Background previous = background(48, 0.5f, 7, 16, true);
        GlassProfile glass = GlassProfile.capture(new float[42], 16, true);
        assertEquals(NativeMaterialDelta.GLASS | NativeMaterialDelta.PALETTE,
                NativeMaterialDelta.between(glass, null, previous, null, null, previous));
        assertEquals(0, NativeMaterialDelta.between(glass, null, previous, glass, null, previous));
    }
    @Test public void emptyPaletteHasNoCompletePaletteProofButRetainsContainerChannels() {
        GlassProfile.Background incomplete = GlassProfile.Background.capture(48, 0.5f, new int[0], new int[0], 16, true);
        assertFalse(incomplete.hasPalette());
        assertTrue(background(48, 0.5f, 7, 16, true).hasPalette());
        assertEquals(NativeMaterialDelta.PALETTE, NativeMaterialDelta.between(null, null, incomplete,
                null, null, background(48, 0.5f, 7, 16, true)));
    }
}
