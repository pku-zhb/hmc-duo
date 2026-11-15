package com.zhuhuibin.hypermusiccover.duo;

/** Immutable logical content bounds plus the window/affine owner that can present them. */
final class ContentMotionSnapshot {
    final Object window, owner;
    final long revision;
    final FoldMusicLayout.Box bounds;

    ContentMotionSnapshot(Object window, long revision, Object owner, FoldMusicLayout.Box bounds) {
        this.window = window; this.revision = revision; this.owner = owner; this.bounds = bounds;
    }

    boolean current(Object window, long revision, Object owner) {
        return this.window != null && this.window == window && this.revision == revision
                && this.owner != null && this.owner == owner && bounds != null;
    }

    static FoldMusicLayout.Box logicalPlan(FoldMusicLayout.Box plan, float layoutX, float layoutY) {
        if (plan == null || !FoldMusicLayout.finite(layoutX, layoutY)) return null;
        return new FoldMusicLayout.Box(plan.left - layoutX, plan.top - layoutY,
                plan.right - layoutX, plan.bottom - layoutY);
    }
}
