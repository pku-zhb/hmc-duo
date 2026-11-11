package com.zhuhuibin.hypermusiccover.duo;

/** A stable next-action control: native clock, artwork, lyrics, native clock. */
final class RailMusicPagePolicy {
    static final int CLOCK = 0, ARTWORK = 1, LYRICS = 2;
    private RailMusicPagePolicy() {}
    static int current(boolean cover, boolean lyricsRequested) {
        return !cover ? CLOCK : lyricsRequested ? LYRICS : ARTWORK;
    }
    static int next(int current, boolean lyricsEnabled) {
        if (current == CLOCK) return ARTWORK;
        if (current == ARTWORK && lyricsEnabled) return LYRICS;
        return CLOCK;
    }
}
