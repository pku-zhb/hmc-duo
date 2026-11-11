package com.os4.musiccover;

/** Geometry shared by the vertical island and the remaining native lock-screen content. */
final class RightRailLayout {
    private RightRailLayout() {}
    static final class Plan {
        final FoldMusicLayout.Box pill, companion, content;
        final float controlsX, controlsY, controlsSpan, firstOffsetY, secondOffsetY;
        Plan(FoldMusicLayout.Box pill, FoldMusicLayout.Box companion, FoldMusicLayout.Box content,
             float controlsX, float controlsY, float controlsSpan, float firstOffsetY, float secondOffsetY) {
            this.pill = pill; this.companion = companion; this.content = content;
            this.controlsX = controlsX; this.controlsY = controlsY; this.controlsSpan = controlsSpan;
            this.firstOffsetY = firstOffsetY; this.secondOffsetY = secondOffsetY;
        }
    }

    /** Only an observed, right-hand vertical shortcut pair enables the MiDuo adapter. */
    static Plan plan(float width, float height, float density, float leftInset, float topInset,
                     float rightInset, float bottomInset, FoldMusicLayout.Box a,
                     FoldMusicLayout.Box b) {
        return plan(width, height, density, leftInset, topInset, rightInset, bottomInset, a, b,
                40.8f * density, 9f * density);
    }

    static Plan plan(float width, float height, float density, float leftInset, float topInset,
                     float rightInset, float bottomInset, FoldMusicLayout.Box a,
                     FoldMusicLayout.Box b, float visibleDiscDiameter, float indicatorExtent) {
        if (a == null || b == null || !FoldMusicLayout.finite(width, height, density,
                leftInset, topInset, rightInset, bottomInset, visibleDiscDiameter, indicatorExtent, a.left, a.top, a.right, a.bottom,
                b.left, b.top, b.right, b.bottom) || density <= 0f || width <= 0f || height <= 0f
                || leftInset < 0 || topInset < 0 || rightInset < 0 || bottomInset < 0
                || visibleDiscDiameter <= 0 || indicatorExtent < 0
                || a.width() <= 0 || a.height() <= 0 || b.width() <= 0 || b.height() <= 0) return null;
        if (a.left < leftInset || b.left < leftInset || a.right > width - rightInset
                || b.right > width - rightInset || a.top < 0 || b.top < 0
                || a.bottom > height || b.bottom > height) return null;
        float ax = (a.left + a.right) / 2, bx = (b.left + b.right) / 2;
        float ay = (a.top + a.bottom) / 2, by = (b.top + b.bottom) / 2;
        float diameter = Math.max(Math.max(a.width(), a.height()), Math.max(b.width(), b.height()));
        if (Math.min(ax, bx) < width * 0.65f || Math.abs(ax - bx) > diameter * 0.45f
                || Math.abs(ay - by) < diameter * 0.9f) return null;
        float gap = 12f * density;
        boolean narrow = width / density < 600f || width > height * 1.05f;
        float thickness = (narrow ? 46f : 48f) * density;
        float controlSpan = Math.max(Math.abs(ay - by), 72f * density);
        float controlY = Math.min((ay + by) / 2,
                height - bottomInset - gap - diameter / 2 - controlSpan / 2) - 24f * density;
        float firstY = controlY + (ay < by ? -1 : 1) * controlSpan / 2;
        float secondY = controlY + (ay < by ? 1 : -1) * controlSpan / 2;
        float centerX = (ax + bx) / 2;
        float right = centerX + thickness / 2, left = centerX - thickness / 2;
        float ceiling = topInset + gap + thickness + gap;
        float circleGap = Math.max(0f, controlSpan - visibleDiscDiameter);
        // Include the lowest curved indicator, not just the capsule's rectangular body.
        float limit = narrow
                ? Math.min(firstY, secondY) - visibleDiscDiameter / 2 - circleGap - indicatorExtent
                : Math.min(firstY, secondY) - diameter / 2 - gap;
        float companionTop = topInset + gap;
        float maxLength = limit - companionTop - thickness - gap;
        float length = Math.min(221f * density, maxLength);
        float centerY = Math.max(ceiling + length / 2,
                Math.min((topInset + height - bottomInset) / 2 - (narrow ? 16f : 12f) * density, limit - length / 2));
        float bottom = centerY + length / 2;
        float top = bottom - length;
        if (narrow) {
            // Keep the capsule from moving downward while making the two visible edge gaps equal.
            float firstCenter = bottom + indicatorExtent + circleGap + visibleDiscDiameter / 2;
            controlY = firstCenter + controlSpan / 2;
            firstY = controlY + (ay < by ? -1 : 1) * controlSpan / 2;
            secondY = controlY + (ay < by ? 1 : -1) * controlSpan / 2;
        }
        float contentRight = Math.min(Math.min(a.left, b.left), left) - gap;
        if (left < leftInset || right > width - rightInset || bottom > height - bottomInset
                || length < 140f * density || contentRight - leftInset < 260f * density) return null;
        return new Plan(new FoldMusicLayout.Box(left, top, right, bottom),
                new FoldMusicLayout.Box(left, top - gap - thickness, right, top - gap),
                new FoldMusicLayout.Box(leftInset, topInset, contentRight, height - bottomInset),
                centerX, controlY, controlSpan, firstY - ay, secondY - by);
    }

    static final class VerticalFace {
        final FoldMusicLayout.Box artwork, text, action, secondAction;
        VerticalFace(FoldMusicLayout.Box art, FoldMusicLayout.Box text,
                     FoldMusicLayout.Box action, FoldMusicLayout.Box second) {
            this.artwork = art; this.text = text; this.action = action; this.secondAction = second;
        }
    }

    /** Upright artwork/text/buttons in a tall capsule, with natural text wrapping. */
    static VerticalFace face(float width, float height, float density, boolean second) {
        if (!FoldMusicLayout.finite(width, height, density) || density <= 0 || width < 40 * density
                || height < 140 * density || height < width * 1.6f) return null;
        float gap = 6 * density, edge = 8 * density;
        float icon = Math.min(34 * density, width - 2 * edge);
        float button = Math.min(36 * density, width - 2 * edge);
        float actionTop = height - edge - button;
        float secondTop = second ? actionTop - button - gap : actionTop;
        FoldMusicLayout.Box art = new FoldMusicLayout.Box((width - icon) / 2, edge,
                (width + icon) / 2, edge + icon);
        FoldMusicLayout.Box text = new FoldMusicLayout.Box(edge, art.bottom + gap,
                width - edge, secondTop - gap);
        if (text.height() < 28 * density) return null;
        FoldMusicLayout.Box action = new FoldMusicLayout.Box((width - button) / 2, actionTop,
                (width + button) / 2, actionTop + button);
        FoldMusicLayout.Box other = new FoldMusicLayout.Box(action.left, secondTop,
                action.right, secondTop + button);
        return new VerticalFace(art, text, action, other);
    }
}
