package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;

import static org.junit.Assert.*;

public class AodClockGlassTest {
    private static float[] glyph(float red, float green, float blue, float fill) {
        float[] glass = new float[42];
        glass[11] = red;
        glass[12] = green;
        glass[13] = blue;
        glass[36] = fill;
        return glass;
    }

    @Test public void paletteColourChangeDoesNotRequestAnotherFillUpdate() {
        float[] glass = glyph(1f, 0.7f, 0.4f, 0.65f);
        assertEquals(AodClockGlass.COLOUR, AodClockGlass.hold(glass, 0.8f, 0.8f, 0.8f, 0.65f, true));
        assertEquals(0, AodClockGlass.hold(glass, 0.8f, 0.8f, 0.8f, 0.65f, true));
    }

    @Test public void capturedFillChangeRequestsTheNativeUpdate() {
        float[] glass = glyph(0.8f, 0.8f, 0.8f, 0f);
        assertEquals(AodClockGlass.FILL, AodClockGlass.hold(glass, 0.8f, 0.8f, 0.8f, 0.65f, true));
        assertEquals(0.65f, glass[36], 0f);
    }

    @Test public void nativeFillUpdateCanBeRepairedWithoutStartingAnotherUpdate() {
        float[] glass = glyph(1f, 0.7f, 0.4f, 0f);
        assertEquals(AodClockGlass.COLOUR | AodClockGlass.FILL,
                AodClockGlass.hold(glass, 0.8f, 0.8f, 0.8f, 0.65f, true));

        // The native fill update also reapplies its interpolated palette to the same field.
        glass[11] = 1f;
        glass[12] = 0.7f;
        glass[13] = 0.4f;
        assertEquals(AodClockGlass.COLOUR, AodClockGlass.hold(glass, 0.8f, 0.8f, 0.8f, 0.65f, true));
        assertEquals(0, AodClockGlass.hold(glass, 0.8f, 0.8f, 0.8f, 0.65f, true));
    }

    @Test public void unchangedGlyphDoesNotAcquireAnotherGlyphsChanges() {
        float[] changed = glyph(1f, 0.7f, 0.4f, 1f);
        float[] unchanged = glyph(0.8f, 0.8f, 0.8f, 1f);
        assertEquals(AodClockGlass.COLOUR, AodClockGlass.hold(changed, 0.8f, 0.8f, 0.8f, 1f, false));
        assertEquals(0, AodClockGlass.hold(unchanged, 0.8f, 0.8f, 0.8f, 1f, false));
    }

    @Test public void plainAodRetainsItsOpaqueFillTolerance() {
        float[] glass = glyph(0.8f, 0.8f, 0.8f, 0.9995f);
        assertEquals(0, AodClockGlass.hold(glass, 0.8f, 0.8f, 0.8f, 1f, false));
        glass[36] = 0.998f;
        assertEquals(AodClockGlass.FILL, AodClockGlass.hold(glass, 0.8f, 0.8f, 0.8f, 1f, false));
        assertEquals(1f, glass[36], 0f);
    }
}
