package com.zhuhuibin.hypermusiccover.duo;

/** An exit belongs to a lock session; repeated showing/doze reports cannot begin another. */
final class KeyguardExitState {
    private boolean showingKnown;
    private boolean showing;
    private boolean leaving;

    synchronized boolean showing(boolean value) {
        boolean newSession = value && showingKnown && !showing;
        showingKnown = true;
        showing = value;
        if (!value) leaving = true;
        else if (newSession) leaving = false;
        return leaving;
    }

    synchronized boolean exit(boolean value) {
        // Clearing the native flag after the keyguard is already hidden is cleanup, not
        // cancellation. Only a still-showing lock screen can resume its current session.
        leaving = value || showingKnown && !showing;
        return leaving;
    }

    synchronized boolean sleep() {
        // The explicit sleep lifecycle starts a new session even if the OEM never reported
        // a hidden intermediate frame (for example a cancelled biometric transition).
        leaving = false;
        return false;
    }
}
