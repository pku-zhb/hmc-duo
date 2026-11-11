package com.os4.musiccover;

/** Small event history; no captures, user text or continuous frame logging. */
final class WallpaperHandoffDiagnostics {
    private static final java.util.ArrayDeque<String> events = new java.util.ArrayDeque<>();
    static synchronized void note(String kind, String details) {
        events.addLast(android.os.SystemClock.uptimeMillis() + " " + kind + " " + details);
        while (events.size() > 20) events.removeFirst();
    }
    static synchronized String describe() { return String.join(" ; ", events); }
    private WallpaperHandoffDiagnostics() {}
}
