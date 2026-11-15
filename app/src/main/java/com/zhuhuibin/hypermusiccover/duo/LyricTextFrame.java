package com.zhuhuibin.hypermusiccover.duo;

/** A logical text width and a separately mapped presentation center. */
final class LyricTextFrame {
    final int width;
    final float left;

    private LyricTextFrame(int width, float left) {
        this.width = width;
        this.left = left;
    }

    static LyricTextFrame at(float logicalWidth, float mappedCenter) {
        if (!Float.isFinite(logicalWidth) || logicalWidth <= 0f
                || !Float.isFinite(mappedCenter)) return null;
        int width = Math.max(1, Math.round(logicalWidth));
        return new LyricTextFrame(width, mappedCenter - width / 2f);
    }
}
