package com.zhuhuibin.hypermusiccover.duo;

/** Small event history; no captures, user text or continuous frame logging. */
final class WallpaperHandoffDiagnostics {
    private static final java.util.ArrayDeque<String> events = new java.util.ArrayDeque<>();
    private static final java.util.ArrayDeque<String> dimEvents = new java.util.ArrayDeque<>();
    private static final WallpaperHandoffHistory unlocks = new WallpaperHandoffHistory();
    private static int lastDim = Integer.MIN_VALUE;
    private static float lastBlack = Float.NaN;
    private static boolean lastAway, lastScreen;
    static synchronized void note(String kind, String details) {
        long now = android.os.SystemClock.uptimeMillis();
        events.addLast(now + " " + kind + " " + details);
        while (events.size() > 40) events.removeFirst();
        if (unlocks.add(now, kind, details)) {
            unlocks.add(now, "black-at-exit", "value=" + lastBlack + " observedAway=" + lastAway
                    + " observedScreen=" + lastScreen);
            Main.main().post(() -> watchUnlock(now));
        }
    }
    static synchronized void black(float value, boolean away, boolean screen) {
        lastBlack = value;
        int bucket = value < 0 ? -1 : Math.round(value * 10);
        if (bucket == lastDim && away == lastAway && screen == lastScreen) return;
        lastDim = bucket; lastAway = away; lastScreen = screen;
        long now = android.os.SystemClock.uptimeMillis();
        String details = "value=" + value + " away=" + away + " screen=" + screen;
        dimEvents.addLast(now + " black=" + value + " away=" + away + " screen=" + screen);
        while (dimEvents.size() > 24) dimEvents.removeFirst();
        unlocks.add(now, "black", details);
    }
    static synchronized String describe() {
        return String.join(" ; ", events) + " | dim: " + String.join(" ; ", dimEvents)
                + " | unlockWindows: " + unlocks.describe();
    }

    /** Read-only and temporary: no captures, invalidations, rendering veto or frame pumping. */
    private static void watchUnlock(long start) {
        try {
            watchUnlockSafely(start);
        } catch (Throwable failure) {
            unlocks.finish(start, android.os.SystemClock.uptimeMillis(),
                    "unavailable=" + failure.getClass().getSimpleName());
        }
    }

    private static void watchUnlockSafely(long start) {
        android.view.View container = Main.sContainer;
        android.view.View root = container == null ? null : container.getRootView();
        if (root == null) {
            unlocks.finish(start, android.os.SystemClock.uptimeMillis(), "root=none");
            return;
        }
        android.view.ViewTreeObserver observer = root.getViewTreeObserver();
        final long[] frames = new long[6]; // pre-draws, draws, first draw, last draw, max gap, start
        frames[5] = start;
        android.view.ViewTreeObserver.OnPreDrawListener before = () -> {
            frames[0]++;
            return true;
        };
        android.view.ViewTreeObserver.OnDrawListener drawn = () -> {
            long now = android.os.SystemClock.uptimeMillis();
            frames[1]++;
            if (frames[2] == 0) frames[2] = now;
            if (frames[3] != 0) frames[4] = Math.max(frames[4], now - frames[3]);
            frames[3] = now;
        };
        try {
            observer.addOnPreDrawListener(before);
            observer.addOnDrawListener(drawn);
            // Arrange cleanup before reading optional state; failed samples must not leak listeners.
            Main.main().postAtTime(() -> {
                release(observer, before, drawn);
                long now = android.os.SystemClock.uptimeMillis();
                unlocks.finish(start, now, "elapsed=" + (now - start) + " pre=" + frames[0]
                        + " draw=" + frames[1] + " first=" + (frames[2] == 0 ? -1 : frames[2] - start)
                        + " last=" + (frames[3] == 0 ? -1 : frames[3] - start) + " maxGap=" + frames[4]);
            }, start + WallpaperHandoffHistory.WINDOW_MS);
            sample(root, frames);
            for (long offset : new long[]{120L, 400L, 800L, 1500L}) {
                Main.main().postAtTime(() -> sample(root, frames), start + offset);
            }
        } catch (Throwable failure) {
            release(observer, before, drawn);
            unlocks.finish(start, android.os.SystemClock.uptimeMillis(),
                    "unavailable=" + failure.getClass().getSimpleName());
        }
    }

    private static void release(android.view.ViewTreeObserver observer,
            android.view.ViewTreeObserver.OnPreDrawListener before,
            android.view.ViewTreeObserver.OnDrawListener drawn) {
        try { if (observer.isAlive()) observer.removeOnPreDrawListener(before); }
        catch (Throwable ignored) { }
        try { if (observer.isAlive()) observer.removeOnDrawListener(drawn); }
        catch (Throwable ignored) { }
    }

    private static void sample(android.view.View root, long[] frames) {
        long now = android.os.SystemClock.uptimeMillis();
        if (now > frames[5] + WallpaperHandoffHistory.WINDOW_MS) return;
        try {
            unlocks.add(now, "surface", "dt=" + (now - frames[5])
                    + " pre=" + frames[0] + " draw=" + frames[1]
                    + " rootVis=" + root.getVisibility() + " alpha=" + root.getAlpha()
                    + " attached=" + root.isAttachedToWindow() + " layout=" + root.isLayoutRequested()
                    + " screen=" + Main.screenOnCached() + " aod=" + MiniPlayerScene.INSTANCE.getAodActive()
                    + " away=" + MiniPlayerScene.INSTANCE.getKeyguardGoingAway()
                    + " cover=" + Main.coverModeOn() + " backdrop={" + CoverBackdrop.describe() + "}");
        } catch (Throwable failure) {
            unlocks.add(now, "surface", "dt=" + (now - frames[5])
                    + " unavailable=" + failure.getClass().getSimpleName());
        }
    }
    private WallpaperHandoffDiagnostics() {}
}
