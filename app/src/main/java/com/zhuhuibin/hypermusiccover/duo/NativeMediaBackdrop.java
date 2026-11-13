package com.zhuhuibin.hypermusiccover.duo;

import android.view.View;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

/** Makes the existing native media glass sample the visible artwork in its own window. */
final class NativeMediaBackdrop {
    private NativeMediaBackdrop() {}

    private static final NativeMaterialSourceBinding<View> binding = new NativeMaterialSourceBinding<>();
    private static final NativeSamplingLease lease = new NativeSamplingLease();
    private static final ThreadLocal<Boolean> replay = new ThreadLocal<>();
    private static WeakReference<View> root = new WeakReference<>(null), nativeContainer = new WeakReference<>(null);
    private static Method readMode, readViewMode, readPass, readType, readContainer;
    private static Method writeMode, writePass, writeType, writeContainer;
    private static boolean installed, available, consumesBlur;
    private static int switches, restores;
    private static String failure;

    private static final View.OnAttachStateChangeListener attachment = new View.OnAttachStateChangeListener() {
        @Override public void onViewAttachedToWindow(View owner) {
            View source = binding.source();
            if (source != null && owner == binding.owner()) bind(source, owner);
        }
        @Override public void onViewDetachedFromWindow(View owner) {
            if (owner == binding.owner()) clear();
        }
    };

    static void install() {
        if (installed) return;
        installed = true;
        try {
            // These getters expose committed state in the shipped framework, not guessed defaults.
            readMode = View.class.getMethod("getMiBackgroundBlurMode");
            readViewMode = View.class.getMethod("getMiViewBlurMode");
            readPass = View.class.getMethod("getPassWindowBlurEnabled");
            readType = View.class.getMethod("getMiBackgroundBlurType");
            readContainer = View.class.getMethod("getBackGroundBlurContainer");
            writeMode = View.class.getMethod("setMiBackgroundBlurMode", int.class);
            writePass = View.class.getMethod("setPassWindowBlurEnabled", boolean.class);
            writeType = View.class.getMethod("setMiBackgroundBlurType", int.class);
            writeContainer = View.class.getMethod("chooseBackgroundBlurContainer", View.class);
            Method writeViewMode = View.class.getMethod("setMiViewBlurMode", int.class);
            for (Method setter : new Method[]{writeMode, writePass, writeType, writeContainer, writeViewMode}) {
                Xp.api().hook(setter).intercept(chain -> {
                    Object result = chain.proceed();
                    View target = (View) chain.getThisObject();
                    // Unrelated views do not inspect a scene, hierarchy, or arguments.
                    if (Boolean.TRUE.equals(replay.get()) || !binding.isSource(target)) return result;
                    try {
                        // Capture only the property this native call changed. The other
                        // properties can still contain our temporary local override.
                        if (setter == writeMode) lease.nativeMode((Integer) readMode.invoke(target));
                        else if (setter == writePass) lease.nativePassWindow((Boolean) readPass.invoke(target));
                        else if (setter == writeType) lease.nativeBlurType((Integer) readType.invoke(target));
                        else if (setter == writeContainer) {
                            nativeContainer = new WeakReference<>((View) readContainer.invoke(target));
                            lease.nativeContainerChanged();
                        } else consumesBlur = (Integer) readViewMode.invoke(target) == 1;
                        refresh();
                    } catch (Throwable error) { fail(error); }
                    return result;
                });
            }
            available = true;
        } catch (Throwable error) {
            failure = error.getClass().getSimpleName();
            Xp.w(Main.TAG + "native media sampling unavailable: " + error);
        }
    }

    /** Called only after NativeGlassRelay accepts the actual holder's media_bg. */
    static void bind(View source, View owner) {
        if (!available || source == null || owner == null) return;
        Object parent = source.getParent(), ownerParent = owner.getParent();
        if (binding.matches(source, parent, owner, ownerParent) && root.get() == source.getRootView()) {
            refresh();
            return;
        }
        clear();
        if (!binding.bind(source, parent, owner, ownerParent, () -> true)) return;
        try {
            root = new WeakReference<>(source.getRootView());
            int mode = (Integer) readMode.invoke(source), type = (Integer) readType.invoke(source);
            boolean pass = (Boolean) readPass.invoke(source);
            View container = (View) readContainer.invoke(source);
            boolean consumer = (Integer) readViewMode.invoke(source) == 1;
            // Arm the lease only after every restoration input has been read successfully.
            lease.bind(mode, pass, type);
            nativeContainer = new WeakReference<>(container);
            consumesBlur = consumer;
            owner.addOnAttachStateChangeListener(attachment);
            refresh();
        } catch (Throwable error) { fail(error); }
    }

    static void clear() {
        View owner = binding.owner(), source = binding.source();
        if (owner != null) owner.removeOnAttachStateChangeListener(attachment);
        if (source != null && lease.local()) {
            try { write(source, false); ++restores; }
            catch (Throwable error) { Xp.d(Main.TAG + "native media sampler restore skipped: " + error); }
        }
        binding.clear();
        root.clear();
        nativeContainer.clear();
        consumesBlur = false;
        lease.clear();
    }

    private static boolean validBinding() {
        View source = binding.source(), owner = binding.owner();
        return source != null && owner != null && source.isAttachedToWindow() && owner.isAttachedToWindow()
                && source.getRootView() == root.get()
                && binding.matches(source, source.getParent(), owner, owner.getParent());
    }

    /** The backdrop's existing pre-draw guard only updates an already accepted source. */
    static void refresh() {
        if (!available || Boolean.TRUE.equals(replay.get())) return;
        if (!validBinding()) {
            // An early native effect can run before attach. Keep its native state untouched;
            // the next accepted effect binds the attached holder normally.
            if (lease.local()) clear();
            return;
        }
        try {
            View source = binding.source();
            boolean local = consumesBlur && VideoBackdropMesh.samplingBackdropIn(source);
            if (lease.needsApply(local)) {
                write(source, local);
                if (local) ++switches;
                else if (lease.local()) ++restores;
                lease.committed(local);
            }
        } catch (Throwable error) { fail(error); }
    }

    private static void write(View source, boolean local) throws Exception {
        NativeSamplingLease.Flags flags = lease.target(local);
        View container = local ? null : nativeContainer.get();
        Boolean previous = replay.get();
        replay.set(true);
        try {
            // media_bg is the leaf that draws the real OEM glass. A sampler on a root
            // ancestor cannot see artwork drawn in that ancestor's own child subtree.
            // Give this same leaf its OEM local input, without replacing its glass recipe.
            if ((Boolean) readPass.invoke(source) != flags.passWindow) writePass.invoke(source, flags.passWindow);
            if ((Integer) readType.invoke(source) != flags.blurType) writeType.invoke(source, flags.blurType);
            if (readContainer.invoke(source) != container) writeContainer.invoke(source, container);
            if ((Integer) readMode.invoke(source) != flags.mode) writeMode.invoke(source, flags.mode);
            if ((Boolean) readPass.invoke(source) != flags.passWindow || (Integer) readType.invoke(source) != flags.blurType
                    || (Integer) readMode.invoke(source) != flags.mode || readContainer.invoke(source) != container)
                throw new IllegalStateException("native media sampler rejected flags");
        } finally {
            if (previous == null) replay.remove(); else replay.set(previous);
        }
    }

    private static void fail(Throwable error) {
        // Restore a partial write before retiring this binding; never retry per frame.
        View source = binding.source();
        if (source != null && lease.bound()) {
            try { write(source, false); lease.committed(false); }
            catch (Throwable ignored) {}
        }
        failure = error.getClass().getSimpleName();
        clear();
        Xp.w(Main.TAG + "native media local sampling skipped: " + error);
    }

    static String describe() {
        return "available=" + available + " bound=" + (binding.source() != null)
                + " local=" + lease.local() + " switches=" + switches + " restores=" + restores
                + (failure == null ? "" : " fallback=" + failure);
    }
}
