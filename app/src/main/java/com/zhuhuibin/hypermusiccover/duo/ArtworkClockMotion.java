package com.zhuhuibin.hypermusiccover.duo;

/** A manual scene change interpolates drawn ink, independently of native font relayout. */
final class ArtworkClockMotion {
    static final float RESPONSE = .46f;

    static final class Pose {
        final float top, height, date;
        Pose(float top, float height, float date) {
            this.top = top; this.height = height; this.date = date;
        }
    }

    static Pose between(Pose from, Pose to, float progress) {
        float p = Float.isFinite(progress) ? Math.max(0f, Math.min(1f, progress)) : 1f;
        return new Pose(from.top + (to.top - from.top) * p,
                from.height + (to.height - from.height) * p,
                Float.isNaN(from.date) ? to.date : from.date + (to.date - from.date) * p);
    }

    static boolean sameNativeLayout(long capturedRevision, long currentRevision,
                                    float capturedNatural, float currentNatural) {
        return capturedRevision == currentRevision && Float.isFinite(capturedNatural)
                && Float.isFinite(currentNatural) && Math.abs(capturedNatural - currentNatural) < 1f;
    }

    private ArtworkClockMotion() {}
}
