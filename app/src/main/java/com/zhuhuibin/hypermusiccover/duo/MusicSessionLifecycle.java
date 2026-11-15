package com.zhuhuibin.hypermusiccover.duo;

import java.util.LinkedHashSet;
import java.util.Objects;

/** Destroyed tokens cannot be revived by a retained media card or a queued callback. */
final class MusicSessionLifecycle<T> {
    private final LinkedHashSet<T> destroyed = new LinkedHashSet<>();
    private final int limit;
    private long watchGeneration;
    MusicSessionLifecycle(int limit) { this.limit = Math.max(1, limit); }
    synchronized void destroy(T token) {
        if (token == null) return;
        destroyed.remove(token);
        destroyed.add(token);
        while (destroyed.size() > limit) destroyed.remove(destroyed.iterator().next());
    }
    synchronized boolean alive(T token) { return token != null && !destroyed.contains(token); }
    synchronized boolean accepts(T token, T current) {
        return alive(token) && Objects.equals(token, current);
    }
    synchronized long nextWatchGeneration() { return ++watchGeneration; }
    synchronized long watchGeneration() { return watchGeneration; }
    /** A token can be selected again while its previous callback is still queued. */
    synchronized boolean accepts(T token, T current, long generation) {
        return generation == watchGeneration && accepts(token, current);
    }
}
