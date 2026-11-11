package com.zhuhuibin.hypermusiccover.duo;

/** Owned glass and dim layers stop drawing at unlock start, including direct AOD unlocks. */
final class LockscreenSurfacePolicy {
    static boolean visible(boolean locked, boolean goingAway) { return locked && !goingAway; }
    static boolean holdButtons(boolean locked, boolean goingAway, boolean bouncer, boolean customAod) {
        return visible(locked, goingAway) && !bouncer && !customAod;
    }
    private LockscreenSurfacePolicy() {}
}
