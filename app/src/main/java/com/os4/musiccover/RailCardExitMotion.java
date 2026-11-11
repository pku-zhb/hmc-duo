package com.os4.musiccover;

/** A native media card leaves below the screen; it never moves toward the rail capsule. */
final class RailCardExitMotion {
    static float distance(float screenBottom, float cardTop, float gap) {
        return Math.max(gap, screenBottom - cardTop + gap);
    }
    static float progress(float fraction) {
        float t = Math.max(0f, Math.min(1f, fraction));
        return t * t * (3f - 2f * t);
    }
    static float alpha(float fraction) {
        return 1f - Math.max(0f, Math.min(1f, (fraction - .45f) / .55f));
    }
    private RailCardExitMotion() {}
}
