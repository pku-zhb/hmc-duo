package com.zhuhuibin.hypermusiccover.duo;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.view.View;

/** Shared native content affine; layout is retained in its local frame, never physical pixels. */
final class NativeContentMotion {
    private static final Matrix sourceMatrix = new Matrix(), targetInverse = new Matrix();
    private static final FoldMusicLayout.Box UNIT = new FoldMusicLayout.Box(0, 0, 100, 100);
    private static final RectF measuredUnit = new RectF();
    private static final String[] SOURCE_IDS = {"shared_notification_container", "keyguard_root_view"};
    private static java.lang.ref.WeakReference<View> cachedWindow = new java.lang.ref.WeakReference<>(null);
    private static java.lang.ref.WeakReference<View> cachedOwner = new java.lang.ref.WeakReference<>(null);

    static View owner(View window) {
        if (window == null) return null;
        View root = window.getRootView();
        View cached = cachedOwner.get();
        if (cachedWindow.get() == root && cached != null && cached.isAttachedToWindow()
                && cached.getWidth() > 0) return cached;
        // This is also LyricView's native swipe/AOD source. On current SystemUI the player
        // is a descendant of it, but need not be a descendant of keyguard_root_view.
        for (String name : SOURCE_IDS) {
            int id = root.getResources().getIdentifier(name, "id", "com.android.systemui");
            View source = id == 0 ? null : root.findViewById(id);
            if (source != null && source.isAttachedToWindow() && source.getWidth() > 0) {
                cachedWindow = new java.lang.ref.WeakReference<>(root);
                cachedOwner = new java.lang.ref.WeakReference<>(source);
                return source;
            }
        }
        return root;
    }

    /** Window plans use natural layout pixels, before the native AOD/swipe property affine. */
    static FoldMusicLayout.Box logicalPlan(View window, View owner, FoldMusicLayout.Box plan) {
        if (window == null || owner == null || plan == null) return null;
        View root = window.getRootView();
        float x = 0f, y = 0f;
        View node = owner;
        while (node != root) {
            if (!(node.getParent() instanceof View)) return null;
            View parent = (View) node.getParent();
            x += node.getLeft() - parent.getScrollX();
            y += node.getTop() - parent.getScrollY();
            node = parent;
        }
        return ContentMotionSnapshot.logicalPlan(plan, x, y);
    }

    /** Current native motion and natural layout must be separated before a new plan is fitted. */
    static ContentMotionMeasurement measurement(View window, View owner) {
        FoldMusicLayout.Box naturalUnit = logicalPlan(window, owner, UNIT);
        if (naturalUnit == null || !map(owner, window, UNIT, measuredUnit)) return null;
        return ContentMotionMeasurement.from(box(measuredUnit), naturalUnit);
    }

    static boolean map(View source, View target, FoldMusicLayout.Box box, RectF result) {
        if (source == null || target == null || box == null || !source.isAttachedToWindow()
                || !target.isAttachedToWindow()) return false;
        sourceMatrix.reset(); source.transformMatrixToGlobal(sourceMatrix);
        targetInverse.reset(); target.transformMatrixToGlobal(targetInverse);
        if (!targetInverse.invert(targetInverse)) return false;
        targetInverse.preConcat(sourceMatrix);
        result.set(box.left, box.top, box.right, box.bottom);
        targetInverse.mapRect(result);
        return FoldMusicLayout.finite(result.left, result.top, result.right, result.bottom)
                && result.width() > 0f && result.height() > 0f;
    }

    static FoldMusicLayout.Box box(RectF rect) {
        return new FoldMusicLayout.Box(rect.left, rect.top, rect.right, rect.bottom);
    }

    private NativeContentMotion() {}
}
