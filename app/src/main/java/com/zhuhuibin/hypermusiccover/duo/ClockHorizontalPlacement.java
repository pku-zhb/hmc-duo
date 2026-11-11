package com.zhuhuibin.hypermusiccover.duo;

/** Center measured glyph ink after the native clock's width changes without reflowing its font. */
final class ClockHorizontalPlacement {
    private ClockHorizontalPlacement() {}

    /** The large outer-screen clock reads slightly left of its geometric center. */
    static float opticalOffset(float width, float height, float density) {
        if (!FoldMusicLayout.finite(width, height, density) || width <= 0f || height <= 0f
                || density <= 0f) return 0f;
        return width / density < 600f && height >= width ? 8f * density : 0f;
    }

    static float translation(float inkLeft, float inkRight, float pivot, float scale,
                             float viewLeft, float contentCenter) {
        if (!FoldMusicLayout.finite(inkLeft, inkRight, pivot, scale, viewLeft, contentCenter)
                || inkRight <= inkLeft || scale == 0f) return Float.NaN;
        float center = (inkLeft + inkRight) / 2f;
        return contentCenter - (viewLeft + pivot + (center - pivot) * scale);
    }
}
