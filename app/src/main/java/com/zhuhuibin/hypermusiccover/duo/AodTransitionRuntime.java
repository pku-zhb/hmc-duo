package com.zhuhuibin.hypermusiccover.duo;

import android.view.View;

/** Native transition publication; presentation is consumed at the clock's pre-draw boundary. */
final class AodTransitionRuntime {
    private static final AodTransitionProgress progress = new AodTransitionProgress();
    private static long begins, frames, ends, cancels, scenes;
    private static String lastEvent = "none";
    private static View window() { return Main.sContainer == null ? null : Main.sContainer.getRootView(); }
    private static long revision() { return Main.screenGeometry().revision; }
    static void begin(boolean into) {
        note("begin"); progress.begin(window(), revision(), into); invalidate();
    }
    static void frame(boolean into, float fraction) {
        note("frame");
        progress.frame(window(), revision(), into, fraction); invalidate();
    }
    static void complete(boolean into) {
        note("complete"); progress.complete(window(), revision(), into); invalidate();
    }
    static void cancel() { note("cancel"); progress.cancel(window(), revision()); invalidate(); }
    static void scene(boolean fullAod) {
        note("scene");
        progress.scene(window(), revision(), fullAod, MiniPlayerRuntime.aodTransitionPlanned());
        invalidate();
    }
    static AodTransitionProgress.Snapshot snapshot() {
        return progress.snapshot(window(), revision(), MiniPlayerScene.INSTANCE.getFullScreenAodActive());
    }
    static boolean moving() { return snapshot().running; }
    private static void invalidate() {
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            Main.main().post(AodTransitionRuntime::invalidate);
            return;
        }
        View v = Main.sContainer;
        if (v != null) v.postInvalidateOnAnimation();
        ClockCollapse.nativeAodProgressChanged();
        CoverCardLayer.aodProgressChanged();
        VideoBackdropMesh.aodProgressChanged();
    }
    private static synchronized void note(String event) {
        lastEvent = event;
        switch (event) {
            case "begin": begins++; break;
            case "frame": frames++; break;
            case "complete": ends++; break;
            case "cancel": cancels++; break;
            default: scenes++;
        }
    }
    static synchronized String describe() {
        AodTransitionProgress.Snapshot s = snapshot();
        return "epoch=" + s.epoch + " fraction=" + s.fraction + " running=" + s.running
                + " into=" + s.into + " event=" + lastEvent
                + " events=" + begins + "/" + frames + "/" + ends + "/" + cancels + "/" + scenes;
    }
}
