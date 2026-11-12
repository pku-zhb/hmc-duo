package com.zhuhuibin.hypermusiccover.duo;

/** Owned glass and dim layers stop drawing at unlock start, including direct AOD unlocks. */
final class LockscreenSurfacePolicy {
    static boolean visible(boolean locked, boolean goingAway) { return locked && !goingAway; }
    static boolean holdButtons(boolean locked, boolean goingAway, boolean bouncer, boolean customAod) {
        return visible(locked, goingAway) && !bouncer && !customAod;
    }
    /** Standalone rail shortcuts do not require a music or notification capsule to exist. */
    static boolean startButtonHold(boolean rail, boolean discsWanted, boolean rowWanted,
                                   boolean fullScreenAod, boolean enteringFullScreenAod) {
        return (rail ? discsWanted : rowWanted)
                && (fullScreenAod || rail && enteringFullScreenAod);
    }
    /** A bounded sleep prelude, replaced by authoritative doze state as soon as it arrives. */
    static final class AodEntry {
        private long until;
        void prepare(boolean supported, long now) { until = supported ? now + 2000L : 0L; }
        boolean active(long now) { return now < until; }
        void observe(boolean dozing, boolean goingAway, boolean bouncer) {
            if (dozing || goingAway || bouncer) cancel();
        }
        void cancel() { until = 0L; }
    }
    private LockscreenSurfacePolicy() {}
}
