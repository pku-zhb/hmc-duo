package com.zhuhuibin.hypermusiccover.duo;

/** The artwork page's clock destination; legacy row settings retain their meaning. */
public final class ArtworkClockDestination {
    public static final int SHRINK = 0, DATE_ROW = 1, STATUS = 2;
    private ArtworkClockDestination() {}

    public static int normalize(int mode) {
        return mode >= SHRINK && mode <= STATUS ? mode : DATE_ROW;
    }

    public static int fromLegacy(boolean row) { return row ? DATE_ROW : SHRINK; }

    static float progress(float from, boolean target, float progress) {
        float p = Float.isFinite(progress) ? Math.max(0f, Math.min(1f, progress)) : 1f;
        return from + ((target ? 1f : 0f) - from) * p;
    }

    static float fade(float progress) {
        float p = Float.isFinite(progress) ? Math.max(0f, Math.min(1f, progress)) : 0f;
        return p * p * (3f - 2f * p);
    }
}
