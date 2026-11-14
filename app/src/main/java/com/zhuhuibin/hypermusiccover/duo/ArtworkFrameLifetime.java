package com.zhuhuibin.hypermusiccover.duo;

/** One prepared frame retires its predecessor only after every needed surface has drawn it. */
final class ArtworkFrameLifetime {
    private boolean recorded, backdropCovered, foregroundCovered;
    private boolean backdropPending, foregroundPending;

    boolean recorded() { return recorded; }
    void used() { recorded = true; }

    /** A detached/replaced presenter cannot acknowledge a frame for its new window. */
    void cancel(boolean backdrop) {
        if (backdrop) backdropPending = false;
        else foregroundPending = false;
    }

    /** A full Canvas draw schedules one next-frame acknowledgement per surface. */
    boolean record(boolean backdrop, float coverage) {
        if (!Float.isFinite(coverage) || coverage <= .001f) return false;
        recorded = true;
        if (coverage < .995f || (backdrop ? backdropCovered || backdropPending
                : foregroundCovered || foregroundPending)) return false;
        if (backdrop) backdropPending = true;
        else foregroundPending = true;
        return true;
    }

    /** Called by the acknowledgement associated with this exact prepared frame. */
    void acknowledge(boolean backdrop) {
        if (backdrop) { backdropPending = false; backdropCovered = true; }
        else { foregroundPending = false; foregroundCovered = true; }
    }

    boolean canRetirePrevious(boolean backdropNeeded, boolean foregroundNeeded) {
        return (!backdropNeeded || backdropCovered) && (!foregroundNeeded || foregroundCovered);
    }
}
