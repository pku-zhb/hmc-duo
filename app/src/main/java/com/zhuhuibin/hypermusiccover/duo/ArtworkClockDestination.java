package com.zhuhuibin.hypermusiccover.duo;

/** The artwork page uses the sidebar time; retain old constants for inactive implementations. */
public final class ArtworkClockDestination {
    public static final int SHRINK = 0, DATE_ROW = 1, STATUS = 2;
    private ArtworkClockDestination() {}

    public static int normalize(int mode) {
        return STATUS;
    }

    public static int fromLegacy(boolean row) { return STATUS; }

    static boolean sceneRequested(boolean cover, boolean player, boolean notifications, boolean navigation) {
        return navigation || cover && !player && !notifications;
    }

    static float progress(float from, boolean target, float progress) {
        float p = Float.isFinite(progress) ? Math.max(0f, Math.min(1f, progress)) : 1f;
        return from + ((target ? 1f : 0f) - from) * p;
    }

    static float fade(float progress) {
        float p = Float.isFinite(progress) ? Math.max(0f, Math.min(1f, progress)) : 0f;
        return p * p * (3f - 2f * p);
    }
}
