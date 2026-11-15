package com.zhuhuibin.hypermusiccover.duo;

/** Distinguish an unmeasured owner frame from a measured, legitimately unavailable plan. */
final class LyricFrameReadiness {
    private long revision = -1;
    private int width, height;

    void reset() { revision = -1; width = height = 0; }

    void measured(long revision, int width, int height) {
        if (width <= 0 || height <= 0) { reset(); return; }
        this.revision = revision;
        this.width = width;
        this.height = height;
    }

    boolean pending(long currentRevision, int currentWidth, int currentHeight) {
        return revision < 0 || revision != currentRevision || width != currentWidth || height != currentHeight;
    }
}
