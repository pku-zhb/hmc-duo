package com.zhuhuibin.hypermusiccover.duo;

import android.graphics.Rect;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.view.View;
import android.view.ViewGroup;

import java.util.WeakHashMap;

/** Native header events publish card replacement without waiting for another media-data event. */
final class NativeMediaCardLifecycle {
    private NativeMediaCardLifecycle() {}
    private static final String HEADER =
            "com.android.systemui.statusbar.notification.mediacontrol.MiuiMediaHeaderView";
    private static final String CONTROLLER =
            "com.android.systemui.statusbar.notification.mediacontrol.MiuiMediaViewControllerImpl";
    static final class Selection {
        final Object notificationOwner, owner, data;
        final MediaSession.Token token, controllerToken;
        Selection(Object notificationOwner, Object owner, Object data,
                  MediaSession.Token token, MediaSession.Token controllerToken) {
            this.notificationOwner = notificationOwner;
            this.owner = owner;
            this.data = data;
            this.token = token;
            this.controllerToken = controllerToken;
        }
        boolean coherent() {
            return controllerToken == null || java.util.Objects.equals(token, controllerToken);
        }
    }
    private static final WeakHashMap<View, Boolean> observed = new WeakHashMap<>();
    private static final View.OnLayoutChangeListener LAYOUT = (view, l, t, r, b, ol, ot, or, ob) ->
            Main.nativeMediaCardChanged(view, "header layout");
    private static boolean dirty = true;
    private static String lastEvent = "not bound";
    private static java.lang.ref.WeakReference<View> searchedWindow = new java.lang.ref.WeakReference<>(null);

    static void install(ClassLoader loader) {
        // These are the native commits that actually write the selected card and its text.
        // The top-data synthetic accessor is not the only path to an existing native holder.
        try {
            Class<?> controller = Xp.findClass(CONTROLLER, loader);
            for (String event : new String[]{"bindMediaData", "setInfoText"}) {
                Xp.hookAll(controller, event, chain -> {
                    Object result = chain.proceed();
                    Main.nativeMediaSelectionChanged(chain.getThisObject(), event);
                    return result;
                });
            }
        } catch (Throwable error) {
            Xp.w(Main.TAG + "native media selection hook unavailable: " + error);
        }
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

    /** O(1) ownership reads after discovery; never select a player from active-session order. */
    static Selection selection(View card) {
        View header = header(card);
        if (header == null || card == null || !card.isAttachedToWindow()) return null;
        try {
            Object notification = Xp.getObjectField(header, "mediaNotificationController");
            Object owner = Xp.getObjectField(notification, "mediaViewController");
            Object holder = Xp.getObjectField(owner, "holder");
            if (holder == null || Xp.getObjectField(holder, "player") != card
                    || Xp.getObjectField(header, "mediaViewHolder") != holder
                    || Xp.getObjectField(notification, "mediaContainerView") != header) return null;
            Object data = Xp.getObjectField(owner, "mediaData");
            if (data == null) return null;
            // Read the holder's committed data, not global topMediaData or the header's copy.
            // Reinflate binds the new holder before attaching its player; detach clears data.
            MediaSession.Token token = (MediaSession.Token) Xp.getObjectField(data, "token");
            MediaController controller = (MediaController) Xp.getObjectField(owner, "mediaController");
            return new Selection(notification, owner, data, token,
                    controller == null ? null : controller.getSessionToken());
        } catch (Throwable ignored) { return null; }
    }

    static Object topMediaData(Object notification) {
        try { return Xp.getObjectField(notification, "topMediaData"); }
        catch (Throwable ignored) { return null; }
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
