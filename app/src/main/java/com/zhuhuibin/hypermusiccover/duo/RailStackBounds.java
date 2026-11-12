package com.zhuhuibin.hypermusiccover.duo;

import android.view.View;
import java.lang.reflect.Method;

/** Own the stack's horizontal measurement/frame without rewriting the OEM's LayoutParams. */
final class RailStackBounds {
    private RailStackBounds() {}
    private static View stack;
    private static RailContentFrame frame;
    private static float originalX;
    private static boolean installed;
    private static long measureOverrides, frameOverrides, translationOverrides;
    private static int nativeMeasureWidth, nativeLeft, nativeWidth;

    static void install(ClassLoader loader) {
        try {
            Class<?> type = Xp.findClass(
                    "com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout", loader);
            Method measure = Xp.findMethodExact(type, "onMeasure", int.class, int.class);
            Method setFrame = Xp.findMethodExact(type, "setFrame", int.class, int.class, int.class, int.class);
            Method setX = Xp.findMethodExact(type, "setTranslationX", float.class);
            Xp.hook(measure, chain -> {
                if (!owns(chain.getThisObject())) return chain.proceed();
                int spec = (Integer) chain.getArg(0);
                nativeMeasureWidth = View.MeasureSpec.getSize(spec);
                int fixed = View.MeasureSpec.makeMeasureSpec(frame.width, View.MeasureSpec.EXACTLY);
                if (spec == fixed) return chain.proceed();
                measureOverrides++;
                return chain.proceed(new Object[]{fixed, chain.getArg(1)});
            });
            Xp.hook(setFrame, chain -> {
                if (!owns(chain.getThisObject())) return chain.proceed();
                int left = (Integer) chain.getArg(0), right = (Integer) chain.getArg(2);
                nativeLeft = left; nativeWidth = right - left;
                if (frame.matches(left, right - left)) return chain.proceed();
                frameOverrides++;
                // Leave vertical stacking, scrolling and animation entirely native.
                return chain.proceed(new Object[]{frame.left, chain.getArg(1), frame.right(), chain.getArg(3)});
            });
            Xp.hook(setX, chain -> {
                if (!owns(chain.getThisObject()) || ((Float) chain.getArg(0)) == 0f) return chain.proceed();
                translationOverrides++;
                return chain.proceed(new Object[]{0f});
            });
            installed = true;
        } catch (Throwable error) {
            // Partial hooks stay inert: owns() requires the complete installation.
            installed = false;
            Xp.w(Main.TAG + "fixed stack bounds unavailable: " + error);
        }
    }

    private static boolean owns(Object candidate) {
        return installed && candidate == stack && frame != null && Main.sRightRail
                && Main.sReflowLockContent && Main.keyguardLocked() && !Main.bouncerShown();
    }

    static boolean bind(View next, View root, FoldMusicLayout.Box content) {
        if (!installed || next == null || !next.isAttachedToWindow()) { release(); return false; }
        // Layout coordinates only: AOD scaling belongs to the ancestors, not the width baseline.
        int parentLeft = 0;
        View parent = next.getParent() instanceof View ? (View) next.getParent() : null;
        while (parent != null && parent != root) {
            parentLeft += parent.getLeft();
            parent = parent.getParent() instanceof View ? (View) parent.getParent() : null;
        }
        if (parent != root) { release(); return false; }
        RailContentFrame target = RailContentFrame.in(content,
                root.getResources().getDisplayMetrics().density, parentLeft);
        if (target == null) { release(); return false; }
        if (stack != next) {
            release();
            originalX = next.getTranslationX();
            stack = next;
        }
        boolean changed = frame == null || !frame.matches(target.left, target.width);
        frame = target;
        if (next.getTranslationX() != 0f) next.setTranslationX(0f);
        if (changed) next.requestLayout();
        return true;
    }

    static void release() {
        View previous = stack;
        stack = null; frame = null;
        if (previous == null) return;
        if (previous.getTranslationX() == 0f) previous.setTranslationX(originalX);
        // Native LayoutParams were never touched, so a fresh native pass restores the full shade.
        previous.requestLayout();
    }

    static String describe() {
        return "installed=" + installed + " target=" + (frame == null ? "none" : frame.left + ".." + frame.right())
                + " native=" + nativeLeft + "/" + nativeWidth + " measure=" + nativeMeasureWidth
                + " overrides=" + measureOverrides + "/" + frameOverrides + "/" + translationOverrides;
    }
}
