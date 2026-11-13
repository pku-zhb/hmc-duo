package com.zhuhuibin.hypermusiccover.duo;

import android.view.View;
import android.view.ViewGroup;
import java.util.IdentityHashMap;

/** A lock-screen-only height lease. No width, scale, artwork size or control height is written. */
final class CompactPlayerRuntime {
    private static View card;
    private static final IdentityHashMap<View, Lease> owned = new IdentityHashMap<>();
    private static View background, album, title, seamless, action, seek;
    private static int appliedPercent = 100;

    private static final class Lease {
        int height, heightWritten, top, topWritten, bottom, bottomWritten, minimum, minimumWritten;
        Lease(View view) {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            height = heightWritten = params.height;
            minimum = minimumWritten = view.getMinimumHeight();
            if (params instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams margins = (ViewGroup.MarginLayoutParams) params;
                top = topWritten = margins.topMargin;
                bottom = bottomWritten = margins.bottomMargin;
            }
        }
        void observe(View view) {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            if (params.height != heightWritten) height = params.height;
            if (view.getMinimumHeight() != minimumWritten) minimum = view.getMinimumHeight();
            if (params instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams margins = (ViewGroup.MarginLayoutParams) params;
                if (margins.topMargin != topWritten) top = margins.topMargin;
                if (margins.bottomMargin != bottomWritten) bottom = margins.bottomMargin;
            }
        }
        void apply(View view, int nextHeight, int nextTop, int nextBottom, int nextMinimum) {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            boolean changed = params.height != nextHeight;
            params.height = heightWritten = nextHeight;
            topWritten = nextTop; bottomWritten = nextBottom;
            if (params instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams margins = (ViewGroup.MarginLayoutParams) params;
                changed |= margins.topMargin != nextTop || margins.bottomMargin != nextBottom;
                margins.topMargin = nextTop; margins.bottomMargin = nextBottom;
            }
            if (changed) view.setLayoutParams(params);
            minimumWritten = nextMinimum;
            if (view.getMinimumHeight() != nextMinimum) view.setMinimumHeight(nextMinimum);
        }
        void restore(View view) {
            if (view.getLayoutParams() == null) return;
            observe(view);
            apply(view, height, top, bottom, minimum);
        }
    }

    private static Lease lease(View view) {
        Lease value = owned.get(view);
        if (value == null) { value = new Lease(view); owned.put(view, value); }
        else value.observe(view);
        return value;
    }

    private static View named(View parent, String name) {
        int id = parent.getResources().getIdentifier(name, "id", "com.android.systemui");
        return id == 0 ? null : parent.findViewById(id);
    }

    private static boolean ready(View view) {
        return view != null && view.isAttachedToWindow() && view.getLayoutParams() != null;
    }

    static void update(View root) {
        if (!Main.keyguardLocked() || !Main.onKeyguardNow()) { restore(); return; }
        if (MiniPlayerScene.INSTANCE.getKeyguardGoingAway()) return;
        View header = Main.miniPlayerMediaHeader();
        View next = header == null ? Main.findLockScreenView("mi_media_controls") : named(header, "mi_media_controls");
        if (!ready(next)) { restore(); return; }
        if (next != card) {
            restore(); card = next;
            resolveChildren();
        }
        float density = root.getResources().getDisplayMetrics().density;
        int percent = MediaCardRuntime.heightPercent(CompactPlayerMetrics.innerLandscape(
                root.getWidth(), root.getHeight(), density));
        if (percent == 100) { restoreProperties(); return; }
        if (!ready(background) || !ready(album) || !ready(action) || !ready(seek)) {
            restoreProperties();
            resolveChildren();
            if (!ready(background) || !ready(album) || !ready(action) || !ready(seek)) return;
        }
        Lease bg = lease(background), art = lease(album), controls = lease(action), track = lease(seek);
        // The retained normal constraint set has fixed native rows and a fixed background.
        // Unknown/match-constraint variants remain native rather than guessing a measured size.
        if (bg.height <= 0 || art.height <= 0 || controls.height <= 0 || track.height <= 0) {
            restoreProperties(); return;
        }
        int controlHeight = art.height + controls.height + track.height
                + card.getPaddingTop() + card.getPaddingBottom();
        CompactPlayerMetrics.Plan plan = CompactPlayerMetrics.plan(bg.height, percent,
                art.top, controls.top, track.top, track.bottom, controlHeight, density);
        int topShift = art.top - plan.top;
        bg.apply(background, plan.height, bg.top, bg.bottom, bg.minimum);
        art.apply(album, art.height, plan.top, art.bottom, art.minimum);
        controls.apply(action, controls.height, plan.action, controls.bottom, controls.minimum);
        track.apply(seek, track.height, plan.seekTop, plan.seekBottom, track.minimum);
        moveHeaderTop(title, topShift);
        moveHeaderTop(seamless, topShift);
        Lease container = lease(card);
        // WRAP_CONTENT remains native. Explicit height/minimum follow the safely reduced shell.
        int height = container.height > 0 ? Math.max(plan.height,
                container.height - (bg.height - plan.height)) : container.height;
        container.apply(card, height, container.top, container.bottom,
                Math.min(container.minimum, plan.height));
        appliedPercent = percent;
    }

    private static void resolveChildren() {
        background = named(card, "media_bg"); album = named(card, "album_art");
        title = named(card, "header_title"); seamless = named(card, "media_seamless");
        action = named(card, "action0"); seek = named(card, "media_progress_bar");
    }

    private static void moveHeaderTop(View view, int shift) {
        if (!ready(view)) return;
        Lease value = lease(view);
        value.apply(view, value.height, value.top - shift, value.bottom, value.minimum);
    }

    private static void restoreProperties() {
        for (java.util.Map.Entry<View, Lease> entry : owned.entrySet()) entry.getValue().restore(entry.getKey());
        owned.clear();
        appliedPercent = 100;
    }

    static View currentCard() { return card; }
    static void restore() {
        restoreProperties();
        card = background = album = title = seamless = action = seek = null;
    }
    static String describe() { return "playerHeight=" + appliedPercent + "% leases=" + owned.size(); }
    private CompactPlayerRuntime() {}
}
