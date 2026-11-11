package com.zhuhuibin.hypermusiccover.duo;

/** Shared, Android-free geometry for artwork, the native player and reflowed lyrics. */
final class FoldMusicLayout {
    private FoldMusicLayout() {}

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

    /** All inputs are in the same, unscaled window coordinates; unknown edges fail closed. */
    static Columns columns(float width, float height, float density, int smallestWidthDp,
                           float insetLeft, float insetTop, float insetRight, float insetBottom,
                           float clockBottom, float playerHeight, float shortcutsTop) {
        if (!wideWindow(width, height, density, smallestWidthDp)
                || !finite(insetLeft, insetTop, insetRight, insetBottom, clockBottom, playerHeight)
                || insetLeft < 0f || insetTop < 0f || insetRight < 0f || insetBottom < 0f
                || clockBottom < 0f || playerHeight <= 0f) return null;
        float gap = 16f * density;
        float left = insetLeft + gap, right = width - insetRight - gap;
        float top = Math.max(insetTop, clockBottom) + gap;
        float bottom = height - insetBottom - gap;
        if (Float.isFinite(shortcutsTop) && shortcutsTop > 0f) {
            bottom = Math.min(bottom, shortcutsTop - gap);
        }
        float usable = right - left - gap;
        float columnWidth = usable * 0.46f;
        float playerWidth = columnWidth;
        float playerLeft = left + (columnWidth - playerWidth) / 2;
        float lyricLeft = left + columnWidth + gap;
        float playerTop = bottom - playerHeight;
        if (columnWidth < 240f * density || right - lyricLeft < 260f * density
                || playerTop - gap - top < 120f * density) return null;
        return new Columns(new Box(left, top, left + columnWidth, playerTop - gap),
                new Box(playerLeft, playerTop, playerLeft + playerWidth, bottom),
                new Box(lyricLeft, top, right, bottom));
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
