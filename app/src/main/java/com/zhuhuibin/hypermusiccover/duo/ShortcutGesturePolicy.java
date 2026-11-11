package com.zhuhuibin.hypermusiccover.duo;

/** Project a relocated left shortcut's inward drag onto the OEM's original rightward axis. */
final class ShortcutGesturePolicy {
    private ShortcutGesturePolicy() {}
    static float nativeX(boolean rightRail, boolean leftShortcut, float initialX, float physicalX) {
        return rightRail && leftShortcut && Float.isFinite(initialX) && Float.isFinite(physicalX)
                ? initialX - (physicalX - initialX) : physicalX;
    }
}
