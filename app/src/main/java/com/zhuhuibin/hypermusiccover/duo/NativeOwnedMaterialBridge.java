package com.zhuhuibin.hypermusiccover.duo;

import android.os.Looper;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/** Android boundary of the optional provider contract. No provider class is loaded by Duo. */
final class NativeOwnedMaterialBridge {
    private static final NativeOwnedMaterialRegistry<View> registry = new NativeOwnedMaterialRegistry<>();
    private static final ThreadLocal<Boolean> reapplying = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> nativeAod = new ThreadLocal<>();
    private static final WeakHashMap<View, Readiness> readiness = new WeakHashMap<>();
    private NativeOwnedMaterialBridge() {}

    static boolean register(ImageView element, View container) {
        if (!(element instanceof MaterialElement) || !(container instanceof NativeMaterialTarget)
                || element.getParent() != container) return false;
        retireReparented(element); retireReparented(container);
        if (!registry.register(element, container)) return false;
        if (!readiness.containsKey(element)) {
            Readiness ready = new Readiness(element);
            readiness.put(element, ready);
            element.addOnAttachStateChangeListener(ready);
            element.addOnLayoutChangeListener(ready);
        }
        return true;
    }
    private static void retireReparented(View target) {
        View owner = registry.owner(target), container = registry.container(target);
        if (owner != null && owner.getParent() != container) registry.remove(target);
    }
    private static View view(Object target) { return target instanceof View ? (View) target : null; }
    private static boolean valid(View target) {
        View owner = registry.owner(target), container = registry.container(target);
        return owner != null && container != null && owner.getParent() == container;
    }
    static View owner(Object target) {
        View view = view(target);
        return valid(view) ? registry.owner(view) : null;
    }
    static boolean claimed(View target) { return valid(target) && registry.claimed(target); }
    static boolean claim(Object target, int version) {
        View view = view(target);
        return valid(view) && registry.claim(view, version);
    }
    static int state(Object target) {
        View view = view(target);
        if (!valid(view)) return 0;
        int state = registry.role(view);
        if (Main.sFollowNativeMaterial) state |= NativeMaterialTarget.MATERIAL_FOLLOW;
        if (keyguard(view)) state |= NativeMaterialTarget.MATERIAL_KEYGUARD;
        if (Boolean.TRUE.equals(nativeAod.get()) || MiniPlayerRuntime.ownedNativeAodRecipe())
            state |= NativeMaterialTarget.MATERIAL_NATIVE_AOD;
        if (registry.claimed(view)) state |= NativeMaterialTarget.MATERIAL_CLAIMED;
        return state;
    }
    private static boolean keyguard(View target) {
        View owner = registry.owner(target), container = registry.container(target), host = Main.sContainer;
        return host != null && owner != null && container != null
                && owner.isAttachedToWindow() && container.isAttachedToWindow()
                && owner.getWidth() > 1 && owner.getHeight() > 1
                && container.getWidth() > 1 && container.getHeight() > 1
                && owner.getRootView() == host.getRootView()
                && Main.keyguardLocked() && Main.onKeyguardNow() && !Main.bouncerShown()
                && !MiniPlayerScene.INSTANCE.getKeyguardGoingAway()
                && !MiniPlayerScene.INSTANCE.getControlCenterIsActive()
                && !MiniPlayerScene.INSTANCE.getBlocksMiniPlayer();
    }
    static boolean reapply(Object target) {
        View owner = owner(target);
        if (!(owner instanceof ImageView) || !keyguard(owner)
                || Looper.myLooper() != Looper.getMainLooper() || Boolean.TRUE.equals(reapplying.get())) return false;
        reapplying.set(true);
        try { return MiniPlayerRuntime.reapplyOwnedNativeMaterial((ImageView) owner); }
        finally { reapplying.remove(); }
    }
    static void refresh() {
        if (Looper.myLooper() != Looper.getMainLooper()) { Main.main().post(NativeOwnedMaterialBridge::refresh); return; }
        for (View owner : registry.owners()) {
            if (!reapply(owner)) {
                Readiness ready = readiness.get(owner);
                if (ready != null) ready.applied(false);
            }
        }
    }
    static void applied(View owner) {
        Readiness ready = readiness.get(owner);
        if (ready != null) ready.applied(keyguard(owner));
    }
    private static final class Readiness implements View.OnAttachStateChangeListener, View.OnLayoutChangeListener,
            ViewTreeObserver.OnGlobalLayoutListener {
        final WeakReference<ImageView> element;
        final NativeOwnedMaterialReady gate = new NativeOwnedMaterialReady();
        WeakReference<ViewTreeObserver> watching = new WeakReference<>(null);
        Readiness(ImageView element) { this.element = new WeakReference<>(element); }
        private void unwatch() {
            ViewTreeObserver observer = watching.get();
            if (observer != null && observer.isAlive()) observer.removeOnGlobalLayoutListener(this);
            watching.clear();
        }
        private void applied(boolean ready) {
            gate.applied(ready);
            ImageView owner = element.get();
            if (ready || owner == null || !owner.isAttachedToWindow() || !Main.keyguardLocked()
                    || MiniPlayerScene.INSTANCE.getKeyguardGoingAway()) { unwatch(); return; }
            // Global layout is also the native visibility-change event. The clock host can
            // be a sibling of the element, so an element-only visibility callback is insufficient.
            ViewTreeObserver observer = owner.getViewTreeObserver();
            if (watching.get() != observer && observer.isAlive()) {
                unwatch(); watching = new WeakReference<>(observer); observer.addOnGlobalLayoutListener(this);
            }
        }
        private void changed() {
            ImageView owner = element.get();
            boolean ready = owner != null && valid(owner) && keyguard(owner);
            if (gate.update(ready) && !reapply(owner)) ready = false;
            applied(ready);
        }
        public void onViewAttachedToWindow(View view) { changed(); }
        public void onViewDetachedFromWindow(View view) { gate.applied(false); unwatch(); }
        public void onLayoutChange(View view, int l, int t, int r, int b, int ol, int ot, int or, int ob) { changed(); }
        public void onGlobalLayout() { changed(); }
    }
    static void beginNativeAod() { nativeAod.set(true); }
    static void endNativeAod() { nativeAod.remove(); }

    /** Explicit diagnostic only: identities and material state, never notification content. */
    static String describe() {
        int groups = 0, claimed = 0;
        StringBuilder entries = new StringBuilder();
        for (View owner : registry.owners()) {
            if (!valid(owner)) continue;
            View container = registry.container(owner);
            int elementState = state(owner), containerState = state(container);
            groups++;
            if (registry.claimed(owner)) claimed++;
            if (groups > 12) continue;
            if (entries.length() > 0) entries.append(';');
            entries.append("e#").append(Integer.toHexString(System.identityHashCode(owner)))
                    .append("/c#").append(Integer.toHexString(System.identityHashCode(container)))
                    .append(" roles=").append(registry.role(owner)).append('/').append(registry.role(container))
                    .append(" state=").append(elementState).append('/').append(containerState)
                    .append(" attached=").append(owner.isAttachedToWindow()).append('/').append(container.isAttachedToWindow())
                    .append(" keyguard=").append((elementState & NativeMaterialTarget.MATERIAL_KEYGUARD) != 0)
                    .append(" aod=").append((elementState & NativeMaterialTarget.MATERIAL_NATIVE_AOD) != 0);
        }
        return "groups=" + groups + " roles=" + (groups * 2) + " claimed=" + claimed
                + " owners=[" + entries + (groups > 12 ? ";..." : "") + "]";
    }
}
