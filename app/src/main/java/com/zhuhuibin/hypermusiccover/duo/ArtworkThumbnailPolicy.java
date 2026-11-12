package com.zhuhuibin.hypermusiccover.duo;

/** Keep the native artwork slot stable while its contents name the next page action. */
final class ArtworkThumbnailPolicy {
    private ArtworkThumbnailPolicy() {}

    static boolean fixedControls(boolean rail, boolean onKeyguard, boolean capture) {
        return rail && onKeyguard && !capture;
    }

    static float hideProgress(boolean fixedControls, float morphProgress) {
        return fixedControls ? 0f : morphProgress;
    }

    static boolean replaceWithPageAction(boolean fixedControls, int nextPage) {
        return fixedControls && nextPage != RailMusicPagePolicy.ARTWORK;
    }

    static boolean veil(boolean fixedControls, boolean artworkExpanded, boolean screenOn,
                        boolean aod, boolean leavingKeyguard) {
        return fixedControls && artworkExpanded && screenOn && !aod && !leavingKeyguard;
    }
}
