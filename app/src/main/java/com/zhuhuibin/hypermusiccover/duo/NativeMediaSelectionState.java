package com.zhuhuibin.hypermusiccover.duo;

import java.util.Objects;

/** The committed native card publishes its token and track identity as one value. */
final class NativeMediaSelectionState<T> {
    static final class Snapshot<T> {
        final Object card, owner, data;
        final T token;
        final String key;
        final long generation;
        Snapshot(Object card, Object owner, Object data, T token, String key, long generation) {
            this.card = card;
            this.owner = owner;
            this.data = data;
            this.token = token;
            this.key = key;
            this.generation = generation;
        }
    }

    private volatile Snapshot<T> selected;
    Snapshot<T> snapshot() { return selected; }
    boolean allowsSessionFallback() { return selected == null; }
    T token() { Snapshot<T> value = selected; return value == null ? null : value.token; }
    String key() { Snapshot<T> value = selected; return value == null ? "" : value.key; }

    /** Session-list ordering and playback state never replace a committed native selection. */
    synchronized boolean publish(Object card, Object owner, Object data, T token, String key) {
        Snapshot<T> old = selected;
        String nextKey = key == null ? "" : key;
        if (old != null && old.card == card && old.owner == owner && old.data == data
                && Objects.equals(old.token, token) && old.key.equals(nextKey)) return false;
        selected = new Snapshot<>(card, owner, data, token, nextKey,
                old == null ? 1 : old.generation + 1);
        return true;
    }

    void clear() { publish(null, null, null, null, ""); }
}
