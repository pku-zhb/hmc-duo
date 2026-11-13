package com.zhuhuibin.hypermusiccover.duo;

/** A native effect remains the baseline while a temporary card-only mask is composed above it. */
final class RailCardEffectLease<E> {
    E base, written;
    boolean masked;

    RailCardEffectLease(E baseline) { base = written = baseline; }

    E nativeInput(E value) {
        // A reader may be handing back the current output. Do not recursively adopt our mask.
        return owns(value) ? base : value;
    }

    /** Call only after the verified RenderNode bridge has returned from its native setter. */
    void committed(E nativeBase, E actual, boolean hasMask) {
        base = nativeBase;
        written = actual;
        masked = hasMask;
    }

    boolean owns(E actual) { return masked && actual == written; }
    E nativeRead(E actual) { return owns(actual) ? base : actual; }
}
