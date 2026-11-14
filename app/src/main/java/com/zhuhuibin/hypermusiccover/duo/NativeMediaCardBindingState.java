package com.zhuhuibin.hypermusiccover.duo;

/** A native card binding is one immutable window/layout generation, not a metadata cache. */
final class NativeMediaCardBindingState<T> {
    static final class Snapshot<T> {
        final Object window;
        final long revision;
        final T card, art, title, artist;
        final Object observer;
        final long generation;

        Snapshot(Object window, long revision, T card, T art, T title, T artist,
                 Object observer, long generation) {
            this.window = window;
            this.revision = revision;
            this.card = card;
            this.art = art;
            this.title = title;
            this.artist = artist;
            this.observer = observer;
            this.generation = generation;
        }
    }

    private Snapshot<T> binding;
    private long generation;

    Snapshot<T> snapshot() { return binding; }
    long generation() { return generation; }

    /** Every reference is compared by identity, including reinflated children and observers. */
    boolean matches(Object window, long revision, T card, T art, T title, T artist,
                    Object observer) {
        Snapshot<T> b = binding;
        return b != null && b.window == window && b.revision == revision && b.card == card
                && b.art == art && b.title == title && b.artist == artist
                && b.observer == observer;
    }

    /** Publish all identities together. The caller validates native header/window membership. */
    boolean bind(Object window, long revision, T card, T art, T title, T artist,
                 Object observer) {
        if (matches(window, revision, card, art, title, artist, observer)) return false;
        binding = new Snapshot<>(window, revision, card, art, title, artist, observer,
                ++generation);
        return true;
    }

    void clear() {
        if (binding == null) return;
        binding = null;
        generation++;
    }

    /** A late detach from the previous card must not release its replacement's binding. */
    boolean clearIfCard(T card) {
        if (binding == null || binding.card != card) return false;
        clear();
        return true;
    }

    /** Prefer a visible native replacement; keep a hidden attached current card otherwise. */
    static boolean shouldReplace(boolean currentAttached, boolean sameWindow,
                                 boolean candidateAttached, boolean candidateVisible,
                                 boolean sameNativeHeader) {
        return sameWindow && sameNativeHeader && candidateAttached
                && (!currentAttached || candidateVisible);
    }
}
