package com.zhuhuibin.hypermusiccover.duo;

/** Every input that can change a composition, including paused tracks and window replacements. */
final class ArtworkCompositionKey {
    final Object window;
    final long revision, source;
    final int width, height, mode, blur;
    final boolean cardComposition;
    final float bias;

    ArtworkCompositionKey(Object window, long revision, int width, int height, int mode,
                          boolean cardComposition, long source, int blur, float bias) {
        this.window = window; this.revision = revision;
        this.width = width; this.height = height; this.mode = mode;
        this.cardComposition = cardComposition; this.source = source;
        this.blur = blur; this.bias = bias;
    }

    boolean valid() { return window != null && width > 0 && height > 0 && source >= 0; }

    boolean same(ArtworkCompositionKey other) {
        return other != null && source == other.source
                && sameGeometry(other.window, other.revision, other.width, other.height,
                        other.mode, other.cardComposition, other.blur, other.bias);
    }

    boolean sameGeometry(Object window, long revision, int width, int height, int mode,
                         boolean cardComposition, int blur, float bias) {
        return this.window == window && this.revision == revision && this.width == width
                && this.height == height && this.mode == mode
                && this.cardComposition == cardComposition && this.blur == blur
                && Float.compare(this.bias, bias) == 0;
    }
}
