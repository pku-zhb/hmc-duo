package com.os4.musiccover;

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
}
