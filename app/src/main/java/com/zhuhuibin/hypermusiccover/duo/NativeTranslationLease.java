package com.zhuhuibin.hypermusiccover.duo;

/** Native writes are events, including an unchanged value; desired and committed X stay separate. */
final class NativeTranslationLease {
    private float baseline, written = Float.NaN;
    NativeTranslationLease(float nativeX) { baseline = nativeX; }
    void nativeWrite(float value) { baseline = value; written = Float.NaN; }
    void wrote(float value) { written = value; }
    float restore(float current) { return current == written ? baseline : current; }
    void release() { written = Float.NaN; }
}
