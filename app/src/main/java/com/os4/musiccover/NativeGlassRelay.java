package com.os4.musiccover;

import android.content.res.Configuration;
import android.view.View;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** Relay final native player glass to our own layers, after scene-specific material hooks. */
final class NativeGlassRelay {
    private NativeGlassRelay() {}
    private static final Set<View> targets = Collections.newSetFromMap(new WeakHashMap<>());
    private static GlassProfile profile;
    private static GlassProfile.Blur blur;
    private static boolean installed, refreshPosted;
    private static int captures, applications, blurApplications;

    static void register(View target) { targets.add(target); }
    static void unregister(View target) { targets.remove(target); }

    /** Installed from clock attachment, after both modules' package hooks and our clock guard. */
    static void install() {
        if (installed) return;
        try {
            Xp.hookAll(View.class, "setMiGlass", chain -> {
                Object self = chain.getThisObject();
                Object[] args = chain.getArgs().toArray();
                if (!(self instanceof View) || args.length != 1 || !(args[0] instanceof float[])) {
                    return chain.proceed();
                }
                View view = (View) self;
                int night = view.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                boolean awake = Main.screenOnCached();
                if (targets.contains(view)) {
                    if (Main.sFollowNativeMaterial && Main.keyguardLocked()
                            && !MiniPlayerScene.INSTANCE.getKeyguardGoingAway() && Main.onKeyguardNow()
                            && profile != null) {
                        float[] finalValues = profile.forScene(night, awake);
                        if (finalValues != null) { args[0] = finalValues; ++applications; }
                    }
                } else if (nativePlayerBackground(view)) {
                    GlassProfile next = GlassProfile.capture((float[]) args[0], night, awake);
                    if (next != null && !next.same(profile)) {
                        profile = next;
                        ++captures;
                        refreshTargets();
                    }
                }
                return chain.proceed(args);
            });
            installed = true;
            try {
                Xp.hookAll(View.class, "setMiGlassBlurRadius", chain -> {
                    Object self = chain.getThisObject();
                    Object[] args = chain.getArgs().toArray();
                    if (!(self instanceof View) || args.length != 2
                            || !(args[0] instanceof Integer) || !(args[1] instanceof Integer)) return chain.proceed();
                    View view = (View) self;
                    int night = view.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                    boolean awake = Main.screenOnCached();
                    if (targets.contains(view)) {
                        if (Main.sFollowNativeMaterial && Main.keyguardLocked()
                            && !MiniPlayerScene.INSTANCE.getKeyguardGoingAway() && Main.onKeyguardNow() && blur != null) {
                            int[] radii = blur.forScene(night, awake);
                            if (radii != null) { args[0] = radii[0]; args[1] = radii[1]; ++blurApplications; }
                        }
                    } else if (nativePlayerBackground(view)) {
                        GlassProfile.Blur next = GlassProfile.Blur.capture((Integer) args[0], (Integer) args[1], night, awake);
                        if (next != null && !next.same(blur)) { blur = next; refreshTargets(); }
                    }
                    return chain.proceed(args);
                });
            } catch (Throwable error) { Xp.w(Main.TAG + "native blur relay unavailable: " + error); }
        } catch (Throwable error) {
            Xp.w(Main.TAG + "native glass relay unavailable: " + error);
        }
    }

    private static void refreshTargets() {
        if (!Main.sFollowNativeMaterial || refreshPosted) return;
        refreshPosted = true;
        Main.main().post(() -> {
            refreshPosted = false;
            MiniPlayerRuntime.refreshNativeMaterial();
        });
    }

    private static boolean nativePlayerBackground(View view) {
        if (view.getId() == View.NO_ID) return false;
        String id;
        try { id = view.getResources().getResourceEntryName(view.getId()); }
        catch (Throwable ignored) { return false; }
        if (!"media_bg".equals(id)) return false;
        boolean media = false;
        for (View v = view; v != null; v = v.getParent() instanceof View ? (View) v.getParent() : null) {
            String name = v.getClass().getName();
            if (name.contains("mediaisland") || name.contains("dynamicisland")
                    || name.contains("DynamicIsland") || name.contains("PlayerIsland")) return false;
            if (name.startsWith("com.android.systemui.statusbar.notification.mediacontrol.")) media = true;
        }
        return media && Main.keyguardLocked() && Main.onKeyguardNow();
    }

    static String describe() {
        return "glassRelay enabled=" + Main.sFollowNativeMaterial + " installed=" + installed
                + " profile=" + (profile != null) + " captures=" + captures
                + " applications=" + applications + " blurApplications=" + blurApplications
                + " targets=" + targets.size();
    }
}
