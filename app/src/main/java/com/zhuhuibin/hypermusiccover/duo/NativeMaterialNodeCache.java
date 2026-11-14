package com.zhuhuibin.hypermusiccover.duo;

import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;

/** Last successful native writes for verified OEM nodes, independent of scene-profile lifetime. */
final class NativeMaterialNodeCache<N> {
    private static final class State {
        float[] glass;
        int[] blur;
    }

    private final Map<N, State> nodes = new WeakHashMap<>();

    synchronized void track(N node) {
        if (node != null && !nodes.containsKey(node)) nodes.put(node, new State());
    }

    synchronized boolean tracked(N node) { return node != null && nodes.containsKey(node); }

    /** Unknown/rejected writes cannot seed a cache; unchanged known values can be reused. */
    synchronized float[] glass(N node, float[] values, boolean changed) {
        State state = nodes.get(node);
        if (state == null) return null;
        boolean valid = values != null && values.length == 42;
        if (valid) for (float value : values) if (!Float.isFinite(value)) { valid = false; break; }
        if (changed) state.glass = valid ? values.clone() : null;
        return valid && Arrays.equals(state.glass, values) ? state.glass.clone() : null;
    }

    synchronized int[] blur(N node, int small, int big, boolean changed) {
        State state = nodes.get(node);
        if (state == null) return null;
        boolean valid = small >= 0 && big >= 0;
        if (changed) state.blur = valid ? new int[]{small, big} : null;
        return valid && state.blur != null && state.blur[0] == small && state.blur[1] == big
                ? state.blur.clone() : null;
    }
}
