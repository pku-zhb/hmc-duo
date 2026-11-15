package com.zhuhuibin.hypermusiccover.duo;

/** Strip only the shared native affine from measured inputs before fitting natural layout. */
final class ContentMotionMeasurement {
    private final float scaleX, scaleY, originX, originY, layoutX, layoutY;

    private ContentMotionMeasurement(float scaleX, float scaleY, float originX, float originY,
                                     float layoutX, float layoutY) {
        this.scaleX = scaleX; this.scaleY = scaleY;
        this.originX = originX; this.originY = originY;
        this.layoutX = layoutX; this.layoutY = layoutY;
    }

    /** Both unit rectangles have the same natural size; only the first includes native motion. */
    static ContentMotionMeasurement from(FoldMusicLayout.Box ownerUnitInWindow,
                                          FoldMusicLayout.Box naturalWindowUnitInOwner) {
        if (!valid(ownerUnitInWindow) || !valid(naturalWindowUnitInOwner)) return null;
        float scaleX = ownerUnitInWindow.width() / naturalWindowUnitInOwner.width();
        float scaleY = ownerUnitInWindow.height() / naturalWindowUnitInOwner.height();
        if (!FoldMusicLayout.finite(scaleX, scaleY) || scaleX <= 0f || scaleY <= 0f) return null;
        return new ContentMotionMeasurement(scaleX, scaleY,
                ownerUnitInWindow.left, ownerUnitInWindow.top,
                -naturalWindowUnitInOwner.left, -naturalWindowUnitInOwner.top);
    }

    private static boolean valid(FoldMusicLayout.Box box) {
        return box != null && FoldMusicLayout.finite(box.left, box.top, box.right, box.bottom,
                box.width(), box.height())
                && box.width() > 0f && box.height() > 0f;
    }

    private FoldMusicLayout.Box naturalPlayer(FoldMusicLayout.Box measured) {
        if (!valid(measured)) return null;
        return new FoldMusicLayout.Box(layoutX + (measured.left - originX) / scaleX,
                layoutY + (measured.top - originY) / scaleY,
                layoutX + (measured.right - originX) / scaleX,
                layoutY + (measured.bottom - originY) / scaleY);
    }

    FoldMusicLayout.Columns columns(float width, float height, float density, int smallestWidthDp,
                                   float insetLeft, float insetTop, float insetRight, float insetBottom,
                                   float measuredClockBottom, FoldMusicLayout.Box measuredPlayer,
                                   float naturalShortcutsTop) {
        // Window/content dimensions, insets and dp parameters are already natural layout
        // inputs. Inverting a finished plan would scale those and reflow the lyric paragraph.
        float clock = layoutY + (measuredClockBottom - originY) / scaleY;
        return FoldMusicLayout.columns(width, height, density, smallestWidthDp,
                insetLeft, insetTop, insetRight, insetBottom, clock,
                naturalPlayer(measuredPlayer), naturalShortcutsTop);
    }
}
