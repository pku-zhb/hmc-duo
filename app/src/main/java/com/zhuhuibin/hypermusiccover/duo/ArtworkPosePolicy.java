package com.zhuhuibin.hypermusiccover.duo;

/** A committed native-motion frame is already fitted, not another local layout input. */
final class ArtworkPosePolicy {
    static CoverCardStyle.Rect fit(CoverCardStyle.Rect rect, boolean motionOwned,
                                   boolean innerPortrait, float playerWidth) {
        if (motionOwned || !innerPortrait) return rect;
        return FoldArtworkLayout.cap(rect, playerWidth);
    }

    static boolean carryLocal(boolean settled, boolean afterBigClock, boolean motionOwned) {
        return settled && !afterBigClock && !motionOwned;
    }

    private ArtworkPosePolicy() {}
}
