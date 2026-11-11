package com.zhuhuibin.hypermusiccover.duo;

/** Project a relocated left shortcut's inward drag onto the OEM's original rightward axis. */
final class ShortcutGesturePolicy {
    private ShortcutGesturePolicy() {}

    /** A visible icon can receive a gesture before its wake fade finishes. */
    static boolean shortcutVisible(float alpha) {
        return Float.isFinite(alpha) && alpha >= .1f;
    }

    static float nativeX(boolean rightRail, boolean leftShortcut, float initialX, float physicalX) {
        return rightRail && leftShortcut && Float.isFinite(initialX) && Float.isFinite(physicalX)
                ? initialX - (physicalX - initialX) : physicalX;
    }

    /** Forgive a thumb landing near either rim, without crossing into its neighbour. */
    static boolean shortcutHit(float dx, float dy, float radius, float density, float otherDistance) {
        if (!Float.isFinite(dx) || !Float.isFinite(dy) || !Float.isFinite(radius)
                || !Float.isFinite(density) || radius <= 0 || density <= 0) return false;
        float distance = (float) Math.hypot(dx, dy);
        return distance <= radius + 8f * density && distance < otherDistance;
    }
}
