package com.zhuhuibin.hypermusiccover.duo;

/** The page owns transition intent; a ready destination owns only an actual frame commit. */
final class ClockReplacementState {
    private boolean target;
    private float from, progress;
    private Object presentedWindow;
    private long presentedRevision = -1L;
    private float presented;

    boolean target() { return target; }
    boolean moving() { return target || progress > 0f; }
    float progress() { return progress; }

    void settle(boolean intended) {
        target = intended;
        from = progress = intended ? 1f : 0f;
    }

    /** Full AOD is a held endpoint, including a direct entry with no preceding lit frame. */
    void hold(boolean intended) { settle(intended); }

    void begin(boolean intended) {
        from = progress;
        target = intended;
    }

    void advance(boolean settled, float frameProgress) {
        progress = settled ? (target ? 1f : 0f)
                : ArtworkClockDestination.progress(from, target, frameProgress);
    }

    /** Missing resources never change the selected target or restart its transition. */
    float candidate(boolean allowed, boolean ready, Object sceneWindow, Object destinationWindow) {
        return allowed && ready && sceneWindow != null && sceneWindow == destinationWindow
                ? ArtworkClockDestination.fade(progress) : 0f;
    }

    /** Publish only after the native face has received the matching inverse alpha. */
    void present(Object window, long revision, float alpha) {
        presentedWindow = window;
        presentedRevision = revision;
        presented = Float.isFinite(alpha) ? Math.max(0f, Math.min(1f, alpha)) : 0f;
    }

    float presented(Object window, long revision) {
        return window != null && presentedWindow == window && presentedRevision == revision
                ? presented : 0f;
    }

    void releasePresentation() {
        presentedWindow = null;
        presentedRevision = -1L;
        presented = 0f;
    }

    void clear() {
        target = false;
        from = progress = 0f;
        releasePresentation();
    }
}
