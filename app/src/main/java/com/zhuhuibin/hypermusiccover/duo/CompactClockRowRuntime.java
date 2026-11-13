package com.zhuhuibin.hypermusiccover.duo;

import android.graphics.Paint;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Map;
import java.util.WeakHashMap;

/** Only the time is replaced. The date remains the OEM's live view, including its text and color. */
final class CompactClockRowRuntime {
    private CompactClockRowRuntime() {}
    private static ViewGroup host;
    private static CompactClockRowView time;
    private static final Map<View, Alpha> clocks = new WeakHashMap<>();
    private static final Map<View, DatePose> dates = new WeakHashMap<>();
    private static final float[] POINTS = new float[6];
    private static String reason = "not requested";
    private static String dateDrawing = "unavailable";
    private static Plan lastPlan;
    private static float drawnProgress;
    private static java.lang.ref.WeakReference<View> inspectedDate = new java.lang.ref.WeakReference<>(null);

    static boolean requested() {
        ScreenGeometry s = Main.screenGeometry();
        return CompactClockRowLayout.requested(Main.sArtworkClockDestination == ArtworkClockDestination.DATE_ROW, s.width, s.height,
                s.densityDpi / 160f,
                ArtworkClockDestination.sceneRequested(Main.coverModeOn(), LockLyrics.playerChosen(),
                        LockLyrics.notificationCompact(), ImmersiveHost.navigationClock()), false, false)
                && (!ImmersiveHost.holdsClock() || ImmersiveHost.navigationClock());
    }

    static boolean supportsDisplay() {
        ScreenGeometry s = Main.screenGeometry();
        return CompactClockRowLayout.supportsDisplay(s.width, s.height, s.densityDpi / 160f);
    }

    /** Never fade a clock target that also owns the date or hide an unresolved time face. */
    static boolean statusSupported() {
        View date = Main.visibleDate();
        if (date == null) return false;
        boolean found = false;
        for (View root : Main.clockRoots()) {
            View target = Main.clockTarget(root);
            if (target == null) continue;
            if (descendant(date, target)) return false;
            found = true;
        }
        return found;
    }

    /** Clock discovery still measures a face hidden only by our replacement-time crossfade. */
    static float measurementAlpha(View view) {
        Alpha lease = clocks.get(view);
        return lease != null && view.getAlpha() == lease.written ? lease.base : view.getAlpha();
    }

    static final class Plan {
        final View date;
        final TextView text;
        final CompactClockRowLayout.Row row;
        final float inkLeft, baseline, baseLeft, baseScaleX, top, bottom;
        Plan(View date, TextView text, CompactClockRowLayout.Row row, float inkLeft,
             float baseline, float baseLeft, float baseScaleX, float top, float bottom) {
            this.date = date; this.text = text; this.row = row; this.inkLeft = inkLeft;
            this.baseline = baseline; this.baseLeft = baseLeft; this.baseScaleX = baseScaleX;
            this.top = top; this.bottom = bottom;
        }
    }

    /** A missing/custom date face keeps its existing clock rather than losing both times. */
    static Plan prepare(View date, float targetDateTop) {
        if (date != null && inspectedDate.get() != date) inspectedDate = new java.lang.ref.WeakReference<>(date);
        if (date == null || !Float.isFinite(targetDateTop)) return unavailable("date unavailable");
        // Non-anchored faces have a companion solver that owns leaf transforms and may put
        // date/weekday in several nested nodes. Preserve that layout until it has its own adapter.
        if (!Main.anchoredStyle()) return unavailable("non-anchored date layout");
        TextView text = text(date);
        if (text == null) return unavailable("date has no TextView");
        CompactClockRowDate.Measurement drawing = CompactClockRowDate.read(text);
        dateDrawing = drawing.description;
        if (drawing.ink == null) return unavailable(drawing.description);
        for (View root : Main.clockRoots()) {
            View target = Main.clockTarget(root);
            if (target != null && descendant(date, target)) return unavailable("date inside time target");
        }
        View window = date.getRootView();
        int id = ResourceIds.id(window.getResources(), "keyguard_root_view");
        View candidate = id == 0 ? null : window.findViewById(id);
        if (!(candidate instanceof ViewGroup) || !descendant(date, candidate))
            return unavailable("keyguard ancestor unavailable");
        if (host != candidate || time == null) {
            detach();
            host = (ViewGroup) candidate;
            time = new CompactClockRowView(host.getContext());
            time.setVisibility(View.INVISIBLE);
            host.getOverlay().add(time);
        }
        float localX = drawing.ink.left;
        float localY = drawing.ink.baseline;
        for (View v = text; v != date;) {
            localX += v.getLeft() + v.getTranslationX();
            localY += v.getTop() + v.getTranslationY();
            if (!(v.getParent() instanceof View)) return unavailable("date detached");
            v = (View) v.getParent();
        }
        float width = drawing.ink.width;
        DatePose datePose = dates.get(date);
        float nativeSx = datePose == null ? date.getScaleX() : datePose.nativeScaleX(date);
        float nativeSy = datePose == null ? date.getScaleY() : datePose.nativeScaleY(date);
        if (!(nativeSx > 0f) || !(nativeSy > 0f) || !Float.isFinite(nativeSx + nativeSy))
            return unavailable("date scale unavailable");
        float d = Main.density();
        CompactClockRowLayout.Row row = CompactClockRowLayout.place(16f * d,
                LockScreenRailRuntime.contentWidth() - 16f * d,
                time.timeWidth(text.getPaint()) * nativeSx, width * nativeSx,
                8f * d, Main.sArtworkClockBeforeDate);
        if (row == null) return unavailable("row dimensions unavailable");
        float baseline = targetDateTop + localY;
        reason = "ready";
        lastPlan = new Plan(date, text, row, localX, localY, originX(date) - date.getTranslationX(),
                nativeSx, baseline + drawing.top * nativeSy * row.scale,
                baseline + drawing.bottom * nativeSy * row.scale);
        return lastPlan;
    }

    private static Plan unavailable(String why) { reason = why; lastPlan = null; return null; }

    static void apply(Plan p, float progress) {
        if (p == null || !(progress > 0f) || !supportsDisplay() || !Main.keyguardLocked()
                || MiniPlayerScene.INSTANCE.getKeyguardGoingAway() || Main.bouncerShown()
                || MiniPlayerScene.INSTANCE.getCustomAodActive()
                || MiniPlayerScene.INSTANCE.getBlocksMiniPlayer()) {
            clear();
            return;
        }
        float alpha = CompactClockRowLayout.alpha(progress);
        drawnProgress = progress;
        applyClockAlpha(1f - alpha);
        DatePose date = dates.get(p.date);
        if (date == null) { date = new DatePose(p.date); dates.put(p.date, date); }
        date.apply(p, progress);

        // Map the actual date baseline through its native ancestors. Both glyph sets therefore
        // ride the same AOD zoom and clock-container motion; no window-space compensation.
        POINTS[0] = p.inkLeft; POINTS[1] = p.baseline;
        POINTS[2] = p.inkLeft + 1f; POINTS[3] = p.baseline;
        POINTS[4] = p.inkLeft; POINTS[5] = p.baseline + 1f;
        float inheritedAlpha = 1f;
        for (View v = p.date; v != host;) {
            inheritedAlpha *= v.getVisibility() == View.VISIBLE ? v.getAlpha() : 0f;
            v.getMatrix().mapPoints(POINTS);
            POINTS[0] += v.getLeft(); POINTS[2] += v.getLeft(); POINTS[4] += v.getLeft();
            POINTS[1] += v.getTop(); POINTS[3] += v.getTop(); POINTS[5] += v.getTop();
            if (!(v.getParent() instanceof View)) { clear(); return; }
            v = (View) v.getParent();
            POINTS[0] -= v.getScrollX(); POINTS[2] -= v.getScrollX(); POINTS[4] -= v.getScrollX();
            POINTS[1] -= v.getScrollY(); POINTS[3] -= v.getScrollY(); POINTS[5] -= v.getScrollY();
        }
        float scaleX = Math.abs(POINTS[2] - POINTS[0]), scaleY = Math.abs(POINTS[5] - POINTS[1]);
        if (!(scaleX > 0f) || !(scaleY > 0f)) { time.hide(); return; }
        float x = POINTS[0] + (p.row.timeLeft - p.row.dateLeft) * scaleX / (p.row.scale * p.baseScaleX);
        time.style(p.text.getPaint(), scaleX, scaleY, p.text.getCurrentTextColor());
        Paint.FontMetrics metrics = time.ink.getFontMetrics();
        float y = POINTS[1] + metrics.top;
        int width = Math.max(1, (int) Math.ceil(time.timeWidth() + 2f));
        int height = Math.max(1, (int) Math.ceil(metrics.bottom - metrics.top + 2f));
        int left = (int) Math.floor(x), top = (int) Math.floor(y);
        if (time.getWidth() != width || time.getHeight() != height) {
            time.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        }
        if (time.getLeft() != left || time.getTop() != top
                || time.getWidth() != width || time.getHeight() != height)
            time.layout(left, top, left + width, top + height);
        time.setTranslationX(x - left); time.setTranslationY(y - top);
        time.show(alpha * inheritedAlpha, -metrics.top);
    }

    /** The status time crossfades in place; the native clock keeps its own geometry. */
    static void applyStatus(float alpha) {
        if (!(alpha > 0f)) { clear(); return; }
        if (time != null) time.hide();
        for (Map.Entry<View, DatePose> item : dates.entrySet()) item.getValue().restore(item.getKey());
        dates.clear();
        drawnProgress = 0f;
        applyClockAlpha(1f - alpha);
    }

    private static void applyClockAlpha(float alpha) {
        for (View root : Main.clockRoots()) {
            View g = Main.clockTarget(root);
            if (g == null) continue;
            Alpha lease = clocks.get(g);
            if (lease == null) { lease = new Alpha(g.getAlpha()); clocks.put(g, lease); }
            lease.apply(g, alpha);
        }
    }

    static void clear() {
        drawnProgress = 0f;
        if (time != null) time.hide();
        for (Map.Entry<View, Alpha> item : clocks.entrySet()) item.getValue().restore(item.getKey());
        clocks.clear();
        for (Map.Entry<View, DatePose> item : dates.entrySet()) item.getValue().restore(item.getKey());
        dates.clear();
    }

    static void detach() {
        clear();
        if (host != null && time != null) host.getOverlay().remove(time);
        host = null; time = null;
    }

    static String describe() {
        Plan p = lastPlan;
        return "requested=" + requested() + " active=" + (drawnProgress > 0f)
                + " p=" + Main.r2(drawnProgress) + " plan=" + reason
                + " side=" + (Main.sArtworkClockBeforeDate ? "left" : "right")
                + (p == null ? "" : " date=" + p.date.getClass().getSimpleName()
                    + "/" + p.text.getClass().getSimpleName()
                    + " row=[" + Main.r1(p.row.timeLeft) + "," + Main.r1(p.row.dateLeft)
                    + "," + Main.r1(p.top) + "," + Main.r1(p.bottom) + "]"
                    + " fit=" + Main.r2(p.row.scale))
                + (time == null ? "" : " time=[" + time.getLeft() + "," + time.getTop()
                    + "," + time.getWidth() + "," + time.getHeight() + "] a=" + Main.r2(time.getAlpha()))
                + " draw=[" + dateDrawing + "]"
                + " source=" + structure(inspectedDate.get());
    }

    /** Probe only: view classes and geometry, never date text or other lock-screen content. */
    private static String structure(View view) {
        if (view == null) return "none";
        StringBuilder out = new StringBuilder();
        Class<?> type = view.getClass();
        for (int i = 0; type != null && i < 7; i++, type = type.getSuperclass()) {
            if (i > 0) out.append('<');
            out.append(type.getSimpleName());
        }
        children(view, out, 0);
        return out.toString();
    }

    private static void children(View view, StringBuilder out, int depth) {
        if (!(view instanceof ViewGroup) || depth >= 2) return;
        ViewGroup group = (ViewGroup) view;
        out.append('{');
        for (int i = 0; i < Math.min(6, group.getChildCount()); i++) {
            View child = group.getChildAt(i);
            if (i > 0) out.append(',');
            out.append(child.getClass().getSimpleName()).append('[')
                    .append(child.getWidth()).append('x').append(child.getHeight()).append(']');
            children(child, out, depth + 1);
        }
        out.append('}');
    }

    private static TextView text(View v) {
        if (v instanceof TextView && (CompactClockRowDate.classic((TextView) v)
                || ((TextView) v).getText().length() > 0)) return (TextView) v;
        if (v instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) v;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child.getVisibility() != View.VISIBLE) continue;
                TextView found = text(child);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static boolean descendant(View view, View parent) {
        for (View v = view; v != null; v = v.getParent() instanceof View ? (View) v.getParent() : null)
            if (v == parent) return true;
        return false;
    }

    private static float originX(View view) {
        float x = 0f;
        for (View v = view; v != null; v = v.getParent() instanceof View ? (View) v.getParent() : null)
            x += v.getLeft() + v.getTranslationX();
        return x;
    }

    private static final class Alpha {
        float base, written;
        Alpha(float initial) { base = written = initial; }
        void apply(View v, float alpha) {
            if (v.getAlpha() != written) base = v.getAlpha();
            written = base * alpha;
            if (v.getAlpha() != written) v.setAlpha(written);
        }
        void restore(View v) { if (v.getAlpha() == written) v.setAlpha(base); }
    }

    private static final class DatePose {
        float x, sx, sy, px, py, writtenX, writtenSx, writtenSy, writtenPx, writtenPy;
        DatePose(View v) {
            x = writtenX = v.getTranslationX(); sx = v.getScaleX(); sy = v.getScaleY();
            writtenSx = sx; writtenSy = sy;
            px = writtenPx = v.getPivotX(); py = writtenPy = v.getPivotY();
        }
        float nativeScaleX(View v) { return v.getScaleX() == writtenSx ? sx : v.getScaleX(); }
        float nativeScaleY(View v) { return v.getScaleY() == writtenSy ? sy : v.getScaleY(); }
        void apply(Plan p, float progress) {
            View v = p.date;
            if (v.getTranslationX() != writtenX) x = v.getTranslationX();
            sx = nativeScaleX(v); sy = nativeScaleY(v);
            if (v.getPivotX() != writtenPx) px = v.getPivotX();
            if (v.getPivotY() != writtenPy) py = v.getPivotY();
            float originalInk = p.baseLeft + x + px + (p.inkLeft - px) * sx;
            float wantedInk = originalInk + (p.row.dateLeft - originalInk) * progress;
            float fit = 1f + (p.row.scale - 1f) * progress;
            writtenSx = sx * fit; writtenSy = sy * fit;
            writtenPx = px + (p.inkLeft - px) * progress;
            writtenPy = py + (p.baseline - py) * progress;
            writtenX = wantedInk - p.baseLeft - writtenPx - (p.inkLeft - writtenPx) * writtenSx;
            v.setPivotX(writtenPx); v.setPivotY(writtenPy);
            v.setScaleX(writtenSx); v.setScaleY(writtenSy);
            v.setTranslationX(writtenX);
        }
        void restore(View v) {
            if (v.getTranslationX() == writtenX) v.setTranslationX(x);
            if (v.getScaleX() == writtenSx) v.setScaleX(sx);
            if (v.getScaleY() == writtenSy) v.setScaleY(sy);
            if (v.getPivotX() == writtenPx) v.setPivotX(px);
            if (v.getPivotY() == writtenPy) v.setPivotY(py);
        }
    }
}
