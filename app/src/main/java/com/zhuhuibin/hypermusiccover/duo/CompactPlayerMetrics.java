package com.zhuhuibin.hypermusiccover.duo;

/** Compress only available vertical gaps; native artwork, controls and touch areas retain size. */
final class CompactPlayerMetrics {
    static final class Plan {
        final int height, top, action, seekTop, seekBottom;
        Plan(int height, int[] gaps) {
            this.height = height;
            top = gaps[0]; action = gaps[1]; seekTop = gaps[2]; seekBottom = gaps[3];
        }
    }

    static boolean innerLandscape(int width, int height, float density) {
        return density > 0 && width > height && Math.min(width, height) / density >= 600f;
    }

    static Plan plan(int nativeHeight, int percent, int top, int action, int seekTop,
                     int seekBottom, int controlHeight, float density) {
        int[] gaps = {top, action, seekTop, seekBottom};
        if (nativeHeight <= 0 || percent >= 100 || density <= 0) return new Plan(nativeHeight, gaps);
        int[] floor = {Math.round(4f * density), Math.round(2f * density),
                Math.round(2f * density), Math.round(4f * density)};
        int[] capacity = new int[4];
        int available = 0, minimumGaps = 0;
        for (int i = 0; i < 4; ++i) {
            // Do not enlarge an already tight OEM variant or turn a negative margin positive.
            floor[i] = Math.min(gaps[i], floor[i]);
            capacity[i] = Math.max(0, gaps[i] - floor[i]);
            available += capacity[i];
            minimumGaps += Math.max(0, floor[i]);
        }
        int requested = nativeHeight - (int) Math.ceil(nativeHeight * Math.max(85, percent) / 100d);
        int safe = Math.max(0, nativeHeight - Math.max(0, controlHeight) - minimumGaps);
        int reduction = Math.min(requested, Math.min(available, safe));
        int remaining = reduction, remainingCapacity = available;
        for (int i = 0; i < 4; ++i) {
            int take = remainingCapacity == 0 ? 0
                    : Math.min(capacity[i], Math.round((float) remaining * capacity[i] / remainingCapacity));
            gaps[i] -= take;
            remaining -= take;
            remainingCapacity -= capacity[i];
        }
        return new Plan(nativeHeight - reduction, gaps);
    }

    private CompactPlayerMetrics() {}
}
