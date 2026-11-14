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
        if (nativeHeight <= 0 || percent >= 88 || density <= 0)
            return new Plan(nativeHeight, artwork, gaps);
        int[] floor = {Math.round(4f * density), 0, 0,
                seekBottom < Math.round(8f * density) ? seekBottom : 0};
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
        int headerSaving = Math.min(Math.max(0, artwork - Math.round(40f * density)),
                Math.round(Math.max(0, artwork) * .24f));
        int requested = nativeHeight - (int) Math.ceil(nativeHeight * .75d);
        headerSaving = Math.min(headerSaving, requested);
        int safe = Math.max(0, nativeHeight - (controlHeight - headerSaving) - minimumGaps);
        int reduction = Math.min(requested, Math.min(available + headerSaving, safe));
        headerSaving = Math.min(headerSaving, reduction);
        int gapSaving = reduction - headerSaving;
        // The compact preset spends whitespace below/between rows first. Its remaining
        // budget belongs above the header rather than being split into four tiny margins.
        for (int index : new int[]{1, 2, 3, 0}) {
            int take = Math.min(capacity[index], gapSaving);
            gaps[index] -= take; gapSaving -= take;
        }
        return new Plan(nativeHeight - reduction, artwork - headerSaving, gaps);
    }

    /** Large text can raise the compact preset's safety floor without creating a third mode. */
    static Plan withHeaderFloor(Plan plan, int minimum, int nativeHeight, int nativeArtwork) {
        int artwork = Math.max(plan.artwork, Math.min(nativeArtwork, minimum));
        return artwork == plan.artwork ? plan : new Plan(Math.min(nativeHeight,
                plan.height + artwork - plan.artwork), artwork,
                new int[]{plan.top, plan.action, plan.seekTop, plan.seekBottom});
    }

    /** Budget the two visible AOD rows, including the actual playback glyph's lower edge. */
    static int aodHeight(int nativeHeight, Plan normal, int artwork, int top, int action,
                         int actionHeight, int actionBottom, int padding, float density) {
        if (normal == null || density <= 0f || nativeHeight <= 0 || actionHeight <= 0)
            return nativeHeight;
        int nativeBottom = nativeHeight - padding - top - artwork - action - actionHeight;
        if (nativeBottom < 0 || normal.artwork <= 0 || normal.top < 0 || normal.action < 0)
            return nativeHeight;
        if (normal.artwork == artwork && normal.top == top && normal.action == action)
            return nativeHeight;
        int innerBottom = Math.max(0, actionHeight - actionBottom);
        int bottom = Math.max(0, normal.top - innerBottom);
        return Math.min(nativeHeight,
                padding + normal.top + normal.artwork + normal.action + actionHeight + bottom);
    }

    /** Keep button pixels inside their unchanged row by transferring existing vertical padding. */
    static int actionContentShift(int percent, int paddingBottom, float density) {
        if (percent >= 88 || density <= 0f || paddingBottom <= 0) return 0;
        return Math.min(paddingBottom, Math.round(5f * density));
    }

    /** Match visible inter-row gaps using the seek/time union, without overlapping touch rows. */
    static int seekContentShift(int percent, int rowHeight, int visibleHeight, int actionShift,
                                int paddingTop, float density) {
        if (percent >= 88 || density <= 0f || paddingTop <= 0) return 0;
        int visibleTop = Math.max(0, (rowHeight - visibleHeight) / 2);
        return Math.max(0, Math.min(Math.min(paddingTop, Math.round(6f * density)),
                visibleTop - 2 * actionShift));
    }

    /** One affine remapping of the OEM height timeline; both endpoints stay continuous. */
    static int transitionHeight(int height, int nativeNormal, int nativeAod, int normal, int aod) {
        if (height <= 0 || nativeNormal <= nativeAod || normal <= aod) return height;
        float progress = (height - nativeAod) / (float) (nativeNormal - nativeAod);
        return Math.round(aod + progress * (normal - aod));
    }

    private CompactPlayerMetrics() {}
}
