package com.zhuhuibin.hypermusiccover.duo;

/** Complete, immutable native-node geometry. An unavailable capture cannot publish half a pose. */
final class ClockPoseSnapshot {
    final float top, unit, date;
    private final ClockPoseSource source = new ClockPoseSource();
    private final int page, destination;
    private final boolean navigation, anchored;

    private ClockPoseSnapshot(Object window, long revision, Object clock, Object dateView,
                              int page, int destination, boolean navigation, boolean anchored,
                              float top, float unit, float date) {
        source.capture(window, revision, clock, dateView);
        this.page = page; this.destination = destination;
        this.navigation = navigation; this.anchored = anchored;
        this.top = top; this.unit = unit; this.date = date;
    }

    static ClockPoseSnapshot capture(Object window, long revision, Object clock, Object dateView,
                                     int page, int destination, boolean navigation, boolean anchored,
                                     float top, float unit, float date) {
        if (window == null || clock == null || !Float.isFinite(top)
                || !Float.isFinite(unit) || unit <= 0f
                || !(Float.isFinite(date) || !anchored && Float.isNaN(date))
                || anchored && dateView == null) return null;
        return new ClockPoseSnapshot(window, revision, clock, dateView, page, destination,
                navigation, anchored, top, unit, date);
    }

    boolean current(Object window, long revision, Object clock, Object dateView,
                    int page, int destination, boolean navigation, boolean anchored) {
        return this.page == page && this.destination == destination
                && this.navigation == navigation && this.anchored == anchored
                && source.matches(window, revision, clock, dateView);
    }
}
