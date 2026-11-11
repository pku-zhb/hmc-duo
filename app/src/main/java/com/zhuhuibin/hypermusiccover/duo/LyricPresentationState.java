package com.zhuhuibin.hypermusiccover.duo;

/** Keeps lyric rendering independent of the panel's temporary DOZE/DOZE_SUSPEND flips. */
final class LyricPresentationState {
    private boolean ambient;

    boolean update(boolean screenOn, boolean clockInAod, boolean awakeSettled) {
        // The clock receives sleep before SCREEN_OFF. On wake, SCREEN_ON and the clock's
        // entry arrive separately; keep one text treatment until both have settled.
        if (!screenOn || clockInAod) ambient = true;
        else if (awakeSettled) ambient = false;
        return ambient;
    }

    static boolean waitsForBlur(boolean blurRequested, boolean backdropHidden) {
        return blurRequested && !backdropHidden;
    }
}
