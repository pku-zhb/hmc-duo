package com.zhuhuibin.hypermusiccover.duo;

/** Full-screen navigation owns a compact clock before its remote map surface is available. */
final class NavigationAodPolicy {
    static boolean prepare(boolean fullAod, boolean keyguard, boolean liveCard, boolean ready, boolean opened) {
        return fullAod && keyguard && liveCard && ready && opened;
    }
    static boolean keepClock(boolean navigation, boolean fullAod) { return navigation && fullAod; }
    private NavigationAodPolicy() {}
}
