package com.zhuhuibin.hypermusiccover.duo;

/** A prepared bitmap is not a presented frame; stale frame callbacks cannot reveal a new scene. */
final class ArtworkBackdropState {
    private long epoch;
    private int source = -1, drawn = -1;
    private boolean active, revealed;

    static boolean eligible(boolean selected, boolean cover, boolean awake, boolean locked,
                            boolean goingAway, boolean onKeyguard) {
        return selected && cover && awake && locked && !goingAway && onKeyguard;
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
}
