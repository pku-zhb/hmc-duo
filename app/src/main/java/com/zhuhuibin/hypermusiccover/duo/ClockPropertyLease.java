package com.zhuhuibin.hypermusiccover.duo;

/**
 * Own one finite View transform only while its value is still our last output.
 * A native or rail rewrite becomes the next baseline; releasing never rolls it back.
 */
final class ClockPropertyLease {
    private boolean owned;
    private float baseline;
    private float written;

    /** Return the value to apply. An untouched property does not acquire a lease. */
    float write(float current, float next) {
        if (!owned) {
            if (current == next) return current;
            baseline = current;
            owned = true;
        } else if (current != written) {
            baseline = current;
        }
        written = next;
        return next;
    }

    /** Return the remaining owner's value, then release this lease exactly once. */
    float restore(float current) {
        float restored = owned && current == written ? baseline : current;
        owned = false;
        return restored;
    }

    boolean owned() { return owned; }
}
