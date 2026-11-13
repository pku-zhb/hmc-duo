package com.zhuhuibin.hypermusiccover.duo;

/** Symmetric alpha feather inside the actual native clipping boundary. */
final class RailNotificationEdge {
    static final float WIDTH_DP = 24f;
    static final int MAX_FEATHER_PX = 192;
    final int width, height, maxWidth;

    private RailNotificationEdge(int width, int height, int maxWidth) {
        this.width = width; this.height = height; this.maxWidth = maxWidth;
    }

    static RailNotificationEdge resolve(int width, int height, float density) {
        if (width <= 0 || height <= 0 || !Float.isFinite(density) || density <= 0f) return null;
        int band = Math.min(MAX_FEATHER_PX, Math.min(width / 4, Math.max(1, Math.round(WIDTH_DP * density))));
        return band > 0 ? new RailNotificationEdge(width, height, band) : null;
    }

    /** A small swipe narrows the band; it must never leave a nonzero alpha at the hard cut. */
    int featherWidth(float translation) {
        if (!Float.isFinite(translation) || translation == 0f) return 0;
        return Math.max(1, Math.min(maxWidth, Math.round(Math.abs(translation))));
    }

    Region region(float rowLeft, float rowTop, float rowRight, float rowBottom,
                  int clipLeft, int clipTop, int clipRight, int clipBottom, float translation) {
        int band = featherWidth(translation);
        if (band == 0 || !Float.isFinite(rowLeft) || !Float.isFinite(rowTop)
                || !Float.isFinite(rowRight) || !Float.isFinite(rowBottom)
                || rowRight <= rowLeft || rowBottom <= rowTop) return null;
        int left = Math.max(Math.max(0, clipLeft), (int) Math.floor(rowLeft));
        int right = Math.min(Math.min(width, clipRight), (int) Math.ceil(rowRight));
        int top = Math.max(Math.max(0, clipTop), (int) Math.floor(rowTop));
        int bottom = Math.min(Math.min(height, clipBottom), (int) Math.ceil(rowBottom));
        if (right <= left || bottom <= top) return null;
        band = Math.min(band, right - left);
        return translation < 0f ? new Region(left, top, left + band, bottom, true, clipLeft, left + band)
                : new Region(right - band, top, right, bottom, false, right - band, clipRight);
    }

    static float opacity(float x, int width, boolean left) {
        if (width <= 0 || !Float.isFinite(x)) return 0f;
        float t = Math.max(0f, Math.min(1f, x / width));
        float p = t * t * (3f - 2f * t);
        return left ? p : 1f - p;
    }

    static final class Region {
        final int left, top, right, bottom;
        final int clearLeft, clearRight;
        final boolean leftSide;
        Region(int left, int top, int right, int bottom, boolean leftSide, int clearLeft, int clearRight) {
            this.left = left; this.top = top; this.right = right; this.bottom = bottom;
            this.leftSide = leftSide;
            this.clearLeft = clearLeft; this.clearRight = clearRight;
        }
        int width() { return right - left; }
    }
}
