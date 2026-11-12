package com.zhuhuibin.hypermusiccover.duo;

/** Fixed status geometry, matching the inspected clock/ring proportions at density 2.75. */
final class RailStatusLayout {
    static final float REFERENCE_DENSITY = 2.75f;
    static final float WIDTH = 205f, HEIGHT = 203f, CENTER_X = 98f;
    // Reference surface starts at y=158 below the observed 126-pixel safe top.
    static final float TOP_DP = 32f / REFERENCE_DENSITY;
    // The complete cluster plus ten dp clearance before the first notification bubble.
    static final float RESERVE_DP = 96f;

    static FoldMusicLayout.Box place(float centerX, float safeTop, float density) {
        if (!FoldMusicLayout.finite(centerX, safeTop, density) || density <= 0 || safeTop < 0) return null;
        float scale = density / REFERENCE_DENSITY;
        // Match the original integer-positioned surface, retaining density and safe-inset scaling.
        float left = Math.round(centerX - CENTER_X * scale), top = Math.round(safeTop + TOP_DP * density);
        return new FoldMusicLayout.Box(left, top, left + WIDTH * scale, top + HEIGHT * scale);
    }
    private RailStatusLayout() {}
}
