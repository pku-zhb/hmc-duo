package com.zhuhuibin.hypermusiccover.duo;

import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.view.View;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

/** Return a retired native artwork's actual properties, without undoing a newer native writer. */
final class NativeMediaArtworkLease {
    private NativeMediaArtworkLease() {}
    private static final Map<View, Pose> poses = java.util.Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View, NativeArtworkEffectState<RenderEffect>> effects =
            java.util.Collections.synchronizedMap(new WeakHashMap<>());
    private static boolean observesEffects;
    private static int artworkId;

    private static final class Pose {
        final ClockPropertyLease visibility = new ClockPropertyLease(), alpha = new ClockPropertyLease();
        final ClockPropertyLease sx = new ClockPropertyLease(), sy = new ClockPropertyLease();
        float blurRadius = Float.NaN;
    }

    static void install() {
        try {
            Method setter = View.class.getDeclaredMethod("setRenderEffect", RenderEffect.class);
            Xp.api().deoptimize(setter);
            Xp.hook(setter, chain -> {
                Object result = chain.proceed();
                Object target = chain.getThisObject();
                if (target instanceof View) {
                    View view = (View) target;
                    // Observation only. Unrelated SystemUI materials are never read or changed.
                    if (artwork(view)) {
                        Object[] args = chain.getArgs().toArray();
                        if (args.length == 1) effect(view).observe((RenderEffect) args[0]);
                    }
                }
                return result;
            });
            observesEffects = true;
        } catch (Throwable error) {
            // Without a getter or a verified setter observation, the native effect is unknown.
            Xp.w(Main.TAG + "artwork effect observation unavailable: " + error);
        }
    }

    private static boolean artwork(View view) {
        if (poses.containsKey(view) || effects.containsKey(view)) return true;
        int id = view.getId();
        if (id == View.NO_ID || id == 0) return false;
        if (artworkId != 0) return id == artworkId;
        try {
            if (!"com.android.systemui".equals(view.getResources().getResourcePackageName(id))
                    || !"album_art".equals(view.getResources().getResourceEntryName(id))) return false;
            artworkId = id;
            return true;
        } catch (Throwable ignored) { return false; }
    }

    private static Pose pose(View view) {
        Pose pose = poses.get(view);
        if (pose == null) { pose = new Pose(); poses.put(view, pose); }
        return pose;
    }

    private static NativeArtworkEffectState<RenderEffect> effect(View view) {
        synchronized (effects) {
            NativeArtworkEffectState<RenderEffect> effect = effects.get(view);
            if (effect == null) { effect = new NativeArtworkEffectState<>(); effects.put(view, effect); }
            return effect;
        }
    }

    static void visibility(View view, int value) {
        int current = view.getVisibility();
        int next = (int) pose(view).visibility.write(current, value);
        if (current != next) view.setVisibility(next);
    }

    static void alpha(View view, float value) {
        float current = view.getAlpha(), next = pose(view).alpha.write(current, value);
        if (current != next) view.setAlpha(next);
    }

    static void scale(View view, float x, float y) {
        Pose pose = pose(view);
        float sx = view.getScaleX(), sy = view.getScaleY();
        float nx = pose.sx.write(sx, x), ny = pose.sy.write(sy, y);
        if (sx != nx) view.setScaleX(nx);
        if (sy != ny) view.setScaleY(ny);
    }

    static void blur(View view, float radius) {
        if (radius < .5f) { clearBlur(view); return; }
        if (!observesEffects) return;
        NativeArtworkEffectState<RenderEffect> effect = effect(view);
        if (!effect.known()) return;
        Pose pose = pose(view);
        if (pose.blurRadius == radius && effect.owns()) return;
        RenderEffect next = RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.DECAL);
        if (effect.write(next)) { view.setRenderEffect(next); pose.blurRadius = radius; }
    }

    private static void clearBlur(View view) {
        Pose pose = poses.get(view);
        if (pose != null) pose.blurRadius = Float.NaN;
        NativeArtworkEffectState<RenderEffect> effect = effects.get(view);
        if (effect == null || !effect.known()) return;
        RenderEffect before = effect.current(), restored = effect.release();
        if (restored != before) view.setRenderEffect(restored);
    }

    static void release(View view) {
        if (view == null) return;
        Pose pose = poses.remove(view);
        if (pose != null) {
            int visibility = view.getVisibility(), restoredVisibility = (int) pose.visibility.restore(visibility);
            float alpha = view.getAlpha(), restoredAlpha = pose.alpha.restore(alpha);
            float sx = view.getScaleX(), sy = view.getScaleY();
            float restoredX = pose.sx.restore(sx), restoredY = pose.sy.restore(sy);
            if (visibility != restoredVisibility) view.setVisibility(restoredVisibility);
            if (alpha != restoredAlpha) view.setAlpha(restoredAlpha);
            if (sx != restoredX) view.setScaleX(restoredX);
            if (sy != restoredY) view.setScaleY(restoredY);
        }
        clearBlur(view);
    }
}
