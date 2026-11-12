package com.zhuhuibin.hypermusiccover.duo;

/** A bounded View setter scope, matched to the RenderNode that actually accepted its write. */
final class NativeMaterialCommitScope<T> {
    static final class Frame<T> {
        final T target;
        final Object node;
        final int role, nightMode;
        final boolean awake;
        private final Frame<T> previous;

        private Frame(T target, Object node, int role, int nightMode, boolean awake, Frame<T> previous) {
            this.target = target;
            this.node = node;
            this.role = role;
            this.nightMode = nightMode;
            this.awake = awake;
            this.previous = previous;
        }
    }

    private final ThreadLocal<Frame<T>> current = new ThreadLocal<>();

    Frame<T> enter(T target, Object node, int role, int nightMode, boolean awake) {
        Frame<T> frame = new Frame<>(target, node, role, nightMode, awake, current.get());
        current.set(frame);
        return frame;
    }

    Frame<T> matching(Object node) {
        Frame<T> frame = current.get();
        return frame != null && node != null && frame.node == node ? frame : null;
    }

    Frame<T> committed(Object node, boolean changed) { return changed ? matching(node) : null; }

    void leave(Frame<T> frame) {
        if (frame.previous == null) current.remove(); else current.set(frame.previous);
    }
}
