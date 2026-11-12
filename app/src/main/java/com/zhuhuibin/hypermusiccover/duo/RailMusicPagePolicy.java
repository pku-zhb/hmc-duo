package com.zhuhuibin.hypermusiccover.duo;

/** Page intent, independent of asynchronous lyric lookup or layout availability. */
final class RailMusicPagePolicy {
    static final int CLOCK = 0, ARTWORK = 1, LYRICS = 2, ARTWORK_AND_LYRICS = 3;
    private RailMusicPagePolicy() {}
    static int current(boolean cover, boolean lyricsRequested) {
        return current(cover, lyricsRequested, false);
    }
    static int current(boolean cover, boolean lyricsRequested, boolean combinedRequested) {
        return !cover ? CLOCK : !lyricsRequested ? ARTWORK
                : combinedRequested ? ARTWORK_AND_LYRICS : LYRICS;
    }
    static boolean showsLyrics(int page) {
        return page == LYRICS || page == ARTWORK_AND_LYRICS;
    }
    static int next(int current, boolean lyricsEnabled) {
        return next(current, lyricsEnabled, false);
    }
    static int next(int current, boolean lyricsEnabled, boolean combinedAvailable) {
        if (current == CLOCK) return ARTWORK;
        if (current == ARTWORK && lyricsEnabled) return LYRICS;
        if (current == LYRICS && lyricsEnabled && combinedAvailable) return ARTWORK_AND_LYRICS;
        return CLOCK;
    }
}
