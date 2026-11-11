package com.os4.musiccover;

/** Native square artwork does not own the wallpaper engine or its unlock animation. */
final class WallpaperHandoffPolicy {
    static boolean fadeCoverWallpaper(boolean cover, boolean nativeBackdrop) {
        return cover && !nativeBackdrop;
    }
    static boolean reassertAodDim(boolean cover, boolean nativeBackdrop, boolean locked,
                                  boolean goingAway, boolean aod) {
        return fadeCoverWallpaper(cover, nativeBackdrop) && locked && !goingAway && aod;
    }
    private WallpaperHandoffPolicy() {}
}
