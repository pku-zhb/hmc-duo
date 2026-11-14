package com.zhuhuibin.hypermusiccover.duo;

/** A handoff retains actual opaque coverage until the incoming renderer has presented it. */
final class AodBackdropAlpha {
    private AodBackdropAlpha() {}
    static float value(boolean retained, boolean nativeTransition, boolean aodRenderer,
                       float fraction, float fallback) {
        if (retained) return 1f;
        if (!nativeTransition || !Float.isFinite(fraction)) return fallback;
        float p = Math.max(0f, Math.min(1f, fraction));
        return aodRenderer ? p : 1f - p;
    }
}
