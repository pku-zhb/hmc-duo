package com.os4.musiccover;

/** A rail's square artwork is independent of the wallpaper renderer and its saved texture. */
final class SquareCardWallpaperPolicy {
    enum Route { WALLPAPER, RESTORE_NATIVE, CARD_ONLY }
    private boolean restored;

    static boolean nativeBackdrop(boolean rail, boolean square) { return rail && square; }

    synchronized boolean releaseNeeded(boolean rail, boolean square) {
        // An exit cannot refresh native wallpaper that was never replaced in this session.
        return route(rail, square) != Route.CARD_ONLY;
    }

    synchronized Route route(boolean rail, boolean square) {
        if (!nativeBackdrop(rail, square)) {
            restored = false;
            return Route.WALLPAPER;
        }
        if (restored) return Route.CARD_ONLY;
        restored = true;
        return Route.RESTORE_NATIVE;
    }
}
