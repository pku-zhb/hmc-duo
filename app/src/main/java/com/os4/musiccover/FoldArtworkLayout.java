package com.os4.musiccover;

/** Fold presentation and fitting rules in one coordinate system. */
final class FoldArtworkLayout {
    static boolean innerPortrait(float width, float height, float density) {
        return inner(width, height, density) && height > width * 1.05f;
    }
    static boolean innerLandscape(float width, float height, float density) {
        return inner(width, height, density) && width > height * 1.05f;
    }
    private static boolean inner(float width, float height, float density) {
        return FoldMusicLayout.finite(width, height, density) && density > 0
                && Math.min(width, height) / density >= 600f;
    }
    static CoverCardStyle.Rect cap(CoverCardStyle.Rect rect, float playerWidth) {
        if (rect == null || !(playerWidth > 0) || !Float.isFinite(playerWidth)) return null;
        float side = Math.min(rect.side, playerWidth);
        float inset = (rect.side - side) / 2f;
        return new CoverCardStyle.Rect(rect.x + inset, rect.y + inset, side);
    }
    static float clockUnit(float current, float nativeUnit, float measuredUnit, float inkWidth,
                           float inkHeight, float availableWidth, float availableHeight, float minimum) {
        if (!FoldMusicLayout.finite(current, nativeUnit, measuredUnit, inkWidth, inkHeight,
                availableWidth, availableHeight, minimum) || measuredUnit <= 0 || inkWidth <= 0 || inkHeight <= 0)
            return current;
        float fit = Math.min(nativeUnit, Math.min(measuredUnit * availableWidth / inkWidth,
                measuredUnit * Math.max(0, availableHeight) / inkHeight));
        return Math.min(current, Math.max(Math.min(minimum, current), fit));
    }
    private FoldArtworkLayout() {}
}
