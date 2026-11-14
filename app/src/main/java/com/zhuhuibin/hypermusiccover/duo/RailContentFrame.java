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

    static RailContentFrame inWindow(FoldMusicLayout.Box content, float density, int parentLeft,
                                     float windowWidth, float windowHeight, boolean combined) {
        // Single inner pages retain OEM widths. Combined mode uses its separately
        // calculated player frame, never an outer-display width as a substitute.
        return ownsNativeBounds(windowWidth, windowHeight, density) ? in(content, density, parentLeft) : null;
    }

    static boolean ownsNativeBounds(float width, float height, float density) {
        return FoldMusicLayout.finite(width, height, density) && width > 0f && height > 0f && density > 0f
                && Math.min(width, height) / density < 600f;
    }


    static FoldMusicLayout.Box presentation(FoldMusicLayout.Box rail, float width, float height,
                                            float density, float rightInset, boolean combined) {
        // The right sidebar is occupied in every page and on every display. Native card
        // width remains a separate policy; it must not change this shared center axis.
        return rail;
    }

    static RailContentFrame aroundCard(FoldMusicLayout.Box player, int parentLeft,
                                       float leading, float trailing) {
        if (player == null || !FoldMusicLayout.finite(player.left, player.right, leading, trailing)
                || leading < 0f || trailing < 0f || player.width() <= 0f) return null;
        int left = Math.round(player.left - leading);
        int right = Math.round(player.right + trailing);
        return right > left ? new RailContentFrame(left - parentLeft, right - left) : null;
    }

    /** Move a native-width column without borrowing its animated width as a new baseline. */
    static float columnTranslation(float currentX, float drawnLeft, float desiredLeft, float parentScale) {
        if (!FoldMusicLayout.finite(currentX, drawnLeft, desiredLeft, parentScale)
                || Math.abs(parentScale) < .001f) return Float.NaN;
        return currentX + (desiredLeft - drawnLeft) / parentScale;
    }

    int right() { return left + width; }
    boolean matches(int otherLeft, int otherWidth) { return left == otherLeft && width == otherWidth; }
}
