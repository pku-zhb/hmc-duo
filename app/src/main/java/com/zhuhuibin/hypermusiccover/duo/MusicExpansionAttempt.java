package com.zhuhuibin.hypermusiccover.duo;

import java.util.Objects;

/** A user request may wait briefly for the real native header, but never invent its replacement. */
final class MusicExpansionAttempt<T> {
    enum Step { OPEN, WAIT, CANCEL, EXPIRE }
    static final long MAX_WAIT_MS = 1000L;
    final T token;
    final long generation;
    private final long started;

    MusicExpansionAttempt(T token, long generation, long started) {
        this.token = token; this.generation = generation; this.started = started;
    }

    Step evaluate(T currentToken, long currentGeneration, boolean eligible,
                  boolean live, boolean cardPresent, boolean attachedHeader, long now) {
        if (!eligible || !live || !cardPresent || token == null
                || !Objects.equals(token, currentToken) || generation != currentGeneration) return Step.CANCEL;
        if (attachedHeader) return Step.OPEN;
        return now - started >= MAX_WAIT_MS ? Step.EXPIRE : Step.WAIT;
    }

    long retryAfter(long now) {
        long elapsed = Math.max(0L, now - started);
        long interval = elapsed < 80L ? 80L : elapsed < 240L ? 160L : 320L;
        return Math.max(1L, Math.min(interval, MAX_WAIT_MS - elapsed));
    }

    static boolean nativeOnly(boolean resumption, boolean hasToken, boolean tokenAlive) {
        return resumption || hasToken && !tokenAlive;
    }
}
