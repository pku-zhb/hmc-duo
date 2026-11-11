package com.os4.musiccover;

/** Page choice must not remove the explicit lyrics switch on the dedicated lock-screen rail. */
final class LyricsButtonPolicy {
    static boolean offer(boolean rail, boolean cover, boolean enabled, boolean content, boolean pageAllows) {
        return enabled && content && (rail || cover) && (rail || pageAllows);
    }
    static boolean beside(boolean rail, boolean canvas, boolean compact) {
        return !rail && (canvas || compact);
    }
    static boolean animate(boolean rail) { return !rail; }
    private LyricsButtonPolicy() {}
}
