package com.zhuhuibin.hypermusiccover.duo;

/** Immutable final vendor-glass values, scoped to the theme and display state that produced them. */
final class GlassProfile {
    private final float[] values;
    final int nightMode;
    final boolean awake;
    private GlassProfile(float[] values, int nightMode, boolean awake) {
        this.values = values.clone(); this.nightMode = nightMode; this.awake = awake;
    }
    static GlassProfile capture(float[] values, int nightMode, boolean awake) {
        if (values == null || values.length != 42) return null;
        for (float v : values) if (!Float.isFinite(v)) return null;
        return new GlassProfile(values, nightMode, awake);
    }
    float[] forScene(int nightMode, boolean awake) {
        return this.nightMode == nightMode && this.awake == awake ? values.clone() : null;
    }
    boolean same(GlassProfile other) {
        return other != null && nightMode == other.nightMode && awake == other.awake
                && java.util.Arrays.equals(values, other.values);
    }

    static final class Blur {
        final int small, big, nightMode;
        final boolean awake;
        private Blur(int small, int big, int nightMode, boolean awake) {
            this.small = small; this.big = big; this.nightMode = nightMode; this.awake = awake;
        }
        static Blur capture(int small, int big, int nightMode, boolean awake) {
            return small < 0 || big < 0 ? null : new Blur(small, big, nightMode, awake);
        }
        int[] forScene(int nightMode, boolean awake) {
            return this.nightMode == nightMode && this.awake == awake ? new int[]{small, big} : null;
        }
        boolean same(Blur other) {
            return other != null && small == other.small && big == other.big
                    && nightMode == other.nightMode && awake == other.awake;
        }
    }

    /** Values read from the View's committed background state, not from a caller's arguments. */
    static final class Background {
        final int radius, nightMode;
        final float scale;
        final boolean awake;
        private final int[] colors, modes;

        private Background(int radius, float scale, int[] colors, int[] modes, int nightMode, boolean awake) {
            this.radius = radius; this.scale = scale; this.colors = colors.clone(); this.modes = modes.clone();
            this.nightMode = nightMode; this.awake = awake;
        }

        static Background capture(int radius, float scale, int[] colors, int[] modes, int nightMode, boolean awake) {
            if (radius < 0 || !Float.isFinite(scale) || scale < 0f || colors == null || modes == null ||
                    colors.length != modes.length) return null;
            return new Background(radius, scale, colors, modes, nightMode, awake);
        }

        boolean forScene(int nightMode, boolean awake) { return this.nightMode == nightMode && this.awake == awake; }
        int[] colors() { return colors.clone(); }
        int[] modes() { return modes.clone(); }
        boolean same(Background other) {
            return other != null && radius == other.radius && Float.compare(scale, other.scale) == 0 &&
                    nightMode == other.nightMode && awake == other.awake &&
                    java.util.Arrays.equals(colors, other.colors) && java.util.Arrays.equals(modes, other.modes);
        }
    }
}
