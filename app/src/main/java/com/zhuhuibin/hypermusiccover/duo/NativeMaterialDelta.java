package com.zhuhuibin.hypermusiccover.duo;

/** A complete native frame invalidates only the channels whose committed values changed. */
final class NativeMaterialDelta {
    static final int GLASS = 1, BLUR = 2, PALETTE = 4, RADIUS = 8, SCALE = 16;
    private NativeMaterialDelta() {}
    static int between(GlassProfile glass, GlassProfile.Blur blur, GlassProfile.Background background,
                       GlassProfile previousGlass, GlassProfile.Blur previousBlur, GlassProfile.Background previousBackground) {
        int changes = 0;
        if (glass == null ? previousGlass != null : !glass.same(previousGlass)) changes |= GLASS;
        if (blur == null ? previousBlur != null : !blur.same(previousBlur)) changes |= BLUR;
        if (background == null) {
            if (previousBackground != null) changes |= PALETTE | RADIUS | SCALE;
        } else {
            if (!background.samePalette(previousBackground)) changes |= PALETTE;
            // The previous frame could not project its palette without glass proof.
            // When that proof arrives, equal palette values still need their first replay.
            if (glass != null && previousGlass == null && background.hasPalette()) changes |= PALETTE;
            if (!background.sameRadius(previousBackground)) changes |= RADIUS;
            if (!background.sameScale(previousBackground)) changes |= SCALE;
        }
        return changes;
    }
}
