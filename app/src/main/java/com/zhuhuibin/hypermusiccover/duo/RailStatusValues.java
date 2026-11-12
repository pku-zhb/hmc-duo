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
    /** Generation labels used by the reference vector alphabet, using Android radio type IDs. */
    static String networkLabel(int overrideType, int networkType) {
        switch (overrideType) {
            case 1: case 2: return "4G";
            case 3: case 4: case 5: return "5G";
            default:
                switch (networkType) {
                    case 1: case 2: case 4: case 7: case 11: case 16: return "2G";
                    case 3: case 5: case 6: case 8: case 9: case 10:
                    case 12: case 14: case 15: case 17: return "3G";
                    case 13: return "4G";
                    case 20: return "5G";
                    default: return "";
                }
        }
    }
    private RailStatusValues() {}
}
