package com.zhuhuibin.hypermusiccover.duo;

/** A local safety edge: keep the content slot, soften only pixels approaching its right side. */
final class RailNotificationEdge {
    static final float WIDTH_DP = 24f;
    static final float BLUR_DP = 8f;
    static final int STRENGTH_STEPS = 64;

    final int right, width, radius, sourceLeft;

    private RailNotificationEdge(int right, int width, int radius) {
        this.right = right;
        this.width = width;
        this.radius = radius;
        // The GPU input includes the neighbouring sharp pixels that the edge samples.
        sourceLeft = Math.max(0, right - width - radius);
    }

    static RailNotificationEdge resolve(int width, int height, float density) {
        if (width <= 0 || height <= 0 || !Float.isFinite(density) || density <= 0f) return null;
        int band = Math.min(width / 4, Math.max(1, Math.round(WIDTH_DP * density)));
        if (band <= 0) return null;
        int radius = Math.min(band / 2, Math.max(1, Math.round(BLUR_DP * density)));
        return new RailNotificationEdge(width, band, radius);
    }

    int start() { return right - width; }
    int sourceWidth() { return right - sourceLeft; }

    /** The shader uses the same smoothstep: exact original at the start, zero at the boundary. */
    float progress(float x) {
        float t = Math.max(0f, Math.min(1f, (x - start()) / width));
        return t * t * (3f - 2f * t);
    }

    float opacity(float x) { return 1f - progress(x); }
    float blur(float x) { return radius * progress(x); }

    float strength(float translation) {
        if (!Float.isFinite(translation) || translation <= 0f) return 0f;
        float t = Math.min(1f, translation / width);
        return t * t * (3f - 2f * t);
    }

    Region region(float rowY, int clipTop, int clipBottom, int stackHeight, float translation) {
        int level = Math.round(strength(translation) * STRENGTH_STEPS);
        float strength = (float) level / STRENGTH_STEPS;
        if (strength <= 0f || !Float.isFinite(rowY) || clipBottom <= clipTop) return null;
        int top = Math.max(0, (int) Math.floor(rowY + clipTop));
        int bottom = Math.min(stackHeight, (int) Math.ceil(rowY + clipBottom));
        return bottom > top ? new Region(top, bottom, strength, level) : null;
    }

    static final class Region {
        final int top, bottom;
        final float strength;
        final int level;
        Region(int top, int bottom, float strength, int level) {
            this.top = top; this.bottom = bottom; this.strength = strength;
            this.level = level;
        }
    }

    private RailNotificationEdge() { throw new AssertionError(); }
}
