package com.zhuhuibin.hypermusiccover.duo;

/** Only the per-display eligibility gate may be extended; master/style policy stays native. */
final class InnerScreenAodPolicy {
    private InnerScreenAodPolicy() {}

    static boolean enabled(boolean nativeEnabled, boolean optIn, boolean nativeMaster,
                           boolean foldInner) {
        return nativeEnabled || optIn && nativeMaster && foldInner;
    }
}
