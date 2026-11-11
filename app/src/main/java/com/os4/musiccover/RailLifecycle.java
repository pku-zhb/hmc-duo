package com.os4.musiccover;

/** AOD and its wake retain the last valid rail until unlock, disable or geometry replacement. */
final class RailLifecycle {
    private RailLifecycle() {}
    static boolean eligible(boolean enabled, boolean locked, boolean bouncer) {
        // Visibility can lag screen-on while the AOD clock is being replaced. The actual
        // keyguard lock, rather than either tree's visibility, owns the remembered geometry.
        return enabled && locked && !bouncer;
    }
    static boolean retain(boolean eligible, boolean hasPlan, long planRevision, long revision) {
        return eligible && hasPlan && planRevision == revision;
    }
    static boolean freeze(boolean reusable, boolean screenOn, boolean aod, float rootScaleX) {
        return reusable && (!screenOn || aod || Float.isFinite(rootScaleX)
                && Math.abs(rootScaleX - 1f) > .001f);
    }
}
