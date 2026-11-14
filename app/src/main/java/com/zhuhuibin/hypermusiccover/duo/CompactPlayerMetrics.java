package com.zhuhuibin.hypermusiccover.duo;

/** Compact the header and whitespace while retaining native playback/seek control dimensions. */
final class CompactPlayerMetrics {
    static final class Plan {
        final int height, artwork, top, action, seekTop, seekBottom;
        Plan(int height, int artwork, int[] gaps) {
            this.height = height; this.artwork = artwork;
            top = gaps[0]; action = gaps[1]; seekTop = gaps[2]; seekBottom = gaps[3];
        }
    }

    static boolean innerLandscape(int width, int height, float density) {
        return density > 0 && width > height && Math.min(width, height) / density >= 600f;
    }

    static Plan plan(int nativeHeight, int percent, int top, int action, int seekTop,
                     int seekBottom, int controlHeight, int artwork, float density) {
        int[] gaps = {top, action, seekTop, seekBottom};
        if (nativeHeight <= 0 || percent >= 100 || density <= 0)
            return new Plan(nativeHeight, artwork, gaps);
        int[] floor = {Math.round(4f * density), 0, 0, Math.round(4f * density)};
        int[] capacity = new int[4];
        int available = 0, minimumGaps = 0;
        for (int i = 0; i < 4; ++i) {
            floor[i] = Math.min(gaps[i], floor[i]);
            capacity[i] = Math.max(0, gaps[i] - floor[i]);
            available += capacity[i];
            minimumGaps += Math.max(0, floor[i]);
        }
        // Unknown/tight variants retain their native rows, rather than shrinking around them.
        if (nativeHeight < controlHeight + minimumGaps)
            return new Plan(nativeHeight, artwork, gaps);
        int bounded = Math.max(75, percent);
        float amount = (100 - bounded) / 25f;
        int headerSaving = Math.min(Math.max(0, artwork - Math.round(40f * density)),
                Math.round(Math.max(0, artwork) * .24f * amount));
        int requested = nativeHeight - (int) Math.ceil(nativeHeight * bounded / 100d);
        headerSaving = Math.min(headerSaving, requested);
        int safe = Math.max(0, nativeHeight - (controlHeight - headerSaving) - minimumGaps);
        int reduction = Math.min(requested, Math.min(available + headerSaving, safe));
        headerSaving = Math.min(headerSaving, reduction);
        int gapSaving = reduction - headerSaving;
        // Prefer the two inter-row gaps; preserve breathing room above and below the header.
        int middle = Math.min(capacity[1] + capacity[2], Math.round(gapSaving * .60f));
        middle = Math.max(middle, gapSaving - capacity[0] - capacity[3]);
        take(gaps, capacity, 1, 2, middle);
        take(gaps, capacity, 0, 3, gapSaving - middle);
        return new Plan(nativeHeight - reduction, artwork - headerSaving, gaps);
    }

    /** Budget only the two visible AOD rows; the hidden seek row owns no residual space. */
    static int aodHeight(int nativeHeight, Plan normal, int artwork, int top, int action,
                         int actionHeight, int padding, float density) {
        if (normal == null || density <= 0f || nativeHeight <= 0 || actionHeight <= 0)
            return nativeHeight;
        int nativeBottom = nativeHeight - padding - top - artwork - action - actionHeight;
        if (nativeBottom < 0 || normal.artwork <= 0 || normal.top < 0 || normal.action < 0)
            return nativeHeight;
        // Derive the existing AOD breathing room instead of subtracting a normal seek row
        // from the OEM's already seek-free 138dp endpoint a second time.
        float edgeRatio = top > 0 ? Math.min(1f, normal.top / (float) top) : 1f;
        int bottomFloor = Math.min(nativeBottom, Math.round(4f * density));
        int bottom = Math.max(bottomFloor, Math.round(nativeBottom * edgeRatio));
        int height = padding + normal.top + normal.artwork + normal.action + actionHeight + bottom;
        return Math.min(nativeHeight, height);
    }

    /** Move seek pixels inside their unchanged touch row without scaling the track or thumb. */
    static int seekContentShift(int percent, int paddingTop, float density) {
        if (percent >= 100 || density <= 0f || paddingTop <= 0) return 0;
        float amount = (100 - Math.max(75, percent)) / 25f;
        return Math.min(paddingTop, Math.round(6f * density * amount));
    }

    private static void take(int[] gaps, int[] capacity, int first, int second, int total) {
        int room = capacity[first] + capacity[second];
        int a = room == 0 ? 0 : Math.min(capacity[first], Math.round((float) total * capacity[first] / room));
        int b = Math.min(capacity[second], total - a);
        a = total - b;
        gaps[first] -= a; gaps[second] -= b;
    }

    /** One affine remapping of the OEM height timeline; both endpoints stay continuous. */
    static int transitionHeight(int height, int nativeNormal, int nativeAod, int normal, int aod) {
        if (height <= 0 || nativeNormal <= nativeAod || normal <= aod) return height;
        float progress = (height - nativeAod) / (float) (nativeNormal - nativeAod);
        return Math.round(aod + progress * (normal - aod));
    }

    private CompactPlayerMetrics() {}
}
