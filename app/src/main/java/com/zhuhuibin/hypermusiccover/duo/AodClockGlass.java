package com.zhuhuibin.hypermusiccover.duo;

/** Held glyph fields; only a fill change needs the clock's native material update. */
final class AodClockGlass {
    static final int COLOUR = 1, FILL = 2;

    private AodClockGlass() {}

    static int hold(float[] glass, float red, float green, float blue, float fill,
                    boolean capturedFill) {
        int changed = 0;
        if (glass[11] != red || glass[12] != green || glass[13] != blue) {
            glass[11] = red;
            glass[12] = green;
            glass[13] = blue;
            changed |= COLOUR;
        }
        // A captured cover keeps its exact fill. The plain AOD only raises transparent ink.
        if (capturedFill ? glass[36] != fill : glass[36] < 0.999f) {
            glass[36] = fill;
            changed |= FILL;
        }
        return changed;
    }
}
