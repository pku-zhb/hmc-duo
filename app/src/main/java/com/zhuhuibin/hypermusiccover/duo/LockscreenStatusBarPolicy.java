package com.zhuhuibin.hypermusiccover.duo;

/** Only the lock-screen status strip is owned; shade and application status bars stay native. */
final class LockscreenStatusBarPolicy {
    static boolean hide(boolean enabled, boolean locked, boolean leaving, boolean bouncer,
                        boolean editor, boolean controlCenter) {
        return enabled && locked && !leaving && !bouncer && !editor && !controlCenter;
    }
    private LockscreenStatusBarPolicy() {}
}
