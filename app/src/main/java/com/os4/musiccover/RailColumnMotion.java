package com.os4.musiccover;

/** One affine transform from a canonical button column to its actual drawn column. */
final class RailColumnMotion {
    private RailColumnMotion() {}
    static final class Transform {
        final float scale, x, y;
        Transform(float scale, float x, float y) { this.scale = scale; this.x = x; this.y = y; }
    }
    static Transform follow(float restX, float restY, float restSpan,
                            float drawnX, float drawnY, float drawnSpan) {
        if (!FoldMusicLayout.finite(restX, restY, restSpan, drawnX, drawnY, drawnSpan)
                || restSpan < 1f || drawnSpan < 1f) return null;
        float scale = drawnSpan / restSpan;
        if (scale < .5f || scale > 1.5f) return null;
        return new Transform(scale, drawnX - restX * scale, drawnY - restY * scale);
    }
}
