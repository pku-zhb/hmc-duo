package com.zhuhuibin.hypermusiccover.duo;

/** A repeated doze callback during biometric exit is not a new lock-screen session. */
final class AodWakePolicy {
    private AodWakePolicy() {}
    static boolean enterCover(boolean keyguardShowing, boolean goingAway) {
        return keyguardShowing && !goingAway;
    }
}
