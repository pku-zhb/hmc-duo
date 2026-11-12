package com.zhuhuibin.hypermusiccover.duo;

/** An absolute horizontal slot, independent of any native view's current width or position. */
final class RailContentFrame {
    static final float CARD_INSET_DP = 11.25f;
    final int left, width;

    RailContentFrame(int left, int width) {
        this.left = left;
        this.width = width;
    }

    static RailContentFrame in(FoldMusicLayout.Box content, float density, int parentLeft) {
        if (content == null || !FoldMusicLayout.finite(content.left, content.right, density)
                || density <= 0f) return null;
        int left = Math.round(content.left + CARD_INSET_DP * density);
        int right = Math.round(content.right - CARD_INSET_DP * density);
        return right > left ? new RailContentFrame(left - parentLeft, right - left) : null;
    }

    int right() { return left + width; }
    boolean matches(int otherLeft, int otherWidth) { return left == otherLeft && width == otherWidth; }
}
