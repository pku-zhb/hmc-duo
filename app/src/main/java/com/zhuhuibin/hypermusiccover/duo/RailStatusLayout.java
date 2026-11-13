package com.zhuhuibin.hypermusiccover.duo;

/** Fixed status geometry, matching the inspected clock/ring proportions at density 2.75. */
final class RailStatusLayout {
    static final float REFERENCE_DENSITY = 2.75f;
    static final float WIDTH = 205f, HEIGHT = 203f, CENTER_X = 98f;
    // Reference surface starts at y=158 below the observed 126-pixel safe top.
    static final float TOP_DP = 32f / REFERENCE_DENSITY;
    // The complete cluster plus ten dp clearance before the first notification bubble.
    static final float RESERVE_DP = 96f;
    static final float OFFSET_Y_MIN_DP = -80f, OFFSET_Y_MAX_DP = 80f;

    static float normalizeOffsetY(float value) {
        return Float.isNaN(value) || Float.isInfinite(value) ? 0f
                : Math.max(OFFSET_Y_MIN_DP, Math.min(OFFSET_Y_MAX_DP, value));
    }

    static FoldMusicLayout.Box place(float centerX, float safeTop, float density) {
        return place(centerX, safeTop, density, 0f);
    }

    /** Apply the user's dp offset once in the same canonical coordinates for lock screen and AOD. */
    static FoldMusicLayout.Box place(float centerX, float safeTop, float density, float offsetYDp) {
        if (!FoldMusicLayout.finite(centerX, safeTop, density) || density <= 0 || safeTop < 0) return null;
        float scale = density / REFERENCE_DENSITY;
        // Match the original integer-positioned surface, retaining density and safe-inset scaling.
        float left = Math.round(centerX - CENTER_X * scale);
        float top = Math.round(safeTop + (TOP_DP + normalizeOffsetY(offsetYDp)) * density);
        return new FoldMusicLayout.Box(left, top, left + WIDTH * scale, top + HEIGHT * scale);
    }

    static final class Offset {
        final float x, y;
        Offset(float x, float y) { this.x = x; this.y = y; }
    }

    /** Invert only a horizontal window-space correction, retaining native Y and scale. */
    static Offset pinX(float canonicalX, float drawnX, float a, float b, float c, float d) {
        if (!FoldMusicLayout.finite(canonicalX, drawnX, a, b, c, d)) return null;
        double determinant = (double) a * d - (double) b * c;
        if (Math.abs(determinant) < 0.000001) return null;
        double dx = (double) canonicalX - drawnX;
        float x = (float) (d * dx / determinant);
        float y = (float) (-c * dx / determinant);
        return FoldMusicLayout.finite(x, y) ? new Offset(x, y) : null;
    }
    private RailStatusLayout() {}
}
