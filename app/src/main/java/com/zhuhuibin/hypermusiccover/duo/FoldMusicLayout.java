package com.zhuhuibin.hypermusiccover.duo;

/** Shared, Android-free geometry for artwork, the native player and reflowed lyrics. */
final class FoldMusicLayout {
    private FoldMusicLayout() {}
    static final float GAP_DP = 16f, MIN_PLAYER_DP = 240f, MIN_LYRIC_DP = 260f;

    static boolean wideWindow(float width, float height, float density, int smallestWidthDp) {
        return finite(width, height, density) && width > 0f && height > 0f && density > 0f
                && smallestWidthDp >= 600
                && width / density >= 600f && height / density >= 400f
                && width > height * 1.05f;
    }

    static final class Box {
        final float left, top, right, bottom;
        Box(float left, float top, float right, float bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
        float width() { return right - left; }
        float height() { return bottom - top; }
    }

    static final class Columns {
        final Box artworkRoom, player, lyrics;
        Columns(Box artworkRoom, Box player, Box lyrics) {
            this.artworkRoom = artworkRoom;
            this.player = player;
            this.lyrics = lyrics;
        }
    }

    /** Keep the native player/header and every row below it in their existing vertical frame. */
    static Columns columns(float width, float height, float density, int smallestWidthDp,
                           float insetLeft, float insetTop, float insetRight, float insetBottom,
                           float clockBottom, Box nativePlayer, float shortcutsTop) {
        // Runtime checks the complete window's orientation. This width excludes the rail,
        // so reusing the full-window aspect threshold here can reject a usable inner screen.
        if (!finite(width, height, density) || width <= 0f || height <= 0f || density <= 0f
                || smallestWidthDp < 600 || height / density < 400f || nativePlayer == null
                || !finite(insetLeft, insetTop, insetRight, insetBottom, clockBottom,
                    nativePlayer.left, nativePlayer.top, nativePlayer.right, nativePlayer.bottom)
                || insetLeft < 0f || insetTop < 0f || insetRight < 0f || insetBottom < 0f
                || clockBottom < 0f || nativePlayer.height() <= 0f) return null;
        float gap = GAP_DP * density;
        float right = width - insetRight - gap;
        float top = Math.max(insetTop, clockBottom) + gap;
        float bottom = height - insetBottom - gap;
        if (Float.isFinite(shortcutsTop) && shortcutsTop > 0f)
            bottom = Math.min(bottom, shortcutsTop - gap);
        float lyricLeft = nativePlayer.right + gap;
        float artworkBottom = nativePlayer.top - gap;
        if (nativePlayer.left < insetLeft || nativePlayer.width() < MIN_PLAYER_DP * density
                || right - lyricLeft < MIN_LYRIC_DP * density || bottom <= top) return null;
        artworkBottom = Math.min(artworkBottom, bottom);
        if (artworkBottom - top >= 120f * density) {
            return new Columns(new Box(nativePlayer.left, top, nativePlayer.right, artworkBottom),
                    nativePlayer, new Box(lyricLeft, top, right, bottom));
        }
        // An expanded native stack can start immediately below the clock. Keep both scenes
        // reachable by stacking artwork over lyrics in the free right column, never by moving
        // a media child outside its header or pushing a live/notification row off screen.
        float available = bottom - top;
        float artworkHeight = Math.min(right - lyricLeft, Math.min(available * .4f,
                available - gap - 160f * density));
        if (artworkHeight < 120f * density) return null;
        return new Columns(new Box(lyricLeft, top, right, top + artworkHeight), nativePlayer,
                new Box(lyricLeft, top + artworkHeight + gap, right, bottom));
    }

    /** Same square-based coordinates used by the artwork renderer and its morph endpoints. */
    static CoverCardStyle.Rect artwork(Box room, float aspect, float density) {
        if (room == null || !finite(room.left, room.top, room.right, room.bottom, aspect, density)
                || aspect <= 0f || density <= 0f) return null;
        aspect = CoverCardStyle.aspect(aspect, 1f);
        float wr = Math.min(1f, aspect), hr = Math.min(1f, 1f / aspect);
        float side = Math.min(room.width() / wr, room.height() / hr);
        if (side * Math.min(wr, hr) < 96f * density) return null;
        return new CoverCardStyle.Rect((room.left + room.right - side) / 2f,
                (room.top + room.bottom - side) / 2f, side);
    }

    static boolean finite(float... values) {
        for (float value : values) if (!Float.isFinite(value)) return false;
        return true;
    }
}
