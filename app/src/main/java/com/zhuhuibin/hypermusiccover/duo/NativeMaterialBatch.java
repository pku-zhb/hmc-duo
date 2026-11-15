package com.zhuhuibin.hypermusiccover.duo;

/** A complete native effect/frame publishes once, after all of its accepted setters return. */
final class NativeMaterialBatch<T> {
    static final class Frame<T> {
        final T source;
        private final Frame<T> previous, owner;
        private boolean dirty, failed;
        private Frame(T source, Frame<T> previous, Frame<T> owner) {
            this.source = source; this.previous = previous; this.owner = owner == null ? this : owner;
        }
    }
    private final ThreadLocal<Frame<T>> current = new ThreadLocal<>();

    Frame<T> begin(T source) {
        Frame<T> previous = current.get(), found = matching(source);
        Frame<T> frame = new Frame<>(source, previous, found == null ? null : found.owner);
        current.set(frame);
        return frame;
    }

    boolean active(T source) { return matching(source) != null; }

    /** The native node has already accepted this write; postpone only its relay publication. */
    boolean defer(T source) {
        Frame<T> frame = matching(source);
        if (frame == null) return false;
        frame.owner.dirty = true;
        return true;
    }

    /** A failed/partial native recipe must never become a complete follower baseline. */
    boolean end(Frame<T> frame, boolean success) {
        if (!success) frame.owner.failed = true;
        if (frame.previous == null) current.remove(); else current.set(frame.previous);
        return frame.owner == frame && frame.dirty && !frame.failed;
    }

    private Frame<T> matching(T source) {
        if (source == null) return null;
        Frame<T> frame = current.get();
        while (frame != null && frame.source != source) frame = frame.previous;
        return frame;
    }
}
