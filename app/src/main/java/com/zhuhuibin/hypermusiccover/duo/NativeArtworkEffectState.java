package com.zhuhuibin.hypermusiccover.duo;

/** An unknown native effect is never ours to replace; null is known only after observation. */
final class NativeArtworkEffectState<T> {
    private boolean known, held;
    private T current, baseline, written;

    /** Record only the actual setter argument, including an explicit native null. */
    void observe(T actual) {
        known = true;
        current = actual;
    }

    boolean known() { return known; }
    boolean owns() { return known && held && current == written; }
    T current() { return current; }

    /** Reserve a change; the successful native setter subsequently publishes observe(next). */
    boolean write(T next) {
        if (!known || current == next) return false;
        if (!owns()) baseline = current;
        written = next;
        held = true;
        return true;
    }

    /** Restore only our exact last output, then forget the lease even if another owner won. */
    T release() {
        T restored = owns() ? baseline : current;
        held = false;
        baseline = written = null;
        return restored;
    }
}
