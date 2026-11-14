package com.zhuhuibin.hypermusiccover.duo;

/** Page intent survives a temporarily missing measurement; a lease never crosses window domains. */
final class ClockSceneState {
    long epoch = -1L, revision = -1L;
    int page = -1;
    Object window;
    boolean requested, ready, cached;
    long passes, commits, retainedMisses, glyphChanges;
    int passCommits;
    private Object committedWindow;
    private long committedRevision = -1L, committedEpoch = -1L;

    void update(long epoch, Object window, long revision, int page, boolean requested) {
        if (this.window != window || this.revision != revision) {
            ready = cached = false;
        }
        this.epoch = epoch;
        this.window = window;
        this.revision = revision;
        this.page = page;
        this.requested = requested;
    }

    /** A held native pose is not proof that a coordinate-based destination has committed. */
    boolean needsAodDestinationCommit(boolean aod, boolean waking, boolean held,
                                      boolean rowRequested, boolean rowReady, boolean rowPresented) {
        if (!requested || window == null || !aod || waking) return false;
        return rowRequested ? rowReady && (!held || !rowPresented || !retainLease()) : !held;
    }

    void beginPass() { passes++; passCommits = 0; }

    void measured(boolean ready, boolean cached) {
        this.ready = ready;
        this.cached = cached;
        if (!ready && retainLease()) retainedMisses++;
    }

    boolean retainLease() {
        return requested && window != null && committedWindow == window
                && committedRevision == revision;
    }

    void committed() {
        if (passCommits++ == 0) commits++;
        committedWindow = window;
        committedRevision = revision;
        committedEpoch = epoch;
    }

    boolean currentCommit() { return retainLease() && committedEpoch == epoch; }

    void released() {
        committedWindow = null;
        committedRevision = committedEpoch = -1L;
    }

    String describe() {
        return "epoch=" + epoch + " page=" + page + " requested=" + requested
                + " ready=" + ready + " cached=" + cached + " committed=" + currentCommit()
                + " retained=" + retainLease() + " revision=" + revision
                + " passes=" + passes + " commits=" + commits + " passCommits=" + passCommits
                + " missesHeld=" + retainedMisses + " glyphChanges=" + glyphChanges;
    }
}
