package com.zhuhuibin.hypermusiccover.duo;

/** Page choice must not remove the explicit lyrics switch on the dedicated lock-screen rail. */
final class LyricsButtonPolicy {
    static final int AVAILABLE = 0, LOADING = 1, UNAVAILABLE = 2;

    /** Use this track's result, never the previous track's held presentation state. */
    static int availability(boolean enabled, boolean currentContent, boolean loading) {
        return !enabled ? UNAVAILABLE : currentContent ? AVAILABLE : loading ? LOADING : UNAVAILABLE;
    }

    static int actionAvailability(int page, int availability) {
        return RailMusicPagePolicy.showsLyrics(page) ? availability : AVAILABLE;
    }

    static boolean offer(boolean rail, boolean cover, boolean enabled, boolean content, boolean pageAllows) {
        return enabled && content && (rail || cover) && (rail || pageAllows);
    }
    static boolean beside(boolean rail, boolean compact) {
        return !rail && compact;
    }
    static boolean animate(boolean rail) { return !rail; }
    private LyricsButtonPolicy() {}
}
