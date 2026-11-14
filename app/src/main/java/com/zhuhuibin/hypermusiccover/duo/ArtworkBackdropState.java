package com.zhuhuibin.hypermusiccover.duo;

/** A prepared bitmap is not a presented frame; stale frame callbacks cannot reveal a new scene. */
final class ArtworkBackdropState {
    static final int NATIVE = 0, MESH = 1, STILL = 2;
    private long epoch;
    private int source = -1, drawn = -1;
    private boolean active, revealed;

    /** A compact unavailable lyric page has no foreground to put an artwork background behind. */
    static boolean foregroundReady(boolean explicitPlayer, boolean compact, boolean lyricContentReady) {
        // The legacy explicit player page has its own background choice. A real artwork
        // page (including combined layout) or an available lyric/BPM page can also own it.
        return explicitPlayer || !compact || lyricContentReady;
    }

    static boolean eligible(boolean selected, boolean cover, boolean awake, boolean locked,
                            boolean goingAway, boolean onKeyguard) {
        return selected && cover && awake && locked && !goingAway && onKeyguard;
    }

    /** Full-screen background ownership is independent of the foreground clock's pose. */
    static boolean eligibleAod(boolean cover, boolean awake, boolean fullScreenAod,
                               boolean locked, boolean goingAway, boolean windowPresent) {
        return cover && !awake && fullScreenAod && locked && !goingAway && windowPresent;
    }

    /** A sampler may follow only pixels submitted for the current source, not an old wash. */
    static boolean presentedFrame(int current, int drawn, float alpha) {
        return current >= 0 && drawn == current && Float.isFinite(alpha) && alpha > .001f;
    }

    void select(int generation) {
        if (!active) { active = true; epoch++; }
        if (source != generation) { source = generation; drawn = -1; }
    }

    void drawn(int generation) { if (active && source == generation) drawn = generation; }

    boolean reveal(long callbackEpoch, int generation) {
        if (!active || callbackEpoch != epoch || generation != source || drawn != source) return false;
        revealed = true;
        return true;
    }

    void hide() {
        if (active) epoch++;
        active = revealed = false;
        source = drawn = -1;
    }

    long epoch() { return epoch; }
    int source() { return source; }
    int drawn() { return drawn; }
    boolean revealed() { return revealed; }

    /**
     * The outgoing display list may bridge a renderer change only in the same window,
     * geometry and source generation. A prepared bitmap or a timeout never owns sampling.
     */
    static final class Handoff {
        private Object window;
        private long revision, epoch;
        private int generation = -1, desired, owner;
        private final float[] presented = new float[3];

        void select(long epoch, int desired, Object window, long revision, int generation) {
            boolean valid = desired != NATIVE && window != null && generation >= 0;
            if (!valid || this.window != window || this.revision != revision
                    || this.generation != generation) {
                owner = NATIVE;
                presented[MESH] = presented[STILL] = 0f;
            }
            this.epoch = epoch;
            this.desired = valid ? desired : NATIVE;
            this.window = valid ? window : null;
            this.revision = revision;
            this.generation = valid ? generation : -1;
        }

        boolean presented(long epoch, int renderer, Object window, long revision,
                          int generation, float alpha) {
            if (!matches(epoch, window, revision, generation) || renderer == NATIVE) return false;
            if (!presentedFrame(generation, generation, alpha)) {
                withdraw(renderer);
                return false;
            }
            presented[renderer] = Math.min(1f, alpha);
            // First presentation has no outgoing artwork to keep. During a handoff the
            // replacement must actually cover it before the old display list is retired.
            if (renderer == desired && (owner == NATIVE || alpha >= .995f)) owner = renderer;
            return true;
        }

        void withdraw(int renderer) {
            presented[renderer] = 0f;
            if (owner == renderer) {
                owner = presented[desired] >= .995f ? desired : NATIVE;
            }
        }

        boolean matches(long epoch, Object window, long revision, int generation) {
            return desired != NATIVE && this.epoch == epoch && this.window == window
                    && this.revision == revision && this.generation == generation;
        }

        boolean retains(int renderer) { return owner == renderer && desired != renderer; }
        boolean local() { return owner != NATIVE && presented[owner] > .001f; }
        boolean needsPresentation(int renderer, float alpha) {
            return desired != NATIVE && renderer == desired && owner != renderer
                    && (owner == NATIVE || alpha >= .995f);
        }
        int owner() { return owner; }
        int desired() { return desired; }
        int generation() { return generation; }
        long epoch() { return epoch; }
    }
}
