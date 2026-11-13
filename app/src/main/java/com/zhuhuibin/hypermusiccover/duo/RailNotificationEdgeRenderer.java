package com.zhuhuibin.hypermusiccover.duo;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.RenderEffect;
import android.graphics.RuntimeShader;
import android.view.View;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Each native card keeps its own RenderNode, clipping and Z; only its effect gains a mask. */
final class RailNotificationEdgeRenderer {
    interface NativeEffect { Object set(RenderEffect effect) throws Throwable; }
    private static final int NATIVE = 0, MASK = 1, RESTORE = 2;
    private static final String SHADER = "uniform shader content;\n"
            + "uniform float edge; uniform float width; uniform float direction;\n"
            + "half4 main(float2 p) {\n"
            + " float d = (p.x - edge) * direction;\n"
            + " float t = clamp((d - 0.5) / max(width - 0.5, 0.5), 0.0, 1.0);\n"
            + " float a = t * t * (3.0 - 2.0 * t);\n"
            + " return content.eval(p) * half(a);\n}\n";
    private static final int MAX_EFFECTS = 384;

    private static final class Key {
        final int edgeHalf, widthHalf, direction;
        Key(float edge, float width, boolean left) {
            edgeHalf = Math.round(edge * 2f); widthHalf = Math.max(2, Math.round(width * 2f));
            direction = left ? 1 : -1;
        }
        @Override public int hashCode() { return (edgeHalf * 31 + widthHalf) * 31 + direction; }
        @Override public boolean equals(Object value) {
            if (!(value instanceof Key)) return false;
            Key other = (Key) value;
            return edgeHalf == other.edgeHalf && widthHalf == other.widthHalf && direction == other.direction;
        }
    }

    private static final class Card {
        final RailCardEffectLease<RenderEffect> lease;
        final Rect clip = new Rect();
        final RectF viewport = new RectF();
        final Matrix matrix = new Matrix(), inverse = new Matrix();
        final float[] values = new float[9];
        Key key;
        RenderEffect mask, composed, composedBase, composedMask;
        float translation;
        boolean retired;
        Card(RenderEffect nativeEffect) { lease = new RailCardEffectLease<>(nativeEffect); }
    }
    private static final class WriteScope {
        final View view;
        final RenderEffect requested;
        WriteScope(View view, RenderEffect requested) { this.view = view; this.requested = requested; }
    }

    // A value never references its View key. Active rows use this same weak index.
    private final WeakHashMap<View, Card> cards = new WeakHashMap<>();
    private final WeakHashMap<View, Card> moving = new WeakHashMap<>();
    private final WeakHashMap<Object, WeakReference<View>> nodes = new WeakHashMap<>();
    private final NativeMaterialCommitScope<WriteScope> scopes = new NativeMaterialCommitScope<>();
    private final Rect parentClip = new Rect();
    private final ThreadLocal<Boolean> actualRead = new ThreadLocal<>();
    private final LinkedHashMap<Key, RenderEffect> effects = new LinkedHashMap<Key, RenderEffect>(32, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Key, RenderEffect> entry) { return size() > MAX_EFFECTS; }
    };
    private final Method getter;
    private final Field renderNode;
    private final io.github.libxposed.api.XposedInterface.Invoker<?, Method> viewOrigin;
    private View owner;
    private RailNotificationEdge geometry;
    private int width, height;
    private float density;
    private RuntimeShader shader;
    private boolean unavailable;

    RailNotificationEdgeRenderer(Method getter, Field renderNode, Method effect) {
        this.getter = getter; this.renderNode = renderNode;
        viewOrigin = Xp.api().getInvoker(effect)
                .setType(io.github.libxposed.api.XposedInterface.Invoker.Type.ORIGIN);
    }

    /** Called at the precise ExpandableView constructor tail, before subclass initialization. */
    synchronized void born(View view) {
        if (!cards.containsKey(view)) cards.put(view, new Card(null));
        try { nodes.put(renderNode.get(view), new WeakReference<>(view)); }
        catch (Throwable error) { failed(error); }
    }

    synchronized Object nativeRead(View view, Object value) {
        Card card = cards.get(view);
        return actualRead.get() == Boolean.TRUE || card == null ? value
                : card.lease.nativeRead((RenderEffect) value);
    }

    /** View callbacks may reenter, replace arguments or skip the native bridge entirely. */
    Object viewWrite(View view, NativeEffect original, RenderEffect value) throws Throwable {
        Object node;
        try { node = renderNode.get(view); }
        catch (Throwable ignored) { return original.set(value); }
        synchronized (this) { nodes.put(node, new WeakReference<>(view)); }
        NativeMaterialCommitScope.Frame<WriteScope> scope = scopes.enter(new WriteScope(view, value), node, NATIVE, 0, false);
        try { return original.set(value); } finally { scopes.leave(scope); }
    }

    /** The verified RenderNode body calls nSetRenderEffect directly, with no View callbacks. */
    synchronized Object nativeWrite(Object node, RenderEffect value, NativeEffect original) throws Throwable {
        NativeMaterialCommitScope.Frame<WriteScope> scope = scopes.matching(node);
        WeakReference<View> reference = nodes.get(node);
        View view = scope != null ? scope.target.view : reference == null ? null : reference.get();
        if (view == null) return original.set(value);
        Card card = cards.get(view);
        // A downstream View hook that replaced our requested value becomes the native
        // owner. Never hide that fresh value behind an earlier mask/restore intention.
        int role = scope == null || scope.target.requested != value ? NATIVE : scope.role;
        if (card == null && role != NATIVE) return original.set(value);
        // A fresh native commit is a known baseline even for a late, previously unknown card.
        RenderEffect baseline = role == NATIVE ? (card == null ? value : card.lease.nativeInput(value)) : card.lease.base;
        RenderEffect output = baseline;
        boolean masked = false;
        try {
            if (role == MASK && !unavailable && belongs(view) && card.translation != 0f) {
                RenderEffect mask = mask(view, card);
                if (mask != null) { output = compose(card, mask); masked = true; }
            }
        } catch (Throwable error) { failed(error); }
        // Do not mutate the lease at View entry, or on a bridge that throws. The actual
        // native commit order (including outer-last View reentry) supplies the final state.
        Object result = original.set(output);
        if (card == null) { card = new Card(baseline); cards.put(view, card); }
        card.lease.committed(baseline, output, masked);
        return result;
    }

    synchronized void configure(View next, int width, int height, float density) {
        if (owner != next) release();
        owner = next; this.width = width; this.height = height; this.density = density;
        parentClip.set(0, 0, width, height);
        geometry = RailNotificationEdge.resolve(width, height, density);
    }

    synchronized void beforeDraw(Canvas canvas) {
        if (owner == null) return;
        if (unavailable) { release(); return; }
        if (!canvas.isHardwareAccelerated() || !canvas.getClipBounds(parentClip)) {
            for (Map.Entry<View, Card> entry : moving.entrySet()) restore(entry.getKey(), entry.getValue());
            return;
        }
        // Only actively swiped/returning cards are visited; there is no View-tree scan.
        for (Map.Entry<View, Card> entry : moving.entrySet()) apply(entry.getKey(), entry.getValue());
        moving.entrySet().removeIf(entry -> entry.getValue().retired);
    }

    synchronized void translated(View view, float translation) {
        if (!belongs(view) || unavailable || !Float.isFinite(translation)) return;
        Card card = cards.get(view);
        if (card == null) {
            // A late attachment with an unreadable existing effect must stay native.
            if (getter == null) return;
            try { card = new Card(readActual(view)); cards.put(view, card); }
            catch (Throwable ignored) { return; }
        }
        card.retired = false;
        card.translation = translation;
        if (translation == 0f) { restore(view, card); moving.remove(view); return; }
        moving.put(view, card);
        apply(view, card);
    }

    private boolean belongs(View view) { return owner != null && view != null && view.getParent() == owner; }

    private RenderEffect mask(View view, Card card) {
        if (geometry == null || !belongs(view) || !view.isAttachedToWindow() || view.getWidth() <= 0 || view.getHeight() <= 0) return null;
        card.matrix.set(view.getMatrix()); card.matrix.getValues(card.values);
        if (!card.matrix.rectStaysRect() || card.values[Matrix.MSCALE_X] <= 0f
                || card.values[Matrix.MSKEW_X] != 0f || card.values[Matrix.MSKEW_Y] != 0f) return null;
        card.matrix.postTranslate(view.getLeft() - owner.getScrollX(), view.getTop() - owner.getScrollY());
        if (!card.matrix.invert(card.inverse)) return null;
        card.viewport.set(Math.max(0, parentClip.left), Math.max(0, parentClip.top),
                Math.min(width, parentClip.right), Math.min(height, parentClip.bottom));
        if (card.viewport.isEmpty()) return null;
        card.inverse.mapRect(card.viewport);
        if (!view.getClipBounds(card.clip)) card.clip.set(0, 0, view.getWidth(), view.getHeight());
        float left = Math.max(card.viewport.left, card.clip.left);
        float right = Math.min(card.viewport.right, card.clip.right);
        float top = Math.max(card.viewport.top, card.clip.top), bottom = Math.min(card.viewport.bottom, card.clip.bottom);
        if (!(right > left && bottom > top)) return null;
        float scale = card.values[Matrix.MSCALE_X];
        float span = Math.min(right - left, geometry.featherWidth(card.translation) / scale);
        if (!Float.isFinite(span) || !Float.isFinite(left) || !Float.isFinite(right)) return null;
        Key key = new Key(card.translation < 0f ? left : right, span, card.translation < 0f);
        RenderEffect mask = effects.get(key);
        if (mask == null) {
            if (shader == null) shader = new RuntimeShader(SHADER);
            shader.setFloatUniform("edge", key.edgeHalf / 2f);
            shader.setFloatUniform("width", key.widthHalf / 2f);
            shader.setFloatUniform("direction", (float) key.direction);
            mask = RenderEffect.createRuntimeShaderEffect(shader, "content");
            effects.put(key, mask);
        }
        card.key = key; card.mask = mask;
        return mask;
    }

    private RenderEffect compose(Card card, RenderEffect mask) {
        if (card.composed != null && card.composedBase == card.lease.base && card.composedMask == mask) return card.composed;
        card.composedBase = card.lease.base;
        card.composedMask = mask;
        card.composed = card.lease.base == null ? mask : RenderEffect.createChainEffect(mask, card.lease.base);
        return card.composed;
    }

    private void apply(View view, Card card) {
        if (!belongs(view) || !view.isAttachedToWindow()) { restore(view, card); card.retired = true; return; }
        try {
            RenderEffect mask = mask(view, card);
            if (mask == null) { restore(view, card); return; }
            RenderEffect output = compose(card, mask);
            if (card.lease.owns(output)) return;
            write(view, output, MASK);
        } catch (Throwable error) { restore(view, card); failed(error); }
    }

    private RenderEffect readActual(View view) throws Throwable {
        actualRead.set(true);
        try { return (RenderEffect) getter.invoke(view); }
        finally { actualRead.remove(); }
    }

    private void write(View view, RenderEffect effect, int role) throws Throwable {
        Object node = renderNode.get(view);
        NativeMaterialCommitScope.Frame<WriteScope> scope = scopes.enter(new WriteScope(view, effect), node, role, 0, false);
        // Owned overlays must not be captured, replaced or short-circuited by a View
        // effect owner's callbacks. Keep the exact OEM body and its invalidation, and
        // let the ordinary RenderNode bridge observe the actual commit below it.
        try { viewOrigin.invoke(view, effect); }
        finally { scopes.leave(scope); }
    }

    private void restore(View view, Card card) {
        if (!card.lease.masked) return;
        try {
            RenderEffect actual = getter == null ? card.lease.written : readActual(view);
            if (!card.lease.owns(actual)) {
                card.lease.committed(actual, actual, false); card.retired = true; return;
            }
            RenderEffect nativeEffect = card.lease.base;
            write(view, nativeEffect, RESTORE);
        } catch (Throwable error) { failed(error); }
        card.composed = card.mask = card.composedBase = card.composedMask = null; card.key = null;
    }

    private void failed(Throwable error) {
        if (!unavailable) { unavailable = true; Xp.w(Main.TAG + "card alpha edge unavailable; native effects retained: " + error); }
    }

    synchronized void release() {
        for (Map.Entry<View, Card> entry : moving.entrySet()) restore(entry.getKey(), entry.getValue());
        moving.clear(); owner = null; effects.clear(); shader = null;
    }
}
