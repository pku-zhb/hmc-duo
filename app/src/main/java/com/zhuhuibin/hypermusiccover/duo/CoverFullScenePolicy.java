package com.zhuhuibin.hypermusiccover.duo;

/** Full-screen artwork is a full-width band, never a card fitted between native controls. */
final class CoverFullScenePolicy {
    static final class Band {
        final float left, top, width, height;

        Band(float left, float top, float width, float height) {
            this.left = left;
            this.top = top;
            this.width = width;
            this.height = height;
        }
    }

    static boolean fullScene(int style, boolean splitArtworkAndLyrics) {
        return style == CoverCardStyle.FULL && !splitArtworkAndLyrics;
    }

    /** The background artwork follows page intent, not foreground card/clock occlusion. */
    static boolean sharpBand(boolean sceneVisible, boolean userCompact) {
        return sceneVisible && !userCompact;
    }

    /** Preserve the source aspect even for panoramas and tall artwork; the viewport clips it. */
    static Band band(int width, int height, int artworkWidth, int artworkHeight, float bias) {
        if (width <= 0 || height <= 0 || artworkWidth <= 0 || artworkHeight <= 0) return null;
        float safeBias = Float.isFinite(bias) ? Math.max(0f, Math.min(1f, bias)) : 0.5f;
        float bandHeight = artworkHeight * (width / (float) artworkWidth);
        return new Band(0f, (height - bandHeight) * safeBias, width, bandHeight);
    }

    /** The original 240-screen-pixel feather, rescaled with the worker's bounded bitmap. */
    static int feather(int preparedWidth, int preparedHeight, int viewportWidth) {
        return Math.max(0, Math.min(preparedHeight / 4,
                Math.round(240f * preparedWidth / Math.max(1, viewportWidth))));
    }

    private CoverFullScenePolicy() {}
}
