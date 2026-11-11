package com.zhuhuibin.hypermusiccover.duo;

/** Center measured glyph ink after the native clock's width changes without reflowing its font. */
final class ClockHorizontalPlacement {
    private ClockHorizontalPlacement() {}
    static float translation(float inkLeft, float inkRight, float pivot, float scale,
                             float viewLeft, float contentCenter) {
        if (!FoldMusicLayout.finite(inkLeft, inkRight, pivot, scale, viewLeft, contentCenter)
                || inkRight <= inkLeft || scale == 0f) return Float.NaN;
        float center = (inkLeft + inkRight) / 2f;
        return contentCenter - (viewLeft + pivot + (center - pivot) * scale);
    }
}
