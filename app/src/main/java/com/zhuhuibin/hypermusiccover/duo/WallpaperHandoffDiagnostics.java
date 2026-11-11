package com.zhuhuibin.hypermusiccover.duo;

/** Small event history; no captures, user text or continuous frame logging. */
final class WallpaperHandoffDiagnostics {
    private static final java.util.ArrayDeque<String> events = new java.util.ArrayDeque<>();
    private static final java.util.ArrayDeque<String> dimEvents = new java.util.ArrayDeque<>();
    private static int lastDim = Integer.MIN_VALUE;
    private static boolean lastAway, lastScreen;
    static synchronized void note(String kind, String details) {
        events.addLast(android.os.SystemClock.uptimeMillis() + " " + kind + " " + details);
        while (events.size() > 40) events.removeFirst();
    }
    static synchronized void black(float value, boolean away, boolean screen) {
        int bucket = value < 0 ? -1 : Math.round(value * 10);
        if (bucket == lastDim && away == lastAway && screen == lastScreen) return;
        lastDim = bucket; lastAway = away; lastScreen = screen;
        dimEvents.addLast(android.os.SystemClock.uptimeMillis() + " black=" + value + " away=" + away + " screen=" + screen);
        while (dimEvents.size() > 24) dimEvents.removeFirst();
    }
    static synchronized String describe() {
        return String.join(" ; ", events) + " | dim: " + String.join(" ; ", dimEvents);
    }
    private WallpaperHandoffDiagnostics() {}
}
