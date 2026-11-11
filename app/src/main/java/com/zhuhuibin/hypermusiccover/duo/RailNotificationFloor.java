package com.zhuhuibin.hypermusiccover.duo;

/** Recover the old full-width footer reservation after controls move into a separate rail. */
final class RailNotificationFloor {
    private RailNotificationFloor() {}
    static int safeBottom(int height, int bottomInset, float density) {
        if (height <= 0 || bottomInset < 0 || !Float.isFinite(density) || density <= 0f) return 0;
        int floor = height - bottomInset - Math.round(12f * density);
        return floor > height / 2 ? floor : 0;
    }
    static int handoff(int current, int safeBottom, boolean geometryChanged) {
        // The previous display can leave a larger absolute floor in the shared state flow.
        if (geometryChanged && safeBottom > 0 && current > 0) return safeBottom;
        return extend(current, safeBottom);
    }

    static int extend(int nativeBottom, int safeBottom) {
        // Initial/unmeasured native values and coordinate systems that do not match the
        // window fail closed. Never pull down a footer the OEM already places lower.
        if (safeBottom <= 0 || nativeBottom < safeBottom / 2 || nativeBottom >= safeBottom)
            return nativeBottom;
        return safeBottom;
    }
}
