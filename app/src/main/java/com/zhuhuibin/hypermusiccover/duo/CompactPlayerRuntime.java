package com.zhuhuibin.hypermusiccover.duo;

import android.view.View;
import android.view.ViewGroup;
import java.util.IdentityHashMap;

/** Remeasure the inner-screen player with tighter vertical rows; preserve its full column width. */
final class CompactPlayerRuntime {
    private static View card;
    private static final IdentityHashMap<View, Lease> owned = new IdentityHashMap<>();
    private static final class Lease {
        int w, h, wt, ht, pt, pb, pwt, pwb, mt, mb, mwt, mwb, min, minWritten;
        Lease(View v) {
            ViewGroup.LayoutParams p = v.getLayoutParams();
            w = wt = p.width; h = ht = p.height;
            pt = pwt = v.getPaddingTop(); pb = pwb = v.getPaddingBottom();
            min = minWritten = v.getMinimumHeight();
            if (p instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams m = (ViewGroup.MarginLayoutParams) p;
                mt = mwt = m.topMargin; mb = mwb = m.bottomMargin;
            }
        }
        void observe(View v) {
            ViewGroup.LayoutParams p = v.getLayoutParams();
            if (p.width != wt) w = p.width;
            if (p.height != ht) h = p.height;
            if (v.getPaddingTop() != pwt) pt = v.getPaddingTop();
            if (v.getPaddingBottom() != pwb) pb = v.getPaddingBottom();
            if (v.getMinimumHeight() != minWritten) min = v.getMinimumHeight();
            if (p instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams m = (ViewGroup.MarginLayoutParams) p;
                if (m.topMargin != mwt) mt = m.topMargin;
                if (m.bottomMargin != mwb) mb = m.bottomMargin;
            }
        }
        void apply(View v, int kind, float d) {
            observe(v);
            ViewGroup.LayoutParams p = v.getLayoutParams();
            wt = kind == 1 ? CompactPlayerMetrics.cap(w, 44f, d) : w;
            ht = kind == 1 ? CompactPlayerMetrics.cap(h, 44f, d)
                    : kind == 2 ? CompactPlayerMetrics.cap(h, 32f, d)
                    : kind == 3 ? CompactPlayerMetrics.cap(h, 48f, d) : h;
            pwt = kind == 2 || kind == 3 ? CompactPlayerMetrics.trackPadding(h, pt, ht)
                    : kind == 0 ? Math.max(0, pt - Math.round(4f * d)) : pt;
            pwb = kind == 2 || kind == 3 ? CompactPlayerMetrics.trackPadding(h, pb, ht)
                    : kind == 0 ? Math.max(0, pb - Math.round(4f * d)) : pb;
            mwt = kind == 2 ? CompactPlayerMetrics.gap(mt) : mt;
            mwb = kind == 2 ? CompactPlayerMetrics.gap(mb) : mb;
            boolean layout = p.width != wt || p.height != ht;
            p.width = wt; p.height = ht;
            if (p instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams m = (ViewGroup.MarginLayoutParams) p;
                layout |= m.topMargin != mwt || m.bottomMargin != mwb;
                m.topMargin = mwt; m.bottomMargin = mwb;
            }
            if (layout) v.setLayoutParams(p);
            if (v.getPaddingTop() != pwt || v.getPaddingBottom() != pwb)
                v.setPadding(v.getPaddingLeft(), pwt, v.getPaddingRight(), pwb);
            minWritten = kind == 0 ? CompactPlayerMetrics.cardMinimum(min, d) : min;
            if (v.getMinimumHeight() != minWritten) v.setMinimumHeight(minWritten);
        }
        void restore(View v) {
            ViewGroup.LayoutParams p = v.getLayoutParams();
            if (p == null) return;
            boolean changed = false;
            if (p.width == wt && p.width != w) { p.width = w; changed = true; }
            if (p.height == ht && p.height != h) { p.height = h; changed = true; }
            if (p instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams m = (ViewGroup.MarginLayoutParams) p;
                if (m.topMargin == mwt && m.topMargin != mt) { m.topMargin = mt; changed = true; }
                if (m.bottomMargin == mwb && m.bottomMargin != mb) { m.bottomMargin = mb; changed = true; }
            }
            if (changed) v.setLayoutParams(p);
            int top = v.getPaddingTop() == pwt ? pt : v.getPaddingTop();
            int bottom = v.getPaddingBottom() == pwb ? pb : v.getPaddingBottom();
            if (top != v.getPaddingTop() || bottom != v.getPaddingBottom())
                v.setPadding(v.getPaddingLeft(), top, v.getPaddingRight(), bottom);
            if (v.getMinimumHeight() == minWritten && min != minWritten) v.setMinimumHeight(min);
        }
    }
    private static void apply(View v, int kind, float d) {
        if (v == null || v.getLayoutParams() == null) return;
        Lease lease = owned.get(v);
        if (lease == null) { lease = new Lease(v); owned.put(v, lease); }
        lease.apply(v, kind, d);
    }
    private static View named(View parent, String name) {
        int id = parent.getResources().getIdentifier(name, "id", "com.android.systemui");
        return id == 0 ? null : parent.findViewById(id);
    }
    static void update(View root) {
        float d = root.getResources().getDisplayMetrics().density;
        boolean inner = d > 0 && Math.min(root.getWidth(), root.getHeight()) / d >= 600f;
        if (!inner || !Main.keyguardLocked() || !Main.onKeyguardNow()) { restore(); return; }
        if (MiniPlayerScene.INSTANCE.getKeyguardGoingAway()) return;
        View header = Main.miniPlayerMediaHeader();
        View next = header == null ? Main.findLockScreenView("mi_media_controls") : named(header, "mi_media_controls");
        if (next == null || !next.isAttachedToWindow()) { restore(); return; }
        if (next != card) { restore(); card = next; }
        apply(card, 0, d);
        apply(named(card, "album_art"), 1, d);
        apply(named(card, "album_art_image"), 1, d);
        apply(named(card, "media_progress_bar"), 2, d);
        for (int i = 0; i < 5; i++) apply(named(card, "action" + i), 3, d);
    }
    static View currentCard() { return card; }
    static void restore() {
        for (java.util.Map.Entry<View, Lease> e : owned.entrySet()) e.getValue().restore(e.getKey());
        owned.clear(); card = null;
    }
    static String describe() { return "innerCompact=" + (card != null) + " leases=" + owned.size(); }
    private CompactPlayerRuntime() {}
}
