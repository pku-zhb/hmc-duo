package com.zhuhuibin.hypermusiccover.duo;

import java.util.List;

/** A same-song reflow changes row metrics, not the playback or depth animation state. */
final class LyricLayoutContinuity {
    private LyricLayoutContinuity() {}

    static boolean sameTimeline(List<LyricLine> before, List<LyricLine> after) {
        if (before.isEmpty() || before.size() != after.size()) return false;
        for (int i = 0; i < before.size(); i++) {
            LyricLine a = before.get(i), b = after.get(i);
            if (a.start != b.start || a.end != b.end || a.opposite != b.opposite
                    || !a.text.equals(b.text)) return false;
        }
        return true;
    }

    /** Keep each currently drawn row at its old y, then let the existing spring carry it home. */
    static void rebase(float[] before, float[] after, float[] scroll, float[] aim,
                       float[] nextAim, float target) {
        for (int i = 0; i < after.length; i++) {
            float delta = after[i] - before[i];
            scroll[i] += delta;
            aim[i] += delta;
            nextAim[i] = target;
        }
    }
}
