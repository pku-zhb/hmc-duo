package com.zhuhuibin.hypermusiccover.duo;

/** An explicit page entry can recover an empty presentation without polling every metadata tick. */
final class LyricPageRecovery {
    static final int NONE = 0, RESTORE = 1, RETRY = 2;
    static final long RETRY_INTERVAL_MS = 15000L;
    static final long PIPELINE_TIMEOUT_MS = 25000L;
    private String attemptedTrack = "";
    private long attemptedAt;
    private boolean sourcePending;

    void lookupStarted(String track, long now) {
        attemptedTrack = track;
        attemptedAt = now;
        sourcePending = true;
    }

    void lookupFinished() { sourcePending = false; }

    int onPageSelected(String track, long now, boolean loading, boolean displayed, boolean cached) {
        if (track == null || track.isEmpty() || displayed) return NONE;
        // The same track's parsed lines remain usable even while optional enrichment runs.
        if (cached) return RESTORE;
        // An early empty answer can precede the source pipeline's final fallback. Its
        // presentation is unavailable already; that must not start a parallel request.
        boolean pending = sourcePending && track.equals(attemptedTrack)
                && now >= attemptedAt && now - attemptedAt < PIPELINE_TIMEOUT_MS;
        if (loading || pending) return NONE;
        return !track.equals(attemptedTrack) || now < attemptedAt
                || now - attemptedAt >= RETRY_INTERVAL_MS ? RETRY : NONE;
    }
}
