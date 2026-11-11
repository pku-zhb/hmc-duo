package com.zhuhuibin.hypermusiccover.duo;

/** Keep a small fraction of the OEM's horizontal AOD inset, shared by the whole column. */
final class RailAodInset {
    private RailAodInset() {}
    static float target(float rest, float nativeDrawn) { return rest + (nativeDrawn - rest) * .3f; }
    static float translation(float base, float drawn, float rest, float parentScale) {
        if (!FoldMusicLayout.finite(base, drawn, rest, parentScale) || parentScale <= .01f) return base;
        return base + (target(rest, drawn) - drawn) / parentScale;
    }
}
