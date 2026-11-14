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
        // Inner cards always retain the OEM width. Combined artwork/lyrics may move
        // that native column, but must not turn an outer rail width into a size override.
        return ownsNativeBounds(windowWidth, windowHeight, density) ? in(content, density, parentLeft) : null;
    }

    static boolean ownsNativeBounds(float width, float height, float density) {
        return FoldMusicLayout.finite(width, height, density) && width > 0f && height > 0f && density > 0f
                && Math.min(width, height) / density < 600f;
    }


    static FoldMusicLayout.Box presentation(FoldMusicLayout.Box rail, float width, float height,
                                            float density, float rightInset, boolean combined) {
        if (rail == null || ownsNativeBounds(width, height, density) || combined) return rail;
        return new FoldMusicLayout.Box(rail.left, rail.top, width - rightInset, rail.bottom);
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
