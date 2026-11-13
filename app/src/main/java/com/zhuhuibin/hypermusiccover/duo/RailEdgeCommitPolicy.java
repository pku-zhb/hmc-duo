package com.zhuhuibin.hypermusiccover.duo;

/** A native material refresh during a swipe must keep the same card's feather in that commit. */
final class RailEdgeCommitPolicy {
    static final int NATIVE = 0, MASK = 1, RESTORE = 2;
    static boolean maskNow(int role, boolean moving, boolean available) {
        return available && moving && (role == NATIVE || role == MASK);
    }
    private RailEdgeCommitPolicy() {}
}
