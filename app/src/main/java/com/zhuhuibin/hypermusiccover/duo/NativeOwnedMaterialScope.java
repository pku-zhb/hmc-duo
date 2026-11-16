package com.zhuhuibin.hypermusiccover.duo;

import java.util.function.Supplier;

/** One synchronous recipe observes one pair/scene; the next recipe always resolves again. */
final class NativeOwnedMaterialScope<T> {
    static final class Snapshot<T> {
        final T owner, container;
        final int state;
        Snapshot(T owner, T container, int state) {
            this.owner = owner; this.container = container; this.state = state;
        }
        boolean contains(T target) { return target != null && (target == owner || target == container); }
    }
    private static final class Frame<T> {
        final T target;
        final Snapshot<T> snapshot;
        final Frame<T> previous;
        Frame(T target, Snapshot<T> snapshot, Frame<T> previous) {
            this.target = target; this.snapshot = snapshot; this.previous = previous;
        }
    }
    private final ThreadLocal<Frame<T>> current = new ThreadLocal<>();

    void begin(T target, Supplier<Snapshot<T>> resolve) {
        Snapshot<T> found = snapshot(target);
        current.set(new Frame<>(target, found != null ? found : resolve.get(), current.get()));
    }
    void end(T target) {
        Frame<T> frame = current.get();
        if (frame == null || frame.target != target) throw new IllegalStateException("unbalanced owned recipe");
        if (frame.previous == null) current.remove(); else current.set(frame.previous);
    }
    Snapshot<T> snapshot(T target) {
        for (Frame<T> frame = current.get(); frame != null; frame = frame.previous)
            if (frame.snapshot != null && frame.snapshot.contains(target)) return frame.snapshot;
        return null;
    }
}
