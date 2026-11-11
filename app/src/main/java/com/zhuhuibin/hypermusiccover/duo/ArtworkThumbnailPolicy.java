package com.zhuhuibin.hypermusiccover.duo;

/** The rail player's thumbnail remains a stable control while its larger artwork is open. */
final class ArtworkThumbnailPolicy {
    private ArtworkThumbnailPolicy() {}

    static boolean fixedControls(boolean rail, boolean onKeyguard, boolean capture) {
        return rail && onKeyguard && !capture;
    }

    static float hideProgress(boolean fixedControls, float morphProgress) {
        return fixedControls ? 0f : morphProgress;
    }

    static boolean veil(boolean fixedControls, boolean artworkExpanded, boolean screenOn,
                        boolean aod, boolean leavingKeyguard) {
        return fixedControls && artworkExpanded && screenOn && !aod && !leavingKeyguard;
    }
}
