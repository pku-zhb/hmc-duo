package com.zhuhuibin.hypermusiccover.duo;

import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;

import java.util.WeakHashMap;

/** Native header events publish card replacement without waiting for another media-data event. */
final class NativeMediaCardLifecycle {
    private NativeMediaCardLifecycle() {}
    private static final String HEADER =
            "com.android.systemui.statusbar.notification.mediacontrol.MiuiMediaHeaderView";
    private static final WeakHashMap<View, Boolean> observed = new WeakHashMap<>();
    private static final View.OnLayoutChangeListener LAYOUT = (view, l, t, r, b, ol, ot, or, ob) ->
            Main.nativeMediaCardChanged(view, "header layout");
    private static boolean dirty = true;
    private static String lastEvent = "not bound";
    private static java.lang.ref.WeakReference<View> searchedWindow = new java.lang.ref.WeakReference<>(null);

    static void install(ClassLoader loader) {
        Class<?> header;
        try { header = Xp.findClass(HEADER, loader); }
        catch (Throwable error) { Xp.w(Main.TAG + "native media header unavailable: " + error); return; }
        for (String event : new String[]{"onAttachedToWindow", "onDetachedFromWindow",
                "onConfigurationChanged", "onVisibilityAggregated", "setMediaViewHolder"}) {
            try {
                Xp.hookAll(header, event, chain -> {
                    Object result = chain.proceed();
                    Object target = chain.getThisObject();
                    if (target instanceof View) {
                        View view = (View) target;
                        observe(view);
                        Main.nativeMediaCardChanged(view, event);
                    }
                    return result;
                });
            } catch (Throwable error) {
                Xp.w(Main.TAG + "native media lifecycle " + event + " unavailable: " + error);
            }
        }
    }

    private static void observe(View header) {
        if (!observed.containsKey(header)) {
            observed.put(header, Boolean.TRUE);
            header.addOnLayoutChangeListener(LAYOUT);
        }
    }

    static void invalidate(String event) { dirty = true; lastEvent = event; }
    static boolean dirty() { return dirty; }
    static String event() { return lastEvent; }

    static View header(View card) {
        for (View view = card; view != null;
                view = view.getParent() instanceof View ? (View) view.getParent() : null)
            if (isHeader(view)) return view;
        return null;
    }

    private static boolean isHeader(View view) {
        for (Class<?> type = view.getClass(); type != null; type = type.getSuperclass())
            if (HEADER.equals(type.getName())) return true;
        return false;
    }

    static boolean inWindow(View card, View window) {
        return card != null && window != null && card.isAttachedToWindow()
                && card.getRootView() == window && header(card) != null;
    }

    static View cardIn(View header) {
        int id = ResourceIds.id(header.getResources(), "mi_media_controls");
        return id == 0 ? null : header.findViewById(id);
    }

    /** Diagnostics must not repair the binding they are trying to inspect. */
    static View inspect(View window) {
        if (window == null || !window.isAttachedToWindow()) return null;
        if (isHeader(window)) {
            View card = cardIn(window);
            Rect visible = new Rect();
            return card != null && card.isAttachedToWindow() && card.isShown()
                    && card.getGlobalVisibleRect(visible) && !visible.isEmpty() ? card : null;
        }
        if (window instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) window;
            for (int i = 0; i < group.getChildCount(); i++) {
                View card = inspect(group.getChildAt(i));
                if (card != null) return card;
            }
        }
        return null;
    }

    /** Discovery runs after lifecycle changes or an invalid binding, never as a polling loop. */
    static View resolve(View window, View current) {
        dirty = false;
        if (window == null || !window.isAttachedToWindow()) return null;
        if (searchedWindow.get() != window) {
            searchedWindow = new java.lang.ref.WeakReference<>(window);
            collect(window);
        }
        boolean currentValid = inWindow(current, window);
        View hidden = currentValid ? current : null;
        Rect visible = new Rect();
        for (View header : observed.keySet()) {
            View candidate = cardIn(header);
            boolean sameWindow = candidate != null && candidate.getRootView() == window;
            boolean attached = candidate != null && candidate.isAttachedToWindow();
            boolean shown = attached && candidate.isShown()
                    && candidate.getGlobalVisibleRect(visible) && !visible.isEmpty();
            if (!NativeMediaCardBindingState.shouldReplace(currentValid, sameWindow,
                    attached, shown, header(candidate) != null)) continue;
            if (shown) return candidate;
            if (hidden == null) hidden = candidate;
        }
        return hidden;
    }

    private static void collect(View view) {
        if (isHeader(view)) { observe(view); return; }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) collect(group.getChildAt(i));
        }
    }
}
