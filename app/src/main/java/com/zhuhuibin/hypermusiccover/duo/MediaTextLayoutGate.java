package com.zhuhuibin.hypermusiccover.duo;

/** One deferred draw for our text padding; an OEM layout loop can never suppress the window. */
final class MediaTextLayoutGate {
    private boolean deferred;
    boolean allow(boolean ourPaddingChanged) {
        if (ourPaddingChanged && !deferred) { deferred = true; return false; }
        if (!ourPaddingChanged) deferred = false;
        return true;
    }
}
