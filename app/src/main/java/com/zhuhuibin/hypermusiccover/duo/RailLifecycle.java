package com.zhuhuibin.hypermusiccover.duo;

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

    /** Keep the whole column pinned through wake; unrelated awake animations do not arm it. */
    static boolean holdColumnX(boolean previous, boolean active, boolean leaving,
                               boolean screenOn, boolean aod, boolean frozen) {
        if (!active || leaving) return false;
        if (aod || !screenOn) return true;
        return previous && frozen;
    }
}
