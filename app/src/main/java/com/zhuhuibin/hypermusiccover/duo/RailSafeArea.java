package com.zhuhuibin.hypermusiccover.duo;

/** A corner camera excludes its actual top/bottom band, not the entire side of a foldable. */
final class RailSafeArea {
    static FoldMusicLayout.Box resolve(float width, float height, float left, float top,
                                      float right, float bottom, FoldMusicLayout.Box[] cutouts) {
        if (cutouts != null) for (FoldMusicLayout.Box cutout : cutouts) {
            if (cutout == null || cutout.width() <= 0 || cutout.height() <= 0) continue;
            if (cutout.top <= 0 && cutout.bottom <= height * .25f) top = Math.max(top, cutout.bottom);
            else if (cutout.bottom >= height && cutout.top >= height * .75f)
                bottom = Math.max(bottom, height - cutout.top);
            else if (cutout.left <= 0) left = Math.max(left, cutout.right);
            else if (cutout.right >= width) right = Math.max(right, width - cutout.left);
        }
        return new FoldMusicLayout.Box(left, top, width - right, height - bottom);
    }
    private RailSafeArea() {}
}
