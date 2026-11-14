package com.zhuhuibin.hypermusiccover.duo;

/** Date placement follows the current display, rather than the outer camera inset. */
final class FoldDateLayout {
    private FoldDateLayout() {}

    static boolean inner(float width, float height, float density) {
        return FoldMusicLayout.finite(width, height, density) && density > 0f
                && Math.min(width, height) / density >= 600f;
    }

    static float line(float width, float height, float density, float outerSafeTop) {
        if (!FoldMusicLayout.finite(width, height, density, outerSafeTop) || density <= 0f
                || width <= 0f || height <= 0f)
            return Float.NaN;
        // The inner camera is outside the centered date's horizontal lane in both rotations.
        if (inner(width, height, density)) return 24f * density;
        return Math.max(64f * density, Math.max(0f, outerSafeTop) + 8f * density);
    }
}
