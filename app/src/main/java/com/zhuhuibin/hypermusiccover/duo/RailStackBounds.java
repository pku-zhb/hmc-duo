package com.zhuhuibin.hypermusiccover.duo;

import android.graphics.Canvas;
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
    private static RailNotificationEdgeRenderer edge;
    private static boolean edgeInstalled;

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
        installEdge(loader);
    }

    private static void installEdge(ClassLoader loader) {
        if (edgeInstalled) return;
        java.util.concurrent.atomic.AtomicBoolean active = new java.util.concurrent.atomic.AtomicBoolean();
        java.util.List<io.github.libxposed.api.XposedInterface.HookHandle> hooks = new java.util.ArrayList<>();
        try {
            Class<?> type = Xp.findClass(
                    "com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout", loader);
            Class<?> row = Xp.findClass(
                    "com.android.systemui.statusbar.notification.row.ExpandableNotificationRow", loader);
            Class<?> media = Xp.findClass(
                    "com.android.systemui.statusbar.notification.mediacontrol.MiuiMediaHeaderView", loader);
            Class<?> expandable = Xp.findClass(
                    "com.android.systemui.statusbar.notification.row.ExpandableView", loader);
            if (!expandable.isAssignableFrom(row) || !expandable.isAssignableFrom(media))
                throw new NoSuchMethodException("native card base mismatch");
            Method draw = type.getDeclaredMethod("dispatchDraw", Canvas.class);
            Method effect = View.class.getMethod("setRenderEffect", android.graphics.RenderEffect.class);
            java.lang.reflect.Field renderNode = View.class.getDeclaredField("mRenderNode");
            renderNode.setAccessible(true);
            Method commit = android.graphics.RenderNode.class.getDeclaredMethod("setRenderEffect", android.graphics.RenderEffect.class);
            Method getter = null;
            try {
                getter = View.class.getMethod("getRenderEffect");
                if (getter.getReturnType() != android.graphics.RenderEffect.class) getter = null;
            } catch (NoSuchMethodException ignored) { }
            RailNotificationEdgeRenderer renderer = new RailNotificationEdgeRenderer(getter, renderNode, effect);
            // This exact framework bridge contains only nSetRenderEffect. Deoptimize its
            // one known View caller so inlining cannot bypass the commit observer.
            if (!Xp.api().deoptimize(effect)) throw new IllegalStateException("card effect commit caller cannot be deoptimized");
            hooks.add(Xp.api().hook(commit)
                    .setPriority(io.github.libxposed.api.XposedInterface.PRIORITY_LOWEST)
                    .setExceptionMode(io.github.libxposed.api.XposedInterface.ExceptionMode.PASSTHROUGH)
                    .intercept(chain -> !active.get() ? chain.proceed()
                            : renderer.nativeWrite(chain.getThisObject(), (android.graphics.RenderEffect) chain.getArg(0),
                                    value -> chain.proceed(new Object[]{value}))));
            // View entry only scopes identity/intent. Actual committed values come from the
            // RenderNode bridge after any downstream View reentry or argument replacement.
            hooks.add(edgeHook(effect, chain -> {
                View view = (View) chain.getThisObject();
                if (!active.get() || !expandable.isInstance(view)) return chain.proceed();
                return renderer.viewWrite(view, value -> chain.proceed(new Object[]{value}),
                        (android.graphics.RenderEffect) chain.getArg(0));
            }));
            if (getter != null) hooks.add(edgeHook(getter, chain -> {
                Object result = chain.proceed();
                View view = (View) chain.getThisObject();
                // External effects must compose with their native baseline, rather than
                // accidentally capturing our temporary mask as their restore snapshot.
                return active.get() && expandable.isInstance(view) ? renderer.nativeRead(view, result) : result;
            }));
            for (Class<?> card : new Class<?>[]{expandable, row, media}) {
                for (java.lang.reflect.Constructor<?> constructor : card.getDeclaredConstructors())
                    hooks.add(edgeHook(constructor, chain -> {
                        Object result = chain.proceed();
                        if (active.get()) renderer.born((View) chain.getThisObject());
                        return result;
                    }));
            }
            // Media owns a separate translation implementation; the previous ENR-only hook
            // could never see its swipe, even though both cards live in this same stack.
            for (Class<?> card : new Class<?>[]{row, media}) {
                Method translation = card.getDeclaredMethod("setTranslation", float.class);
                hooks.add(edgeHook(translation, chain -> {
                    Object result = chain.proceed();
                    if (active.get() && owns(stack)) renderer.translated((View) chain.getThisObject(), (Float) chain.getArg(0));
                    return result;
                }));
            }
            hooks.add(edgeHook(draw, chain -> {
                if (active.get() && owns(chain.getThisObject())) {
                    View view = (View) chain.getThisObject();
                    if (view.getWidth() == frame.width && view.getHeight() > 0) {
                        renderer.configure(view, view.getWidth(), view.getHeight(),
                                view.getResources().getDisplayMetrics().density);
                        renderer.beforeDraw((Canvas) chain.getArg(0));
                    }
                }
                // No offscreen stack copy: the original child nodes, shelf clipping,
                // native blur node and cross-child Z ordering execute without substitution.
                return chain.proceed();
            }));
            edge = renderer;
            edgeInstalled = true;
            active.set(true);
        } catch (Throwable error) {
            active.set(false);
            edgeInstalled = false;
            for (int i = hooks.size() - 1; i >= 0; i--) try { hooks.get(i).unhook(); } catch (Throwable ignored) { }
            Xp.w(Main.TAG + "card alpha edge contract unavailable: " + error);
        }
    }

    private static io.github.libxposed.api.XposedInterface.HookHandle edgeHook(
            java.lang.reflect.Executable method, io.github.libxposed.api.XposedInterface.Hooker hook) {
        return Xp.api().hook(method)
                .setExceptionMode(io.github.libxposed.api.XposedInterface.ExceptionMode.PASSTHROUGH).intercept(hook);
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
        RailContentFrame target = RailContentFrame.inWindow(content,
                root.getResources().getDisplayMetrics().density, parentLeft,
                root.getWidth(), root.getHeight(), FoldMusicRuntime.combinedRequested() && Main.coverModeOn());
        if (target == null) { release(); return false; }
        if (stack != next) {
            release();
            originalX = next.getTranslationX();
            stack = next;

        }
        boolean changed = frame == null || !frame.matches(target.left, target.width);
        frame = target;
        if (edgeInstalled && edge != null) edge.configure(next, target.width, next.getHeight(),
                next.getResources().getDisplayMetrics().density);
        if (next.getTranslationX() != 0f) next.setTranslationX(0f);
        if (changed) next.requestLayout();
        return true;
    }

    static void release() {
        View previous = stack;
        stack = null; frame = null;
        if (edge != null) edge.release();
        if (previous == null) return;
        if (previous.getTranslationX() == 0f) previous.setTranslationX(originalX);
        // Native LayoutParams were never touched, so a fresh native pass restores the full shade.
        previous.requestLayout();
    }

    static String describe() {
        return "installed=" + installed + " target=" + (frame == null ? "none" : frame.left + ".." + frame.right())
                + " native=" + nativeLeft + "/" + nativeWidth + " measure=" + nativeMeasureWidth
                + " overrides=" + measureOverrides + "/" + frameOverrides + "/" + translationOverrides
                + " softEdge=" + edgeInstalled;
    }
}
