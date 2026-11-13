package com.zhuhuibin.hypermusiccover.duo;

/** The artwork clock row uses content bounds, never an animated native clock width. */
final class CompactClockRowLayout {
    private CompactClockRowLayout() {}

    static boolean requested(boolean enabled, float width, float height, float density,
                             boolean cover, boolean player, boolean notifications) {
        return enabled && supportsDisplay(width, height, density) && cover && !player && !notifications;
    }

    /** All valid display orientations share the preference; face/date safety stays in runtime. */
    static boolean supportsDisplay(float width, float height, float density) {
        return width > 0f && height > 0f && density > 0f
                && Float.isFinite(width) && Float.isFinite(height)
                && Float.isFinite(density);
    }

    static final class Row {
        final float timeLeft, dateLeft, timeWidth, scale;
        Row(float timeLeft, float dateLeft, float timeWidth, float scale) {
            this.timeLeft = timeLeft; this.dateLeft = dateLeft;
            this.timeWidth = timeWidth; this.scale = scale;
        }
    }

    static final class DateInk {
        final float left, width, baseline;
        DateInk(float left, float width, float baseline) {
            this.left = left; this.width = width; this.baseline = baseline;
        }
    }

    /** ClassicTextAreaView draws two strings and its icons directly, without a TextView Layout.
     * Keep its complete native drawing together, including the optional trailing rectangle. */
    static DateInk classicDate(float viewWidth, float viewHeight, float paddingStart,
                               float paddingEnd, float prefixWidth, float bodyWidth,
                               float measuredIcons, float iconAdvance, float iconExtent,
                               float rectangleWidth, boolean rtl, float ascent, float descent) {
        float[] dimensions = {viewWidth, viewHeight, paddingStart, paddingEnd, prefixWidth,
                bodyWidth, measuredIcons, iconAdvance, iconExtent, rectangleWidth};
        for (float dimension : dimensions)
            if (!Float.isFinite(dimension) || dimension < 0f) return null;
        if (!(viewWidth > 0f) || !(viewHeight > 0f) || !(prefixWidth + bodyWidth > 0f)
                || !Float.isFinite(ascent + descent) || !(descent > ascent)) return null;
        float left, right;
        if (rtl) {
            left = viewWidth - (prefixWidth + bodyWidth + measuredIcons + paddingStart + paddingEnd);
            float iconsLeft = left + rectangleWidth + bodyWidth;
            float prefixLeft = Math.max(iconsLeft + iconAdvance, viewWidth - paddingEnd - prefixWidth);
            right = Math.max(iconsLeft + iconExtent, prefixLeft + prefixWidth);
        } else {
            left = paddingStart;
            float iconsLeft = left + prefixWidth;
            right = Math.max(iconsLeft + iconExtent, iconsLeft + iconAdvance + bodyWidth + rectangleWidth);
        }
        return new DateInk(left, right - left, viewHeight / 2f - (ascent + descent) / 2f);
    }

    static Row place(float left, float right, float timeWidth, float dateWidth,
                     float gap, boolean beforeDate) {
        if (!(right > left) || !Float.isFinite(left + right) || !(timeWidth > 0f) || !(dateWidth > 0f)
                || !Float.isFinite(timeWidth + dateWidth + gap)) return null;
        float scale = Math.min(1f, (right - left) / (timeWidth + dateWidth + Math.max(0f, gap)));
        float time = timeWidth * scale, date = dateWidth * scale, space = Math.max(0f, gap) * scale;
        float start = (left + right - time - date - space) / 2f;
        return beforeDate ? new Row(start, start + time + space, time, scale)
                : new Row(start + date + space, start, time, scale);
    }

    static float progress(float from, boolean compact, float spring) {
        return Math.max(0f, Math.min(1f, from + ((compact ? 1f : 0f) - from) * spring));
    }

    /** The original face travels first; its single-line replacement joins at the same endpoint. */
    static float alpha(float progress) {
        float t = Math.max(0f, Math.min(1f, (progress - 0.55f) / 0.45f));
        return t * t * (3f - 2f * t);
    }
}
