package com.zhuhuibin.hypermusiccover.duo;

/** Geometry shared by the vertical island and the remaining native lock-screen content. */
final class RightRailLayout {
    private RightRailLayout() {}
    static final class Plan {
        final FoldMusicLayout.Box pill, companion, content, strip;
        final float controlsX, controlsY, controlsSpan, firstOffsetY, secondOffsetY;
        Plan(FoldMusicLayout.Box pill, FoldMusicLayout.Box companion, FoldMusicLayout.Box content, FoldMusicLayout.Box strip,
             float controlsX, float controlsY, float controlsSpan, float firstOffsetY, float secondOffsetY) {
            this.pill = pill; this.companion = companion; this.content = content; this.strip = strip;
            this.controlsX = controlsX; this.controlsY = controlsY; this.controlsSpan = controlsSpan;
            this.firstOffsetY = firstOffsetY; this.secondOffsetY = secondOffsetY;
        }
    }

    // Duo owns these dp values. No MiDuo view, margin or measured width is an input.
    static final float RIGHT_EDGE_DP = 16f;
    static final float CONTENT_RESERVE_DP = 75f;
    static final float TOP_CONTROL_RESERVE_DP = 72f;
    static final float CAPSULE_LENGTH_DP = 221f;

    static Plan plan(float width, float height, float density, float leftInset, float topInset,
                     float rightInset, float bottomInset, float indicatorExtent) {
        if (!FoldMusicLayout.finite(width, height, density, leftInset, topInset, rightInset,
                bottomInset, indicatorExtent) || density <= 0 || width <= 0 || height <= 0
                || leftInset < 0 || topInset < 0 || rightInset < 0 || bottomInset < 0
                || indicatorExtent < 0) return null;
        boolean narrow = width / density < 600f || width > height * 1.05f;
        float gap = 12f * density;
        float thickness = (narrow ? 46f : 48f) * density;
        float right = width - rightInset - RIGHT_EDGE_DP * density;
        float left = right - thickness;
        float contentRight = width - rightInset - CONTENT_RESERVE_DP * density;
        float companionTop = topInset + TOP_CONTROL_RESERVE_DP * density;
        RailShortcutLayout shortcuts = RailShortcutLayout.plan(width, height, density, rightInset, bottomInset);
        if (shortcuts == null) return null;
        float limit = shortcuts.capsuleBottom - indicatorExtent;
        float length = Math.min(CAPSULE_LENGTH_DP * density, limit - companionTop - thickness - gap);
        float ceiling = topInset + gap + thickness + gap;
        float center = Math.max(ceiling + length / 2,
                Math.min((topInset + height - bottomInset) / 2 - (narrow ? 16f : 12f) * density,
                    limit - length / 2));
        float bottom = width / density < 600f && height >= width ? limit : center + length / 2;
        float top = bottom - length;
        if (left < leftInset || length < 140f * density || contentRight - leftInset < 260f * density)
            return null;
        float centerX = (left + right) / 2;
        // Only the X reference is used by the native shortcut AOD inset. The slot itself
        // follows the keyguard root, never a measured or animated shortcut pair.
        float controlSpan = shortcuts.camera.top - shortcuts.torch.top;
        float controlY = (shortcuts.camera.top + shortcuts.torch.bottom) / 2f;
        return new Plan(new FoldMusicLayout.Box(left, top, right, bottom),
                new FoldMusicLayout.Box(left, top - gap - thickness, right, top - gap),
                new FoldMusicLayout.Box(leftInset, topInset, contentRight, height - bottomInset),
                new FoldMusicLayout.Box(left, companionTop, right, bottom),
                centerX, controlY, controlSpan, 0f, 0f);
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
