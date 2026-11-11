package com.zhuhuibin.hypermusiccover.duo;

/** Preserve native sizing intent while the rail owns the content's measured pixel width. */
final class RailContentWidth {
    static final int MATCH_PARENT = -1, WRAP_CONTENT = -2;
    private int nativeLayoutWidth, nativeMeasuredWidth, writtenWidth;

    RailContentWidth(int layoutWidth, int measuredWidth) {
        nativeLayoutWidth = writtenWidth = layoutWidth;
        nativeMeasuredWidth = Math.max(1, layoutWidth > 0 ? layoutWidth : measuredWidth);
    }

    void observe(int layoutWidth) {
        if (layoutWidth == writtenWidth) return;
        if (layoutWidth == MATCH_PARENT || layoutWidth == WRAP_CONTENT) {
            nativeLayoutWidth = layoutWidth;
        } else if (nativeLayoutWidth == WRAP_CONTENT && layoutWidth > 0) {
            // An intrinsic view becoming explicitly sized has no earlier contracted width.
            nativeLayoutWidth = nativeMeasuredWidth = layoutWidth;
        }
        // During reflow the OEM can write the contracted measured pixels back into its
        // LayoutParams. They are not a new uncontracted baseline. Display geometry changes
        // release this lease and capture a fresh native baseline instead.
    }

    int target(int parentAvailableWidth, float fraction) {
        if (nativeLayoutWidth == WRAP_CONTENT) return WRAP_CONTENT;
        int baseline = nativeLayoutWidth == MATCH_PARENT && parentAvailableWidth > 0
                ? parentAvailableWidth : nativeMeasuredWidth;
        if (!Float.isFinite(fraction) || fraction <= 0f) return writtenWidth;
        return Math.max(1, Math.round(baseline * Math.min(1f, fraction)));
    }

    void wrote(int width) { writtenWidth = width; }

    int restore(int currentWidth) {
        return currentWidth == writtenWidth ? nativeLayoutWidth : currentWidth;
    }
}
