package com.zhuhuibin.hypermusiccover.duo;

import android.graphics.Insets;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import java.util.IdentityHashMap;

/** Duo-owned lock-screen geometry, with native vertical content behavior. */
final class LockScreenRailRuntime {
    private LockScreenRailRuntime() {}
    private static View root;
    private static ViewTreeObserver observer;
    private static ViewTreeObserver.OnPreDrawListener listener;
    private static volatile RightRailLayout.Plan plan;
    private static volatile boolean contentActive;
    private static volatile boolean sessionEligible;
    private static volatile int notificationFloor;
    private static View keyguardCanvas;
    private static boolean railXPinned;
    private static long planRevision = -1;
    private static int planWidth, planHeight;
    private static final IdentityHashMap<View, ControlOffset> clockOffsets = new IdentityHashMap<>();
    private static final class ControlOffset {
        float baseline, written, delta;
        ControlOffset(View v, float delta) { baseline = written = v.getTranslationY(); this.delta = delta; }
        void apply(View v) {
            if (v.getTranslationY() != written) baseline = v.getTranslationY();
            written = baseline + delta;
            if (v.getTranslationY() != written) v.setTranslationY(written);
        }
        void restore(View v) { if (v.getTranslationY() == written) v.setTranslationY(baseline); }
    }
    private static String reason = "not attached";
    private static long revision = -1;
    private static final Matrix matrix = new Matrix(), inverse = new Matrix();
    private static final RectF box = new RectF();
    private static final IdentityHashMap<View, Owned> owned = new IdentityHashMap<>();
    private static final IdentityHashMap<ViewGroup, Integer> clipLeases = new IdentityHashMap<>();
    private static final java.util.ArrayDeque<String> events = new java.util.ArrayDeque<>();
    private static final class Owned {
        final GeometryLease lease;
        Owned(View v) {
            lease = new GeometryLease(v.getLayoutParams().width, v.getTranslationX());
        }
    }

    static void attach(View anchor) {
        View next = anchor.getRootView();
        if (root == next && observer != null && observer.isAlive()) return;
        detach();
        root = next;
        observer = next.getViewTreeObserver();
        listener = () -> {
            try { update(); }
            catch (Throwable error) {
                plan = null;
                RailStatusRuntime.hide();
                restore();
                reason = "adapter unavailable: " + error.getClass().getSimpleName();
                Xp.w(Main.TAG + "right rail: " + error);
            }
            return true;
        };
        observer.addOnPreDrawListener(listener);
    }

    static void detach() {
        if (observer != null && observer.isAlive() && listener != null) observer.removeOnPreDrawListener(listener);
        observer = null; listener = null;
        plan = null;
        sessionEligible = false;
        restore();
        root = null;
        keyguardCanvas = null;
        railXPinned = false;
        RailStatusRuntime.detach();
    }

    static void refresh() {
        RailStatusRuntime.hide();
        restore();
        plan = null;
        railXPinned = false;
        if (root != null) root.invalidate();
        MiniPlayerRuntime.refresh();
    }

    static boolean active() {
        return Main.sRightRail && sessionEligible && plan != null;
    }
    static boolean reflowing() { return active() && Main.sReflowLockContent && contentActive; }

    private static void update() {
        boolean wasActive = active(), wasContent = reflowing();
        sessionEligible = root != null && RailLifecycle.eligible(Main.sRightRail,
                Main.keyguardLocked(), Main.bouncerShown());
        if (!sessionEligible) {
            railXPinned = false;
            RailStatusRuntime.hide();
            plan = null;
            restore();
            note(Main.sRightRail ? "outside lock screen" : "disabled");
            return;
        }
        if (MiniPlayerScene.INSTANCE.getKeyguardGoingAway()) {
            railXPinned = false;
            RailStatusRuntime.hide(); return;
        }
        if (revision != Main.screenGeometry().revision || planWidth != root.getWidth()
                || planHeight != root.getHeight()) {
            restore();
            plan = null;
            railXPinned = false;
            revision = Main.screenGeometry().revision;
            planWidth = root.getWidth(); planHeight = root.getHeight();
        }
        boolean aodFrozen = holdGeometry();
        boolean retained = RailLifecycle.retain(sessionEligible, plan != null, planRevision, revision);
        RightRailLayout.Plan next = retained ? plan : planForWindow();
        if (next != null) planRevision = revision;
        plan = next;
        if (next != null) RailStatusRuntime.update(root, next);
        else RailStatusRuntime.hide();
        if (next != null && Main.sReflowLockContent) {
            cacheNotificationFloor();
            int id = root.getResources().getIdentifier("notification_stack_scroller", "id", "com.android.systemui");
            View stack = id == 0 ? null : root.findViewById(id);
            // Keep the OEM clock's drawing canvas at its native width. Its glass renderer
            // hard-clips to that canvas even with clipChildren disabled. Move the measured
            // glyphs instead of narrowing the canvas and then translating ink out of it.
            boolean stackOwned = RailStackBounds.bind(stack, root, next.content);
            View date = Main.visibleDate();
            if (date != null && !under(date, stack) && !ClockCollapse.compactRowMoving())
                centerContentView(date, next.content);
            centerClocks();
            contentActive = stackOwned || !owned.isEmpty();
            if (contentActive && Main.sForceNotificationFloor) NumState.INSTANCE.refreshRailFooter(notificationFloor, revision);
            else NumState.INSTANCE.restoreRailFooter();
        } else restoreContent();
        liftNativeClock();
        note(next == null ? "insufficient room for fixed rail"
                : retained ? aodFrozen ? "fixed through AOD/wake" : "fixed Duo geometry"
                : "fixed Duo geometry initialized");
        if (wasActive != active() || wasContent != reflowing()) {
            MiniPlayerRuntime.refresh();
            Main.refreshCoverGeometry();
        }
    }

    private static void note(String value) {
        if (value.equals(reason)) return;
        reason = value;
        events.addLast(android.os.SystemClock.uptimeMillis() + " " + value);
        while (events.size() > 24) events.removeFirst();
    }

    private static RightRailLayout.Plan planForWindow() {
        Insets safe = RailWindowInsets.read(root);
        return RightRailLayout.plan(root.getWidth(), root.getHeight(),
                root.getResources().getDisplayMetrics().density,
                safe.left, safe.top, safe.right, safe.bottom, MiniPlayerRuntime.railIndicatorExtent());
    }

    static boolean holdGeometry() {
        if (root == null) return false;
        if (keyguardCanvas == null || !keyguardCanvas.isAttachedToWindow()) {
            int id = root.getResources().getIdentifier("keyguard_root_view", "id", "com.android.systemui");
            keyguardCanvas = id == 0 ? null : root.findViewById(id);
        }
        float scale = keyguardCanvas == null ? 1f : keyguardCanvas.getScaleX();
        if (keyguardCanvas != null && map(keyguardCanvas, root,
                new FoldMusicLayout.Box(0f, 0f, 100f, 100f), box)) scale = box.width() / 100f;
        return RailLifecycle.freeze(RailLifecycle.retain(sessionEligible, plan != null,
                        planRevision, Main.screenGeometry().revision), Main.screenOnCached(),
                MiniPlayerScene.INSTANCE.getAodActive(), scale);
    }

    /** Status, capsules and shortcut compensation share one AOD/wake ownership latch. */
    static boolean holdRailX() {
        railXPinned = RailLifecycle.holdColumnX(railXPinned, active(),
                MiniPlayerScene.INSTANCE.getKeyguardGoingAway(), Main.screenOnCached(),
                MiniPlayerScene.INSTANCE.getAodActive(), holdGeometry());
        return railXPinned;
    }

    static boolean controlReference(View host, float[] out) {
        RightRailLayout.Plan p = plan;
        if (!active() || p == null || out == null || out.length < 3
                || !map(root, host, new FoldMusicLayout.Box(p.controlsX - 1f,
                    p.controlsY - p.controlsSpan / 2f, p.controlsX + 1f,
                    p.controlsY + p.controlsSpan / 2f), box)) return false;
        out[0] = box.centerX(); out[1] = box.centerY(); out[2] = box.height();
        return true;
    }

    static FoldMusicLayout.Box artworkOnScreen(View view, CoverCardStyle.Rect rect) {
        if (!reflowing() || !map(view, root, new FoldMusicLayout.Box(rect.x, rect.y,
                rect.x + rect.side, rect.y + rect.side), box)) return null;
        return rect(box);
    }

    static CoverCardStyle.Rect artworkFromScreen(View view, FoldMusicLayout.Box rect) {
        if (!reflowing() || rect == null || !map(root, view, rect, box)) return null;
        return new CoverCardStyle.Rect(box.left, box.top, box.width());
    }

    static FoldMusicLayout.Box aodArtwork(FoldMusicLayout.Box fallback, float clock, float media, float aspect) {
        RightRailLayout.Plan p = plan;
        if (!reflowing() || p == null || root == null) return fallback;
        int[] origin = new int[2]; root.getLocationOnScreen(origin);
        return RailAodArtwork.fit(p.content, fallback, clock - origin[1], media - origin[1],
                root.getResources().getDisplayMetrics().density, aspect);
    }

    private static void cacheNotificationFloor() {
        WindowInsets wi = root.getRootWindowInsets();
        if (wi == null || wi.isVisible(WindowInsets.Type.ime())) { notificationFloor = 0; return; }
        Insets safe = wi.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()
                | WindowInsets.Type.displayCutout() | WindowInsets.Type.mandatorySystemGestures());
        notificationFloor = RailNotificationFloor.safeBottom(root.getHeight(), safe.bottom,
                root.getResources().getDisplayMetrics().density);
    }

    static int notificationBottom(int nativeBottom) {
        return reflowing() && Main.sForceNotificationFloor && Main.keyguardLocked()
                ? RailNotificationFloor.extend(nativeBottom, notificationFloor) : nativeBottom;
    }

    private static void centerClocks() {
        if (ClockCollapse.compactRowMoving()) return;
        if (ClockCollapse.active() && !Main.anchoredStyle()) return;
        RectF ink = Main.glyphBox();
        if (ink == null) return;
        for (View root : Main.clockRoots()) {
            View target = Main.clockTarget(root);
            if (target != null && target.isShown()) centerClock(target, ink);
        }
    }

    static void centerClock(View target, RectF ink) {
        float x = clockTranslation(target, ink);
        if (!Float.isFinite(x)) return;
        Owned state = owned.get(target);
        if (state == null) { state = new Owned(target); owned.put(target, state); }
        state.lease.observe(target.getLayoutParams().width, target.getTranslationX());
        if (Math.abs(target.getTranslationX() - x) >= .25f) target.setTranslationX(x);
        state.lease.wrote(target.getLayoutParams().width, target.getTranslationX());
        // The OEM can keep 1168px glyph origins in a 954px group. Do not clip the newly
        // centered ink to the shifted group's old child bounds. Preserve all clip owners.
        View boundary = null;
        for (View clockRoot : Main.clockRoots()) {
            if (under(target, clockRoot)) { boundary = clockRoot; break; }
        }
        for (View v = target; v != root && v instanceof ViewGroup; v = v.getParent() instanceof View ? (View) v.getParent() : null) {
            ViewGroup group = (ViewGroup) v;
            if (!clipLeases.containsKey(group)) clipLeases.put(group,
                    (group.getClipChildren() ? 1 : 0) | (group.getClipToPadding() ? 2 : 0));
            if (group.getClipChildren()) group.setClipChildren(false);
            if (group.getClipToPadding()) group.setClipToPadding(false);
            if (v == boundary || v == Main.sContainer) break;
        }
    }

    /** Shared by the cover pose and its native handoff, in the target parent's coordinates. */
    static float clockTranslation(View target, RectF ink) {
        RightRailLayout.Plan p = plan;
        if (p == null || !Main.sReflowLockContent || !active() || ink == null || target == null
                || !(target.getParent() instanceof View)) return Float.NaN;
        float bias = clockOpticalOffset();
        FoldMusicLayout.Box area = bias == 0f ? p.content : new FoldMusicLayout.Box(
                p.content.left + bias, p.content.top, p.content.right + bias, p.content.bottom);
        if (!map(root, (View) target.getParent(), area, box)) return Float.NaN;
        return ClockHorizontalPlacement.translation(ink.left, ink.right, target.getPivotX(),
                target.getScaleX(), target.getLeft(), box.centerX());
    }

    private static float clockOpticalOffset() {
        return root == null ? 0f : ClockHorizontalPlacement.opticalOffset(root.getWidth(),
                root.getHeight(), root.getResources().getDisplayMetrics().density);
    }

    private static FoldMusicLayout.Box rect(RectF b) {
        return new FoldMusicLayout.Box(b.left, b.top, b.right, b.bottom);
    }

    private static boolean under(View child, View parent) {
        if (parent == null) return false;
        for (View v = child; v != null; v = v.getParent() instanceof View ? (View) v.getParent() : null) {
            if (v == parent) return true;
        }
        return false;
    }

    private static void centerContentView(View v, FoldMusicLayout.Box content) {
        if (!v.isAttachedToWindow() || v.getWidth() <= 0 || v.getLayoutParams() == null
                || !(v.getParent() instanceof View)) return;
        if (!map(root, (View) v.getParent(), content, box)) return;
        Owned state = owned.get(v);
        if (state == null) { state = new Owned(v); owned.put(v, state); }
        state.lease.observe(v.getLayoutParams().width, v.getTranslationX());
        float x = box.centerX() - v.getWidth() / 2f - v.getLeft();
        if (Math.abs(v.getTranslationX() - x) >= .25f) v.setTranslationX(x);
        state.lease.wrote(v.getLayoutParams().width, v.getTranslationX());
    }

    private static void restoreClockOffsets() {
        for (java.util.Map.Entry<View, ControlOffset> e : clockOffsets.entrySet()) e.getValue().restore(e.getKey());
        clockOffsets.clear();
    }

    private static void liftNativeClock() {
        if (!active() || ClockCollapse.active()) { restoreClockOffsets(); return; }
        java.util.ArrayList<View> targets = new java.util.ArrayList<>();
        for (View clockRoot : Main.clockRoots()) {
            View target = Main.clockTarget(clockRoot);
            if (target != null && target.isShown() && !targets.contains(target)) targets.add(target);
        }
        View date = Main.visibleDate();
        boolean dateInClock = false;
        for (View target : targets) if (under(date, target)) dateInClock = true;
        if (date != null && date.isShown() && !dateInClock && !targets.contains(date)) targets.add(date);
        float delta = nativeClockLift();
        for (View target : targets) {
            ControlOffset lease = clockOffsets.get(target);
            if (lease == null) { lease = new ControlOffset(target, delta); clockOffsets.put(target, lease); }
            lease.apply(target);
        }
    }

    /** The exit animation and the idle lease must land on the same clock pose. */
    static float nativeClockLift() {
        return active() && root != null ? -12f * root.getResources().getDisplayMetrics().density : 0f;
    }

    static void clockReleased() {
        if (!active()) return;
        centerClocks();
        liftNativeClock();
    }

    private static void restore() {
        NumState.INSTANCE.restoreRailFooter();
        restoreClockOffsets();
        restoreContent();
    }

    private static void restoreContent() {
        RailStackBounds.release();
        for (java.util.Map.Entry<View, Owned> e : owned.entrySet()) {
            View v = e.getKey(); GeometryLease lease = e.getValue().lease;
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            if (lp != null) {
                int restored = lease.restoreWidth(lp.width);
                if (restored != lp.width) { lp.width = restored; v.setLayoutParams(lp); }
            }
            float x = lease.restoreX(v.getTranslationX());
            if (v.getTranslationX() != x) v.setTranslationX(x);
        }
        owned.clear();
        for (java.util.Map.Entry<ViewGroup, Integer> e : clipLeases.entrySet()) {
            ViewGroup v = e.getKey();
            if (!v.getClipChildren() && (e.getValue() & 1) != 0) v.setClipChildren(true);
            if (!v.getClipToPadding() && (e.getValue() & 2) != 0) v.setClipToPadding(true);
        }
        clipLeases.clear();
        contentActive = false;
        notificationFloor = 0;
    }

    private static boolean map(View from, View to, FoldMusicLayout.Box region, RectF out) {
        if (from == null || to == null || !to.isAttachedToWindow()) return false;
        matrix.reset(); from.transformMatrixToGlobal(matrix);
        inverse.reset(); to.transformMatrixToGlobal(inverse);
        if (!inverse.invert(inverse)) return false;
        inverse.preConcat(matrix);
        out.set(region.left, region.top, region.right, region.bottom);
        inverse.mapRect(out);
        return FoldMusicLayout.finite(out.left, out.top, out.right, out.bottom) && out.width() > 0;
    }

    static boolean pill(View host, RectF out) {
        RightRailLayout.Plan p = plan;
        return active() && p != null && map(root, host, p.pill, out);
    }
    static boolean companion(View host, RectF out) {
        RightRailLayout.Plan p = plan;
        return active() && p != null && map(root, host, p.companion, out);
    }
    static boolean strip(View host, RectF out) {
        RightRailLayout.Plan p = plan;
        return active() && p != null && map(root, host, p.strip, out);
    }
    static boolean content(View host, RectF out) {
        RightRailLayout.Plan p = plan;
        return reflowing() && p != null && map(root, host, p.content, out);
    }
    /** Empty space beside the rail follows content gestures; actual controls claim their own hits. */
    static boolean contentSwipeArea(View host, RectF out) {
        RightRailLayout.Plan p = plan;
        if (!reflowing() || p == null || root == null) return false;
        return map(root, host, new FoldMusicLayout.Box(p.content.left, p.content.top,
                root.getWidth(), p.content.bottom), out);
    }
    static float contentWidth() {
        RightRailLayout.Plan p = plan;
        return reflowing() && p != null ? p.content.right : Main.screenWidth();
    }
    static float clockCenterX() {
        RightRailLayout.Plan p = plan;
        return reflowing() && p != null
                ? (p.content.left + p.content.right) / 2 + clockOpticalOffset()
                : Main.screenWidth() / 2f;
    }
    static CoverCardStyle.Rect artwork(View view, CoverCardStyle style, float clock,
                                       float media, float aspect) {
        float density = view.getResources().getDisplayMetrics().density;
        if (!content(view, box)) return style.place(view.getWidth(), view.getHeight(), density, clock, media, aspect);
        float left = box.left, width = box.width();
        CoverCardStyle.Rect r = style.place(width, view.getHeight(), density, clock, media, aspect);
        return r == null ? null : new CoverCardStyle.Rect(r.x + left, r.y, r.side);
    }
    static String describe() {
        RightRailLayout.Plan p = plan;
        return "rail=" + active() + " reflow=" + reflowing() + " reason=" + reason
                + (p == null ? "" : " content=" + p.content.left + ".." + p.content.right
                    + " pill=" + p.pill.left + "," + p.pill.top + "," + p.pill.width() + "x" + p.pill.height()
                    + " controlsSpan=" + p.controlsSpan + " offsets=" + p.firstOffsetY + "," + p.secondOffsetY)
                + " stackBounds=[" + RailStackBounds.describe() + "]";
    }

    static String events() { return String.join(" ; ", events); }
}
