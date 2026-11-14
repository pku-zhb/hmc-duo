package com.zhuhuibin.hypermusiccover.duo;

/** Shared, Android-free geometry for artwork, the native player and reflowed lyrics. */
final class FoldMusicLayout {
    private FoldMusicLayout() {}
    static final float GAP_DP = 16f, MIN_LYRIC_DP = 260f;

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

    /** Equal usable columns; retain the native header's vertical stacking and touch frame. */
    static Columns columns(float width, float height, float density, int smallestWidthDp,
                           float insetLeft, float insetTop, float insetRight, float insetBottom,
                           float clockBottom, Box nativePlayer, float shortcutsTop) {
        // Runtime checks the full window's orientation before subtracting the sidebar.
        if (!finite(width, height, density) || width <= 0f || height <= 0f || density <= 0f
                || smallestWidthDp < 600 || height / density < 400f || nativePlayer == null
                || !finite(insetLeft, insetTop, insetRight, insetBottom, clockBottom,
                    nativePlayer.top, nativePlayer.bottom)
                || insetLeft < 0f || insetTop < 0f || insetRight < 0f || insetBottom < 0f
                || clockBottom < 0f || nativePlayer.height() <= 0f) return null;
        float gap = GAP_DP * density;
        float left = insetLeft + gap, right = width - insetRight - gap;
        float column = (right - left - gap) / 2f;
        float top = Math.max(insetTop, clockBottom) + gap;
        float bottom = height - insetBottom - gap;
        if (Float.isFinite(shortcutsTop) && shortcutsTop > 0f)
            bottom = Math.min(bottom, shortcutsTop - gap);
        float artworkBottom = Math.min(nativePlayer.top - gap, bottom);
        float side = Math.min(column, artworkBottom - top);
        if (column < MIN_LYRIC_DP * density || side < 120f * density || bottom <= top
                || nativePlayer.bottom > bottom + gap) return null;
        // A short artwork slot reduces the player to the same square width, instead of
        // consuming the lyric column. Its native 75/100 height is independent of this width.
        float playerLeft = left + (column - side) / 2f;
        Box player = new Box(playerLeft, nativePlayer.top, playerLeft + side, nativePlayer.bottom);
        return new Columns(new Box(player.left, top, player.right, artworkBottom), player,
                new Box(left + column + gap, top, right, bottom));
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
