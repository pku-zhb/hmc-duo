package com.zhuhuibin.hypermusiccover.duo;

/** Unknown inputs remain unknown instead of drawing a full battery or full signal. */
final class RailStatusValues {
    static int batteryPercent(int level, int scale) {
        if (level < 0 || scale <= 0) return -1;
        return Math.round(Math.max(0f, Math.min(1f, level / (float) scale)) * 100f);
    }
    static int signalLevel(int level, boolean inService) {
        return !inService ? 0 : level < 0 ? -1 : Math.min(4, level);
    }
    static int wifiLevel(boolean connected, int rssi) {
        if (!connected || rssi <= -127 || rssi > 0) return -1;
        return rssi >= -55 ? 2 : rssi >= -78 ? 1 : 0;
    }
    private RailStatusValues() {}
}
