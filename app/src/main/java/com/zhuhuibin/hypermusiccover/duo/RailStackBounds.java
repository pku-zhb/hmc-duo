package com.zhuhuibin.hypermusiccover.duo;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.view.View;
import java.lang.reflect.Method;

/** Own the stack's horizontal measurement/frame without rewriting the OEM's LayoutParams. */
final class RailStackBounds {
    private RailStackBounds() {}
    private static View stack;
    private static RailContentFrame frame;
    private static NativeTranslationLease xLease;
    private static float columnX = Float.NaN;
    private static boolean writingX;
    private static final Matrix columnMatrix = new Matrix(), columnInverse = new Matrix();
    private static final RectF columnBox = new RectF();
    private static final float[] columnUnit = new float[4];
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
                if (!ownsStack(chain.getThisObject())) return chain.proceed();
                int spec = (Integer) chain.getArg(0);
                nativeMeasureWidth = View.MeasureSpec.getSize(spec);
                if (frame == null) return chain.proceed();
                int fixed = View.MeasureSpec.makeMeasureSpec(frame.width, View.MeasureSpec.EXACTLY);
                if (spec == fixed) return chain.proceed();
                measureOverrides++;
                return chain.proceed(new Object[]{fixed, chain.getArg(1)});
            });
            Xp.hook(setFrame, chain -> {
                if (!ownsStack(chain.getThisObject())) return chain.proceed();
                int left = (Integer) chain.getArg(0), right = (Integer) chain.getArg(2);
                nativeLeft = left; nativeWidth = right - left;
                if (frame == null) return chain.proceed();
                if (frame.matches(left, right - left)) return chain.proceed();
                frameOverrides++;
                // Leave vertical stacking, scrolling and animation entirely native.
                return chain.proceed(new Object[]{frame.left, chain.getArg(1), frame.right(), chain.getArg(3)});
            });
            Xp.hook(setX, chain -> {
                if (writingX || chain.getThisObject() != stack || xLease == null) return chain.proceed();
                float requested = (Float) chain.getArg(0);
                // Observe native writes even as keyguard eligibility ends, including equal values.
                xLease.nativeWrite(requested);
                if (!ownsStack(chain.getThisObject()) || frame == null && !Float.isFinite(columnX))
                    return chain.proceed();
                float desired = frame != null ? 0f : columnX;
                if (requested != desired) translationOverrides++;
                Object result = chain.proceed(new Object[]{desired});
                if (stack != null && stack.getTranslationX() == desired) xLease.wrote(desired);
                return result;
            });
            installed = true;
        } catch (Throwable error) {
            // Partial hooks stay inert: ownership requires the complete installation.
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
                    if (active.get() && ownsStack(stack)) renderer.translated((View) chain.getThisObject(), (Float) chain.getArg(0));
                    return result;
                }));
            }
            hooks.add(edgeHook(draw, chain -> {
                if (active.get() && ownsStack(chain.getThisObject())) {
                    View view = (View) chain.getThisObject();
                    if (view.getWidth() > 0 && (frame == null || view.getWidth() == frame.width) && view.getHeight() > 0) {
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

    private static boolean ownsStack(Object candidate) {
        return installed && candidate != null && candidate == stack && Main.sRightRail
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
        float density = root.getResources().getDisplayMetrics().density;
        boolean fixedBounds = RailContentFrame.ownsNativeBounds(root.getWidth(), root.getHeight(), density);
        boolean combined = !fixedBounds && FoldMusicRuntime.combinedRequested() && Main.coverModeOn();
        FoldMusicLayout.Box combinedPlayer = combined ? FoldMusicRuntime.combinedPlayer(root, content) : null;
        RailContentFrame target = combinedPlayer != null
                ? RailContentFrame.aroundCard(combinedPlayer, parentLeft, 0f, 0f)
                : RailContentFrame.inWindow(content, density, parentLeft,
                    root.getWidth(), root.getHeight(), combined);
        if (fixedBounds && target == null) { release(); return false; }
        if (stack != next) {
            release();
            xLease = new NativeTranslationLease(next.getTranslationX());
            stack = next;

        }
        boolean wasFixed = frame != null;
        boolean hadX = wasFixed || Float.isFinite(columnX);
        boolean changed = wasFixed != (target != null) || target != null && !target.matches(frame.left, frame.width);
        frame = target;
        float moved = target == null ? centeredColumnX(next, root, content) : Float.NaN;
        if (target == null && !Float.isFinite(moved) && !wasFixed) moved = columnX;
        columnX = moved;
        if (edgeInstalled && edge != null) edge.configure(next, target == null ? next.getWidth() : target.width, next.getHeight(),
                next.getResources().getDisplayMetrics().density);
        if (target != null) writeX(next, 0f, true);
        else if (Float.isFinite(columnX)) writeX(next, columnX, true);
        else if (hadX && xLease != null) {
            writeX(next, xLease.restore(next.getTranslationX()), false);
            xLease.release();
        }
        if (changed) next.requestLayout();
        return target != null || Float.isFinite(columnX);
    }

    /** Keep native-width single pages on the same rail-safe center as artwork and the clock. */
    private static float centeredColumnX(View next, View root, FoldMusicLayout.Box content) {
        int id = next.getResources().getIdentifier("mi_media_controls", "id", "com.android.systemui");
        View player = id == 0 ? null : next.findViewById(id);
        if (player == null && next instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) next;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child.getVisibility() != View.VISIBLE || child.getWidth() <= 0) continue;
                boolean row = false;
                for (Class<?> type = child.getClass(); type != null; type = type.getSuperclass()) {
                    if (type.getName().equals("com.android.systemui.statusbar.notification.row.ExpandableNotificationRow")) {
                        row = true; break;
                    }
                }
                if (row) { player = child; break; }
            }
        }
        if (player == null || !player.isAttachedToWindow() || player.getWidth() <= 0
                || !(next.getParent() instanceof View)) return Float.NaN;
        columnMatrix.reset(); player.transformMatrixToGlobal(columnMatrix);
        columnInverse.reset(); root.transformMatrixToGlobal(columnInverse);
        if (!columnInverse.invert(columnInverse)) return Float.NaN;
        columnMatrix.postConcat(columnInverse);
        columnBox.set(0, 0, player.getWidth(), player.getHeight());
        columnMatrix.mapRect(columnBox);
        columnMatrix.reset(); ((View) next.getParent()).transformMatrixToGlobal(columnMatrix);
        columnMatrix.postConcat(columnInverse);
        columnUnit[0] = columnUnit[1] = columnUnit[3] = 0f; columnUnit[2] = 1f;
        columnMatrix.mapPoints(columnUnit);
        float desiredCenter = (content.left + content.right) / 2f;
        return RailContentFrame.columnTranslation(next.getTranslationX(), columnBox.centerX(), desiredCenter,
                columnUnit[2] - columnUnit[0]);
    }

    private static void writeX(View view, float value, boolean own) {
        if (own ? Math.abs(view.getTranslationX() - value) < .25f : view.getTranslationX() == value) return;
        writingX = true;
        try {
            view.setTranslationX(value);
            if (own && xLease != null && view.getTranslationX() == value) xLease.wrote(value);
        } finally { writingX = false; }
    }

    static void release() {
        View previous = stack;
        NativeTranslationLease lease = xLease;
        stack = null; frame = null; columnX = Float.NaN; xLease = null;
        if (edge != null) edge.release();
        if (previous == null) return;
        if (lease != null) writeX(previous, lease.restore(previous.getTranslationX()), false);
        // Native LayoutParams were never touched, so a fresh native pass restores the full shade.
        previous.requestLayout();
    }

    static String describe() {
        return "installed=" + installed + " target=" + (frame == null ? "none" : frame.left + ".." + frame.right())
                + " native=" + nativeLeft + "/" + nativeWidth + " measure=" + nativeMeasureWidth
                + " overrides=" + measureOverrides + "/" + frameOverrides + "/" + translationOverrides
                + " columnX=" + columnX + " softEdge=" + edgeInstalled;
    }
}
