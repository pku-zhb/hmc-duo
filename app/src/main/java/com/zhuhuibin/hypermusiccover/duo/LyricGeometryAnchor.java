package com.zhuhuibin.hypermusiccover.duo;

/** Move the band's geometry without restarting the lyric focus/centering animation. */
final class LyricGeometryAnchor {
    private LyricGeometryAnchor() {}
    static float rebase(float value, float previousTarget, float nextTarget) {
        return Float.isFinite(value) && Float.isFinite(previousTarget) && Float.isFinite(nextTarget)
                ? value + nextTarget - previousTarget : value;
    }
}
