package com.zhuhuibin.hypermusiccover.duo;

/** One fixed coordinate contract for native shortcut wrappers and the capsule above them. */
final class RailShortcutLayout {
    static final float DIAMETER_DP = 48f, GAP_DP = 6.5f, BOTTOM_DP = 18.5f;
    static final float CAPSULE_GAP_DP = 12.25f;
    final FoldMusicLayout.Box torch, camera;
    final float capsuleBottom;

    private RailShortcutLayout(FoldMusicLayout.Box torch, FoldMusicLayout.Box camera, float bottom) {
        this.torch = torch; this.camera = camera; this.capsuleBottom = bottom;
    }

    static RailShortcutLayout plan(float width, float height, float density, float rightInset, float bottomInset) {
        if (!FoldMusicLayout.finite(width, height, density, rightInset, bottomInset)
                || density <= 0 || width <= 0 || height <= 0 || rightInset < 0 || bottomInset < 0) return null;
        float railWidth = (width / density < 600f || width > height * 1.05f ? 46f : 48f) * density;
        float centerX = width - rightInset - RightRailLayout.RIGHT_EDGE_DP * density - railWidth / 2f;
        float size = DIAMETER_DP * density;
        float left = centerX - size / 2f;
        float bottom = height - Math.max(bottomInset, BOTTOM_DP * density);
        float cameraTop = bottom - size;
        float torchTop = cameraTop - size - GAP_DP * density;
        if (left < 0 || torchTop < 0) return null;
        return new RailShortcutLayout(new FoldMusicLayout.Box(left, torchTop, left + size, torchTop + size),
                new FoldMusicLayout.Box(left, cameraTop, left + size, bottom),
                torchTop - CAPSULE_GAP_DP * density);
    }
}
