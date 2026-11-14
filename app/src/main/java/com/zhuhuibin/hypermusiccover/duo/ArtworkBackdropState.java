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

    /** NaN means only outgoing pixels remain; zero means the whole renderer is invisible. */
    static float compositionEvidence(float incoming, float coverage) {
        if (!Float.isFinite(coverage) || coverage <= .001f) return 0f;
        if (!Float.isFinite(incoming) || incoming <= .001f) return Float.NaN;
        return Math.min(incoming, coverage);
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

    /** Actual outgoing pixels retain their original key while the same source is recomposed. */
    static final class Handoff {
        private Object window;
        private long revision, epoch, sourceIdentity = -1L;
        private int generation = -1, desired, owner;
        private final float[] presented = new float[3];
        private final Object[] drawnWindow = new Object[3];
        private final long[] drawnRevision = new long[3], drawnSource = new long[3], drawnEpoch = new long[3];
        private final int[] drawnGeneration = new int[3];

        /** Legacy callers have no retained-source key, so geometry changes revoke their proof. */
        void select(long epoch, int desired, Object window, long revision, int generation) {
            if (this.revision != revision) { owner = NATIVE; clear(MESH); clear(STILL); }
            select(epoch, desired, window, generation, revision, generation);
        }

        void select(long epoch, int desired, Object window, long sourceIdentity,
                    long revision, int generation) {
            boolean valid = desired != NATIVE && window != null && generation >= 0 && sourceIdentity >= 0L;
            if (!valid || this.window != window || this.sourceIdentity != sourceIdentity) {
                owner = NATIVE;
                clear(MESH);
                clear(STILL);
            }
            this.epoch = epoch;
            this.desired = valid ? desired : NATIVE;
            this.window = valid ? window : null;
            this.revision = revision;
            this.generation = valid ? generation : -1;
            this.sourceIdentity = valid ? sourceIdentity : -1L;
            // Only an already committed display list can bridge composition. A partial
            // candidate from the old geometry cannot become a fallback for the new one.
            for (int renderer = MESH; renderer <= STILL; renderer++)
                if (renderer != owner && !currentFrame(renderer)) clear(renderer);
        }

        boolean presented(long epoch, int renderer, Object window, long revision,
                          int generation, float alpha) {
            return presented(epoch, renderer, window, generation, revision, generation, alpha);
        }

        boolean presented(long epoch, int renderer, Object window, long sourceIdentity,
                          long revision, int generation, float alpha) {
            if (!matches(epoch, window, sourceIdentity, revision, generation) || renderer == NATIVE) return false;
            if (!presentedFrame(generation, generation, alpha)) {
                withdraw(renderer);
                return false;
            }
            if (owner == renderer && !currentFrame(renderer) && alpha < .995f) return false;
            presented[renderer] = Math.min(1f, alpha);
            drawnWindow[renderer] = window;
            drawnSource[renderer] = sourceIdentity;
            drawnRevision[renderer] = revision;
            drawnGeneration[renderer] = generation;
            drawnEpoch[renderer] = epoch;
            // First presentation has no outgoing artwork to keep. During a handoff the
            // replacement must actually cover it before the old display list is retired.
            if (renderer == desired && (owner == NATIVE || alpha >= .995f)) owner = renderer;
            return true;
        }

        void withdraw(int renderer) {
            clear(renderer);
            if (owner == renderer) {
                owner = currentFrame(desired) && presented[desired] >= .995f ? desired : NATIVE;
            }
        }

        boolean matches(long epoch, Object window, long revision, int generation) {
            return matches(epoch, window, generation, revision, generation);
        }

        boolean matches(long epoch, Object window, long sourceIdentity, long revision, int generation) {
            return desired != NATIVE && this.epoch == epoch && this.window == window
                    && this.sourceIdentity == sourceIdentity && this.revision == revision
                    && this.generation == generation;
        }

        /** A zero-alpha actual draw revokes that renderer without relabelling its old key. */
        boolean invisible(long epoch, int renderer, Object window, long sourceIdentity) {
            if (desired == NATIVE || this.epoch != epoch || this.window != window
                    || this.sourceIdentity != sourceIdentity) return false;
            withdraw(renderer);
            return true;
        }

        private boolean currentFrame(int renderer) {
            return renderer != NATIVE && presented[renderer] > .001f && drawnWindow[renderer] == window
                    && drawnSource[renderer] == sourceIdentity && drawnRevision[renderer] == revision
                    && drawnGeneration[renderer] == generation;
        }

        private void clear(int renderer) {
            presented[renderer] = 0f;
            drawnWindow[renderer] = null;
            drawnSource[renderer] = drawnRevision[renderer] = drawnEpoch[renderer] = -1L;
            drawnGeneration[renderer] = -1;
        }

        boolean retains(int renderer) { return owner == renderer && desired != renderer && local(); }
        boolean local() {
            return desired != NATIVE && owner != NATIVE && presented[owner] > .001f
                    && drawnWindow[owner] == window && drawnSource[owner] == sourceIdentity;
        }
        boolean needsPresentation(int renderer, float alpha) {
            return desired != NATIVE && renderer == desired && (owner != renderer || !currentFrame(owner))
                    && (owner == NATIVE || alpha >= .995f);
        }
        int owner() { return owner; }
        int desired() { return desired; }
        int generation() { return local() ? drawnGeneration[owner] : -1; }
        long ownerGeometry() { return local() ? drawnRevision[owner] : -1L; }
        long ownerSource() { return local() ? drawnSource[owner] : -1L; }
        long ownerEpoch() { return local() ? drawnEpoch[owner] : -1L; }
        long epoch() { return epoch; }
    }
}
