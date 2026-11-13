package com.zhuhuibin.hypermusiccover.duo;

/** The latest native sampling flags survive a temporary local-backdrop override. */
final class NativeSamplingLease {
    static final class Flags {
        final boolean passWindow;
        final int mode, blurType;
        Flags(int mode, boolean passWindow, int blurType) {
            this.mode = mode;
            this.passWindow = passWindow;
            this.blurType = blurType;
        }
    }

    private boolean bound, attached, nativePassWindow, local, dirty;
    private int nativeMode, nativeBlurType;

    void bind(int mode, boolean passWindow, int blurType) {
        bound = true;
        attached = true;
        nativeMode = mode;
        nativePassWindow = passWindow;
        nativeBlurType = blurType;
        local = false;
        dirty = true;
    }

    void nativeMode(int value) {
        if (!bound) return;
        nativeMode = value;
        dirty = true;
    }

    void nativeContainerChanged() { if (bound) dirty = true; }

    void nativePassWindow(boolean value) {
        if (!bound) return;
        nativePassWindow = value;
        dirty = true;
    }

    void nativeBlurType(int value) {
        if (!bound) return;
        nativeBlurType = value;
        dirty = true;
    }

    /** Detach pauses writes without forgetting the native domain or the pending restoration. */
    void attachmentChanged(boolean value) {
        if (attached == value) return;
        attached = value;
        if (value) dirty = true;
    }

    boolean needsApply(boolean wantLocal) { return bound && attached && (dirty || local != wantLocal); }
    boolean bound() { return bound; }
    boolean local() { return local; }
    Flags nativeFlags() { return new Flags(nativeMode, nativePassWindow, nativeBlurType); }
    Flags target(boolean wantLocal) {
        // OEM local-content sampling: retain glass/radii/palettes, change only its input domain.
        return wantLocal ? new Flags(1, false, 1) : nativeFlags();
    }
    void committed(boolean wantLocal) { local = wantLocal; dirty = false; }
    void clear() { bound = attached = local = dirty = false; }
}
