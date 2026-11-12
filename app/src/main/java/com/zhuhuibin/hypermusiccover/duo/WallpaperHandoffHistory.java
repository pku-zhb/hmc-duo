package com.zhuhuibin.hypermusiccover.duo;

import java.util.ArrayDeque;

/** Retains bounded unlock evidence even after later sleep/wake events overwrite the live tail. */
final class WallpaperHandoffHistory {
    static final long WINDOW_MS = 1600L;
    private final ArrayDeque<String> recent = new ArrayDeque<>();
    private final ArrayDeque<Window> windows = new ArrayDeque<>();
    private Window active;

    private static final class Window {
        final long start;
        final ArrayDeque<String> lines = new ArrayDeque<>();
        Window(long start) { this.start = start; }
    }

    synchronized boolean add(long now, String kind, String details) {
        boolean opened = kind.equals("unlock") && details.startsWith("away=true")
                && (active == null || now > active.start + WINDOW_MS);
        if (opened) {
            active = new Window(now);
            active.lines.addAll(recent);
            windows.addLast(active);
            while (windows.size() > 3) windows.removeFirst();
        }
        String line = now + " " + kind + " " + details;
        recent.addLast(line);
        while (recent.size() > 16) recent.removeFirst();
        if (active != null && now <= active.start + WINDOW_MS) {
            active.lines.addLast(line);
            // Preserve the prelude and first dim values; retain a bounded number of later samples.
            if (active.lines.size() > 100) active.lines.removeLast();
        }
        return opened;
    }

    /** A delayed main-thread summary belongs to its original window, even when posted late. */
    synchronized void finish(long start, long now, String details) {
        for (Window window : windows) if (window.start == start) {
            window.lines.addLast(now + " frames " + details);
            return;
        }
    }

    synchronized String describe() {
        StringBuilder out = new StringBuilder();
        for (Window window : windows) {
            if (out.length() > 0) out.append(" || ");
            out.append("unlock@").append(window.start).append(" [")
                    .append(String.join(" ; ", window.lines)).append(']');
        }
        return out.toString();
    }
}
