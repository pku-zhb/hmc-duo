package com.zhuhuibin.hypermusiccover.duo;

import android.content.res.Configuration;
import android.graphics.RenderNode;
import android.graphics.Point;
import android.view.View;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** One-way relay from committed OEM player material to explicitly registered Duo layers. */
final class NativeGlassRelay {
    private NativeGlassRelay() {}
    private static final int SOURCE = 0, GLASS_TARGET = 1, BLUR_TARGET = 2;
    private static final Map<View, Integer> targets = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View, GlassProfile> committedGlass = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View, GlassProfile.Blur> committedBlur = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View, GlassProfile.Background> committedBackground = Collections.synchronizedMap(new WeakHashMap<>());
    private static final NativeMaterialCommitScope<View> scopes = new NativeMaterialCommitScope<>();
    private static volatile GlassProfile profile;
    private static volatile GlassProfile.Blur blur;
    private static volatile GlassProfile.Background background;
    private static GlassProfile appliedProfile;
    private static GlassProfile.Blur appliedBlur;
    private static GlassProfile.Background appliedBackground;
    private static boolean installed, glassInstalled, blurInstalled, backgroundInstalled, refreshPosted;
    private static Method glassSetter, blurSetter;
    private static Method backgroundRadius, backgroundScale, backgroundColors;
    private static Method readRadius, readScale, readColors;
    private static Field renderNode;
    private static int captures, applications, blurApplications, refreshes;

    static void register(View target) { register(target, GLASS_TARGET); }
    static void registerBlurContainer(View target) { register(target, BLUR_TARGET); }
    private static void register(View target, int role) {
        // A caller cannot turn an OEM card into a relay destination by registering it.
        if (target != null && NativeMaterialSource.ownedClass(target.getClass().getName()))
            synchronized (targets) { targets.put(target, targets.getOrDefault(target, 0) | role); }
    }
    static void unregister(View target) { targets.remove(target); }

    /** View hook order is irrelevant: only the known RenderNode commit bridge supplies values. */
    static void install() {
        if (installed) return;
        installed = true;
        try {
            renderNode = View.class.getDeclaredField("mRenderNode");
            renderNode.setAccessible(true);
        } catch (Throwable error) {
            Xp.w(Main.TAG + "native material commit identity unavailable: " + error);
            return;
        }
        try {
            glassSetter = View.class.getMethod("setMiGlass", float[].class);
            Method commit = RenderNode.class.getDeclaredMethod("setMiGlass", float[].class);
            Xp.api().hook(commit).intercept(chain -> {
                Object node = chain.getThisObject();
                NativeMaterialCommitScope.Frame<View> frame = scopes.matching(node);
                if (frame == null) return chain.proceed();
                Object[] args = chain.getArgs().toArray();
                if (frame != null && (frame.role & GLASS_TARGET) != 0 && followsNow() && profile != null) {
                    float[] values = profile.forScene(frame.nightMode, frame.awake);
                    if (values != null) { args[0] = values; ++applications; }
                }
                Object result = chain.proceed(args);
                frame = scopes.committed(node, Boolean.TRUE.equals(result));
                if (frame != null && frame.role == SOURCE && args[0] instanceof float[]) {
                    GlassProfile next = GlassProfile.capture((float[]) args[0], frame.nightMode, frame.awake);
                    if (next != null) {
                        committedGlass.put(frame.target, next);
                        if (!next.same(profile)) { profile = next; ++captures; refreshTargets(); }
                    }
                }
                return result;
            });
            installScope(glassSetter);
            glassInstalled = true;
        } catch (Throwable error) { Xp.w(Main.TAG + "native glass commit relay unavailable: " + error); }
        try {
            blurSetter = View.class.getMethod("setMiGlassBlurRadius", int.class, int.class);
            Method commit = RenderNode.class.getDeclaredMethod("setMiGlassBlurRadius", int.class, int.class);
            Xp.api().hook(commit).intercept(chain -> {
                Object node = chain.getThisObject();
                NativeMaterialCommitScope.Frame<View> frame = scopes.matching(node);
                if (frame == null) return chain.proceed();
                Object[] args = chain.getArgs().toArray();
                if (frame != null && (frame.role & BLUR_TARGET) != 0 && followsNow() && blur != null) {
                    int[] values = blur.forScene(frame.nightMode, frame.awake);
                    if (values != null) { args[0] = values[0]; args[1] = values[1]; ++blurApplications; }
                }
                Object result = chain.proceed(args);
                frame = scopes.committed(node, Boolean.TRUE.equals(result));
                if (frame != null && frame.role == SOURCE) {
                    GlassProfile.Blur next = GlassProfile.Blur.capture((Integer) args[0], (Integer) args[1],
                            frame.nightMode, frame.awake);
                    if (next != null) {
                        committedBlur.put(frame.target, next);
                        if (!next.same(blur)) { blur = next; refreshTargets(); }
                    }
                }
                return result;
            });
            installScope(blurSetter);
            blurInstalled = true;
        } catch (Throwable error) { Xp.w(Main.TAG + "native blur commit relay unavailable: " + error); }
        installBackgroundReadback();
    }

    /** These OEM getters read state written by the original setter, including downstream edits. */
    private static void installBackgroundReadback() {
        try {
            readRadius = View.class.getMethod("getMiBackgroundBlurRadius");
            readScale = View.class.getMethod("getMiBackgroundBlurScaleRatio");
            readColors = View.class.getMethod("getMiBackgroundBlendColors");
            backgroundRadius = View.class.getMethod("setMiBackgroundBlurRadius", int.class);
            backgroundScale = View.class.getMethod("setMiBackgroundBlurScaleRatio", float.class);
            backgroundColors = View.class.getMethod("setMiBackgroundBlendColors", ArrayList.class);
            Method clear = View.class.getMethod("clearMiBackgroundBlendColor");
            Method add = View.class.getMethod("addMiBackgroundBlendColor", int.class, int.class);
            for (Method setter : new Method[]{backgroundRadius, backgroundScale, backgroundColors, clear, add}) {
                Xp.api().hook(setter).intercept(chain -> {
                    View view = (View) chain.getThisObject();
                    Integer role = targets.get(view);
                    boolean source = role == null && nativePlayerBackground(view);
                    if (role == null && !source) return chain.proceed();
                    Object[] args = chain.getArgs().toArray();
                    GlassProfile.Background state = currentBackground(view);
                    if (role != null && followsNow() && state != null) {
                        if (setter == backgroundRadius && (role & BLUR_TARGET) != 0) args[0] = state.radius;
                        else if (setter == backgroundScale && (role & BLUR_TARGET) != 0) args[0] = state.scale;
                        else if (setter == backgroundColors && (role & GLASS_TARGET) != 0) args[0] = points(state);
                    }
                    Object result = chain.proceed(args);
                    // Never sample on throw, and never infer committed values from args.
                    if (source && nativePlayerBackground(view)) captureBackground(view);
                    else if (role != null && (role & GLASS_TARGET) != 0 && followsNow() && state != null &&
                            (setter == clear || setter == add)) {
                        // The legacy recipe uses public clear/add. Let it run, then keep the
                        // owned layer on the final palette; bulk apply never re-enters these hooks.
                        try { backgroundColors.invoke(view, points(state)); }
                        catch (Throwable error) { Xp.d(Main.TAG + "owned palette replay skipped: " + error); }
                    }
                    return result;
                });
            }
            backgroundInstalled = true;
        } catch (Throwable error) { Xp.w(Main.TAG + "native background readback unavailable: " + error); }
    }

    private static ArrayList<Point> points(GlassProfile.Background state) {
        int[] colors = state.colors(), modes = state.modes();
        ArrayList<Point> result = new ArrayList<>(colors.length);
        for (int i = 0; i < colors.length; ++i) result.add(new Point(colors[i], modes[i]));
        return result;
    }

    private static void captureBackground(View view) {
        try {
            ArrayList<?> palette = (ArrayList<?>) readColors.invoke(view);
            int[] colors = new int[palette.size()], modes = new int[palette.size()];
            for (int i = 0; i < palette.size(); ++i) {
                Point point = (Point) palette.get(i);
                colors[i] = point.x; modes[i] = point.y;
            }
            GlassProfile.Background next = GlassProfile.Background.capture((Integer) readRadius.invoke(view),
                    (Float) readScale.invoke(view), colors, modes, night(view), Main.screenOnCached());
            if (next != null) {
                committedBackground.put(view, next);
                if (!next.same(background)) { background = next; refreshTargets(); }
            }
        } catch (Throwable error) { Xp.d(Main.TAG + "native background readback skipped: " + error); }
    }

    private static GlassProfile.Background currentBackground(View view) {
        GlassProfile.Background state = background;
        return state != null && state.forScene(night(view), Main.screenOnCached()) ? state : null;
    }

    private static void installScope(Method setter) {
        // The verified RenderNode bridge is tiny and can be inlined into this OEM caller.
        // Deoptimize only these two known callers, not class loading or unrelated rendering.
        Xp.api().deoptimize(setter);
        Xp.api().hook(setter).intercept(chain -> {
            Object self = chain.getThisObject();
            if (!(self instanceof View)) return chain.proceed();
            View view = (View) self;
            Integer role = targets.get(view);
            if (role == null && !nativePlayerBackground(view)) return chain.proceed();
            Object node;
            try { node = renderNode.get(view); } catch (Throwable ignored) { return chain.proceed(); }
            NativeMaterialCommitScope.Frame<View> frame = scopes.enter(view, node,
                    role == null ? SOURCE : role, night(view), Main.screenOnCached());
            try { return chain.proceed(); } finally { scopes.leave(frame); }
        });
    }

    private static int night(View view) {
        return view.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
    }

    private static boolean followsNow() {
        return Main.sFollowNativeMaterial && Main.keyguardLocked() && Main.onKeyguardNow()
                && !MiniPlayerScene.INSTANCE.getKeyguardGoingAway();
    }

    private static void refreshTargets() {
        if (!Main.sFollowNativeMaterial || refreshPosted) return;
        refreshPosted = true;
        Main.main().post(() -> {
            refreshPosted = false;
            if (!followsNow()) return;
            if ((profile == null ? appliedProfile == null : profile.same(appliedProfile)) &&
                    (blur == null ? appliedBlur == null : blur.same(appliedBlur)) &&
                    (background == null ? appliedBackground == null : background.same(appliedBackground))) return;
            // No generation bump, card rebind, or whole-row refresh. Owned target commits
            // can never become sources, even when replay invokes the same vendor setters.
            ArrayList<Map.Entry<View, Integer>> snapshot;
            synchronized (targets) { snapshot = new ArrayList<>(targets.entrySet()); }
            for (Map.Entry<View, Integer> item : snapshot) {
                View view = item.getKey();
                if (view == null || !view.isAttachedToWindow() || !targets.containsKey(view)) continue;
                try {
                    int mode = night(view);
                    boolean awake = Main.screenOnCached();
                    GlassProfile current = profile;
                    GlassProfile.Blur radius = blur;
                    float[] values = current == null ? null : current.forScene(mode, awake);
                    int[] radii = radius == null ? null : radius.forScene(mode, awake);
                    if (glassInstalled && (item.getValue() & GLASS_TARGET) != 0 && values != null)
                        glassSetter.invoke(view, (Object) values);
                    if (blurInstalled && (item.getValue() & BLUR_TARGET) != 0 && radii != null)
                        blurSetter.invoke(view, radii[0], radii[1]);
                    GlassProfile.Background state = currentBackground(view);
                    if (backgroundInstalled && state != null) {
                        if ((item.getValue() & GLASS_TARGET) != 0) backgroundColors.invoke(view, points(state));
                        if ((item.getValue() & BLUR_TARGET) != 0) {
                            backgroundRadius.invoke(view, state.radius);
                            backgroundScale.invoke(view, state.scale);
                        }
                    }
                } catch (Throwable error) { Xp.d(Main.TAG + "owned material relay skipped: " + error); }
            }
            appliedProfile = profile; appliedBlur = blur; appliedBackground = background;
            ++refreshes;
        });
    }

    static boolean nativePlayerBackground(View view) {
        if (view == null || view.getId() == View.NO_ID || !Main.keyguardLocked() || !Main.onKeyguardNow()) return false;
        String id;
        try { id = view.getResources().getResourceEntryName(view.getId()); }
        catch (Throwable ignored) { return false; }
        if (!"media_bg".equals(id)) return false;
        ArrayList<String> lineage = new ArrayList<>();
        for (View v = view; v != null; v = v.getParent() instanceof View ? (View) v.getParent() : null)
            lineage.add(v.getClass().getName());
        return NativeMaterialSource.accepts(id, lineage, targets.containsKey(view));
    }

    /** Signature inputs only: never use these as a baseline for the independent native mode. */
    static float[] committedGlass(View source) {
        GlassProfile found = committedGlass.get(source);
        return found == null || source == null ? null : found.forScene(night(source), Main.screenOnCached());
    }
    static int[] committedBlur(View source) {
        GlassProfile.Blur found = committedBlur.get(source);
        return found == null || source == null ? null : found.forScene(night(source), Main.screenOnCached());
    }
    static GlassProfile.Background committedBackground(View source) {
        GlassProfile.Background found = committedBackground.get(source);
        return found != null && source != null && found.forScene(night(source), Main.screenOnCached()) ? found : null;
    }

    static String describe() {
        return "glassRelay enabled=" + Main.sFollowNativeMaterial + " installed=" + installed
                + " committed=" + glassInstalled + "/" + blurInstalled + "/" + backgroundInstalled
                + " profile=" + (profile != null) + " captures=" + captures
                + " blurProfile=" + (blur != null) + " backgroundProfile=" + (background != null)
                + " applications=" + applications + " blurApplications=" + blurApplications
                + " ownedRefreshes=" + refreshes + " targets=" + targets.size();
    }
}
