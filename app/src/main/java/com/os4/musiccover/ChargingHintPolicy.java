package com.os4.musiccover;

/** Native indication type 3 is charging; battery protection and other prompt types stay native. */
final class ChargingHintPolicy {
    static boolean hide(boolean enabled, boolean locked, int indicationType, boolean hasContent, boolean batteryProtection) {
        return enabled && locked && indicationType == 3 && hasContent && !batteryProtection;
    }
    private ChargingHintPolicy() {}
}
