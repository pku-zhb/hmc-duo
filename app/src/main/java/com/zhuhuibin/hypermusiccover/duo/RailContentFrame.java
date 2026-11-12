package com.zhuhuibin.hypermusiccover.duo;

/** An absolute horizontal slot, independent of any native view's current width or position. */
final class RailContentFrame {
    static final float CARD_INSET_DP = 11.25f;
    // Stable Duo specification: the clean OEM inner-landscape stack measured 1246 px at
    // density 2.75. Read once during diagnosis, never learned from animated view bounds.
    static final float INNER_LANDSCAPE_WIDTH_DP = 453f;
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
        RailContentFrame full = in(content, density, parentLeft);
        if (full == null || !FoldArtworkLayout.innerLandscape(windowWidth, windowHeight, density))
            return full;
        int width = Math.min(full.width, Math.round(INNER_LANDSCAPE_WIDTH_DP * density));
        if (combined) {
            // Retain a useful lyric column on smaller inner screens without resizing per frame.
            int room = full.width - Math.round((FoldMusicLayout.GAP_DP + FoldMusicLayout.MIN_LYRIC_DP
                    + FoldMusicLayout.GAP_DP - CARD_INSET_DP) * density);
            if (room >= Math.round(FoldMusicLayout.MIN_PLAYER_DP * density)) width = Math.min(width, room);
            return new RailContentFrame(full.left, width);
        }
        return new RailContentFrame(full.left + (full.width - width) / 2, width);
    }

    int right() { return left + width; }
    boolean matches(int otherLeft, int otherWidth) { return left == otherLeft && width == otherWidth; }
}
