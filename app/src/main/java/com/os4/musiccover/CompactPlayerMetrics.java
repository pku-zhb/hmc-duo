package com.os4.musiccover;

/** Reduce vertical space while retaining native typography, track thickness and action hit height. */
final class CompactPlayerMetrics {
    static int cap(int nativePixels, float dp, float density) {
        return nativePixels > 0 ? Math.min(nativePixels, Math.round(dp * density)) : nativePixels;
    }
    static int trackPadding(int nativeHeight, int nativePadding, int compactHeight) {
        return Math.max(0, nativePadding - Math.max(0, nativeHeight - compactHeight) / 2);
    }
    static int gap(int value) { return Math.max(0, Math.round(value * .5f)); }
    static int cardMinimum(int nativeMinimum, float density) {
        return nativeMinimum > 0 ? Math.min(nativeMinimum, Math.max(Math.round(132f * density),
                nativeMinimum - Math.round(22f * density))) : nativeMinimum;
    }
    private CompactPlayerMetrics() {}
}
