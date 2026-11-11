package com.zhuhuibin.hypermusiccover.duo;

import android.graphics.Rect;
import android.view.View;
import android.widget.TextView;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/** Reserve real text-layout width and native control hit priority without changing preferences. */
final class MediaTextInset {
    private static final WeakHashMap<TextView, Padding> saved = new WeakHashMap<>();
    private static final WeakHashMap<View, List<Front>> fronts = new WeakHashMap<>();
    private static boolean layoutChanged;
    private static final class Padding {
        int left, right, writtenLeft, writtenRight;
        Rect clip, writtenClip;
        Padding(TextView view) {
            left = writtenLeft = view.getPaddingLeft(); right = writtenRight = view.getPaddingRight();
            clip = copy(view.getClipBounds()); writtenClip = copy(clip);
        }
    }
    private static final class Front {
        final WeakReference<View> view;
        float base, written;
        Front(View view) { this.view = new WeakReference<>(view); base = written = view.getTranslationZ(); }
    }
    private MediaTextInset() {}
    static void update(TextView view, float inset) {
        if (view == null) return;
        if (inset <= 0f) {
            Padding old = saved.remove(view);
            if (old != null) {
                int left = view.getPaddingLeft() == old.writtenLeft ? old.left : view.getPaddingLeft();
                int right = view.getPaddingRight() == old.writtenRight ? old.right : view.getPaddingRight();
                write(view, left, right);
                if (Objects.equals(view.getClipBounds(), old.writtenClip)) view.setClipBounds(old.clip);
            }
        }
    }
    static void update(View card, TextView view, float inset, List<View> controls) {
        if (view == null) return;
        Padding old = saved.get(view);
        if (old == null) { old = new Padding(view); saved.put(view, old); }
        else {
            if (view.getPaddingLeft() != old.writtenLeft) old.left = view.getPaddingLeft();
            if (view.getPaddingRight() != old.writtenRight) old.right = view.getPaddingRight();
            if (!Objects.equals(view.getClipBounds(), old.writtenClip)) old.clip = copy(view.getClipBounds());
        }
        MediaTextBoundsPolicy.Bounds text = bounds(card, view);
        if (text == null || view.getWidth() <= 0) return;
        List<MediaTextBoundsPolicy.Bounds> obstacles = new ArrayList<>();
        for (View control : controls) {
            if (!drawn(control)) continue;
            MediaTextBoundsPolicy.Bounds box = bounds(card, control);
            if (box != null) obstacles.add(box);
        }
        MediaTextBoundsPolicy.Insets target = MediaTextBoundsPolicy.reserve(text, old.left, old.right, inset, obstacles);
        write(view, target.left, target.right);
        old.writtenLeft = target.left; old.writtenRight = target.right;
        // Clamp this very draw too: a native layout that keeps rewriting padding cannot
        // force us to block drawing forever just to keep text out of the output button.
        Rect clip = new Rect(target.left, 0, Math.max(target.left, view.getWidth() - target.right), view.getHeight());
        if (old.clip != null && !clip.intersect(old.clip)) clip.setEmpty();
        if (!Objects.equals(view.getClipBounds(), clip)) view.setClipBounds(clip);
        old.writtenClip = copy(clip);
    }
    private static Rect copy(Rect rect) { return rect == null ? null : new Rect(rect); }
    private static void write(TextView view, int left, int right) {
        if (view.getPaddingLeft() == left && view.getPaddingRight() == right) return;
        view.setPadding(left, view.getPaddingTop(), right, view.getPaddingBottom());
        layoutChanged = true;
    }
    static boolean consumeLayoutChange() {
        boolean changed = layoutChanged; layoutChanged = false; return changed;
    }
    private static boolean drawn(View view) {
        if (view == null || !view.isShown() || view.getWidth() <= 0 || view.getHeight() <= 0) return false;
        float alpha = 1f;
        for (View node = view; node != null;
             node = node.getParent() instanceof View ? (View) node.getParent() : null) {
            alpha *= node.getAlpha() * node.getTransitionAlpha();
            if (alpha <= .01f) return false;
        }
        return true;
    }
    static void prioritize(View card, TextView title, TextView artist, List<View> controls) {
        IdentityHashMap<View, Float> wanted = new IdentityHashMap<>();
        for (View control : controls) {
            if (!drawn(control)) continue;
            View clickable = control;
            while (clickable != null && clickable != card && (!clickable.isEnabled() || !clickable.isClickable()))
                clickable = clickable.getParent() instanceof View ? (View) clickable.getParent() : null;
            if (clickable == null || clickable == card || !drawn(clickable)) continue;
            MediaTextBoundsPolicy.Bounds obstacle = bounds(card, clickable);
            for (TextView text : new TextView[]{title, artist}) {
                MediaTextBoundsPolicy.Bounds textBox = text == null ? null : bounds(card, text);
                if (obstacle == null || textBox == null || !obstacle.intersects(textBox)) continue;
                View branch = clickable;
                while (branch.getParent() instanceof View && branch.getParent() != card
                        && !contains((View) branch.getParent(), text)) branch = (View) branch.getParent();
                View parent = branch.getParent() instanceof View ? (View) branch.getParent() : null;
                if (parent == null || contains(branch, text)) continue;
                View textBranch = text;
                while (textBranch.getParent() instanceof View && textBranch.getParent() != parent)
                    textBranch = (View) textBranch.getParent();
                if (textBranch.getParent() != parent) continue;
                // A subnormal nextUp(0) can be flushed to zero in native rendering. This
                // depth-only bias keeps native hit sorting strict without a visible lift.
                float above = Math.max(Math.nextUp(textBranch.getZ()), textBranch.getZ() + .01f);
                float target = above - branch.getElevation();
                wanted.put(branch, Math.max(wanted.getOrDefault(branch, Float.NEGATIVE_INFINITY), target));
            }
        }
        List<Front> records = fronts.get(card);
        if (records == null) { records = new ArrayList<>(); fronts.put(card, records); }
        for (int i = records.size() - 1; i >= 0; i--) {
            Front saved = records.get(i); View view = saved.view.get();
            if (view == null || !wanted.containsKey(view)) {
                if (view != null && view.getTranslationZ() == saved.written) view.setTranslationZ(saved.base);
                records.remove(i);
            }
        }
        for (Map.Entry<View, Float> entry : wanted.entrySet()) {
            View view = entry.getKey(); Front record = null;
            for (Front item : records) if (item.view.get() == view) { record = item; break; }
            if (record == null) { record = new Front(view); records.add(record); }
            else if (view.getTranslationZ() != record.written) record.base = view.getTranslationZ();
            float next = Math.max(record.base, entry.getValue());
            if (view.getTranslationZ() != next) view.setTranslationZ(next);
            record.written = next;
        }
    }
    static void releaseControls(View card) {
        List<Front> records = fronts.remove(card);
        if (records == null) return;
        for (Front record : records) {
            View view = record.view.get();
            if (view != null && view.getTranslationZ() == record.written) view.setTranslationZ(record.base);
        }
    }
    private static boolean contains(View parent, View child) {
        while (child != null && child != parent)
            child = child.getParent() instanceof View ? (View) child.getParent() : null;
        return child == parent;
    }
    private static MediaTextBoundsPolicy.Bounds bounds(View card, View view) {
        float x = 0f, y = 0f;
        View node = view;
        while (node != null && node != card) {
            x += node.getLeft() + node.getTranslationX(); y += node.getTop() + node.getTranslationY();
            node = node.getParent() instanceof View ? (View) node.getParent() : null;
        }
        return node == card ? new MediaTextBoundsPolicy.Bounds(x, y, x + view.getWidth(), y + view.getHeight()) : null;
    }
}
