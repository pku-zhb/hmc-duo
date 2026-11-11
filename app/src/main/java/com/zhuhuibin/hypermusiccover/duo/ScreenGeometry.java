package com.zhuhuibin.hypermusiccover.duo;

/** An atomic screen-size snapshot: workers must never combine two different display forms. */
final class ScreenGeometry {
    final int width, height, densityDpi;
    final long revision;

    ScreenGeometry(int width, int height, int densityDpi, long revision) {
        this.width = width;
        this.height = height;
        this.densityDpi = densityDpi;
        this.revision = revision;
    }

    ScreenGeometry update(int width, int height, int densityDpi) {
        if (width <= 0 || height <= 0 || densityDpi <= 0) return this;
        return this.width == width && this.height == height && this.densityDpi == densityDpi
                ? this : new ScreenGeometry(width, height, densityDpi, revision + 1);
    }
}
