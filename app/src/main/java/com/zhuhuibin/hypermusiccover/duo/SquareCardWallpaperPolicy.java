package com.zhuhuibin.hypermusiccover.duo;

/** Restore a replaced wallpaper once, independently of foreground artwork geometry. */
final class SquareCardWallpaperPolicy {
    enum Route { WALLPAPER, RESTORE_NATIVE, CARD_ONLY }
    private boolean restored;

    static boolean nativeBackdrop(boolean requested, boolean square) { return requested; }

    synchronized boolean releaseNeeded(boolean nativeBackground, boolean square) {
        // An exit cannot refresh native wallpaper that was never replaced in this session.
        return route(nativeBackground, square) != Route.CARD_ONLY;
    }

    synchronized Route route(boolean nativeBackground, boolean square) {
        if (!nativeBackdrop(nativeBackground, square)) {
            restored = false;
            return Route.WALLPAPER;
        }
        if (restored) return Route.CARD_ONLY;
        restored = true;
        return Route.RESTORE_NATIVE;
    }
}
