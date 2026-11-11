package com.os4.musiccover;

/** Screen-space placement for both a direct-to-AOD entry and an awake-to-AOD transition. */
final class RailAodArtwork {
    private RailAodArtwork() {}
    static FoldMusicLayout.Box fit(FoldMusicLayout.Box content, FoldMusicLayout.Box fallback,
                                  float clockBottom, float mediaTop, float density, float aspect) {
        if (content == null || fallback == null) return fallback;
        float centerX = (content.left + content.right) / 2;
        FoldMusicLayout.Box centered = new FoldMusicLayout.Box(centerX - fallback.width() / 2,
                fallback.top, centerX + fallback.width() / 2, fallback.bottom);
        if (!FoldMusicLayout.finite(clockBottom, mediaTop, density, aspect)
                || density <= 0 || aspect <= 0) return centered;
        float gap = CoverCardStyle.GAP_DP * density;
        float top = Math.max(content.top, clockBottom) + gap;
        float bottom = Math.min(content.bottom, mediaTop) - gap;
        float width = content.width() - 2 * gap;
        float heightRatio = Math.min(1f, 1f / aspect);
        float side = Math.min(width, (bottom - top) / heightRatio);
        if (clockBottom < 0 || mediaTop <= clockBottom || side <= 0)
            return centered;
        float y = top + (bottom - top - side * heightRatio) / 2 - (side - side * heightRatio) / 2;
        return new FoldMusicLayout.Box(centerX - side / 2, y, centerX + side / 2, y + side);
    }
}
