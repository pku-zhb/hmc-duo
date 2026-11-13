package com.zhuhuibin.hypermusiccover.duo;

/** A preference can suspend a page without losing the app's latest readiness signal. */
final class ImmersiveSceneAvailability {
    static final boolean DEFAULT_ENABLED = true;
    private boolean enabled = DEFAULT_ENABLED;
    private boolean armed;

    boolean enabled() { return enabled; }
    boolean armed() { return armed; }
    boolean ready() { return enabled && armed; }

    boolean setEnabled(boolean value) {
        if (enabled == value) return false;
        enabled = value;
        return true;
    }

    boolean setArmed(boolean value) {
        if (armed == value) return false;
        armed = value;
        return true;
    }
}
