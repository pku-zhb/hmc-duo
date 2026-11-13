package com.zhuhuibin.hypermusiccover.duo;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.RuntimeShader;
import android.view.View;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/** Preserve one native dispatch, materials and Z order; feather only a moving row's edge band. */
final class RailNotificationEdgeRenderer implements RailEdgeDrawPass.Target<Canvas> {
    private static final String SHADER = "uniform shader content;\n"
            + "uniform float width;\n"
            + "uniform float leftSide;\n"
            + "half4 main(float2 p) {\n"
            // The terminal pixel is transparent even when the native clip falls between pixels.
            + "  float denominator = max(width - 0.5, 0.5);\n"
            + "  float t = clamp((p.x - 0.5 * leftSide) / denominator, 0.0, 1.0);\n"
            + "  float s = t * t * (3.0 - 2.0 * t);\n"
            + "  float alpha = mix(1.0 - s, s, leftSide);\n"
            + "  return content.eval(p) * half(alpha);\n"
            + "}\n";

    private static final class Slice {
        final Rect nativeClip = new Rect();
        final RectF bounds = new RectF();
        final Matrix toOwner = new Matrix();
        float translation;
        int left, top, right, bottom, clearLeft, clearRight;
        boolean active, drawn;
        RenderNode node;
        RenderEffect effect;

        void prepare(RailNotificationEdge.Region region, RenderEffect effect) {
            left = region.left; top = region.top; right = region.right; bottom = region.bottom;
            clearLeft = region.clearLeft; clearRight = region.clearRight;
            if (node == null) node = new RenderNode("Duo notification alpha edge");
            if (this.effect != effect) { node.setRenderEffect(effect); this.effect = effect; }
            node.setPosition(left, top, right, bottom);
            active = true;
            drawn = false;
        }

        void record(RenderNode content) {
            Canvas target = node.beginRecording(right - left, bottom - top);
            try {
                target.translate(-left, -top);
                target.drawRenderNode(content);
            } finally { node.endRecording(); }
        }

        void release() {
            if (node != null) node.discardDisplayList();
            node = null; effect = null; active = false; drawn = false;
        }
    }

    // Neither the value nor the shader keeps its weak row key alive.
    private final WeakHashMap<View, Slice> rows = new WeakHashMap<>();
    private final Rect clip = new Rect();
    private RenderNode content;
    private RuntimeShader shader;
    // Uniforms are immutable snapshots: at most two directions * 192 integer pixel widths.
    private final RenderEffect[][] effects = new RenderEffect[2][RailNotificationEdge.MAX_FEATHER_PX + 1];
    private RailNotificationEdge edge;
    private View owner;
    private int width, height;
    private float density;
    private boolean recording, outputStarted, unavailable;

    void configure(View owner, int width, int height, float density) {
        if (this.owner == owner && this.width == width && this.height == height && this.density == density) return;
        this.owner = owner; this.width = width; this.height = height; this.density = density;
        edge = RailNotificationEdge.resolve(width, height, density);
        clearEffects();
    }

    /** The exact native translation setter also supplies the return-to-zero animation. */
    void translated(View row, float translation) {
        if (unavailable) return;
        if (row.getParent() != owner || !Float.isFinite(translation)) {
            Slice old = rows.remove(row);
            if (old != null) old.release();
            return;
        }
        Slice slice = rows.get(row);
        if (translation == 0f) {
            if (slice != null && slice.translation != 0f) {
                slice.translation = 0f;
                slice.active = false;
                // The parent can still reference our previous nodes until its next draw.
                // Do not erase them before that native replacement frame is recorded.
                owner.invalidate();
            }
            return;
        }
        if (slice == null) { slice = new Slice(); rows.put(row, slice); }
        if (slice.translation != translation) {
            slice.translation = translation;
            // A RenderNode-only child translation need not re-record parent dispatchDraw.
            // Its mask geometry changes, so invalidate this one owned stack explicitly.
            owner.invalidate();
        }
    }

    @Override public Canvas begin(Canvas original) {
        if (unavailable) { release(); return null; }
        if (edge == null || !original.isHardwareAccelerated()) return null;
        if (!original.getClipBounds(clip) || clip.isEmpty()) return null;
        boolean any = false;
        Iterator<Map.Entry<View, Slice>> iterator = rows.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<View, Slice> entry = iterator.next();
            View row = entry.getKey(); Slice slice = entry.getValue(); slice.active = false;
            if (row.getParent() != owner || !row.isShown()) { slice.release(); iterator.remove(); continue; }
            if (slice.translation == 0f || !row.getClipBounds(slice.nativeClip) || slice.nativeClip.isEmpty()) continue;
            slice.toOwner.set(row.getMatrix());
            // Only axis-aligned rectangles can describe an exact clipping boundary. Perspective
            // press/launch transforms retain their complete native path until they settle.
            if (!slice.toOwner.rectStaysRect()) continue;
            slice.toOwner.postTranslate(row.getLeft() - owner.getScrollX(), row.getTop() - owner.getScrollY());
            slice.bounds.set(slice.nativeClip);
            slice.toOwner.mapRect(slice.bounds);
            RailNotificationEdge.Region region = edge.region(slice.bounds.left, slice.bounds.top,
                    slice.bounds.right, slice.bounds.bottom, clip.left, clip.top, clip.right, clip.bottom,
                    slice.translation);
            if (region == null) continue;
            slice.prepare(region, effect(region.width(), region.leftSide));
            any = true;
        }
        if (!any) return null;
        if (content == null) content = new RenderNode("Duo native notification children");
        outputStarted = false;
        // OEM dispatchDraw sizes its own blur node from Canvas dimensions. Do not substitute
        // our narrower layout slot for those original dimensions during offscreen recording.
        content.setPosition(0, 0, original.getWidth(), original.getHeight());
        content.setClipToBounds(false);
        Canvas target = content.beginRecording(original.getWidth(), original.getHeight());
        recording = true;
        target.clipRect(clip);
        return target;
    }

    private RenderEffect effect(int width, boolean left) {
        int direction = left ? 1 : 0;
        RenderEffect effect = effects[direction][width];
        if (effect != null) return effect;
        if (shader == null) shader = new RuntimeShader(SHADER);
        shader.setFloatUniform("width", (float) width);
        shader.setFloatUniform("leftSide", left ? 1f : 0f);
        effect = RenderEffect.createRuntimeShaderEffect(shader, "content");
        effects[direction][width] = effect;
        return effect;
    }

    @Override public void complete(Canvas original) {
        content.endRecording(); recording = false;
        for (Slice slice : rows.values()) if (slice.active) slice.record(content);
        int saved = original.save();
        try {
            for (Slice slice : rows.values()) if (slice.active)
                original.clipOutRect(slice.clearLeft, slice.top, slice.clearRight, slice.bottom);
            outputStarted = true;
            original.drawRenderNode(content);
        } finally { original.restoreToCount(saved); }
        for (Slice slice : rows.values()) if (slice.active) {
            saved = original.save();
            try {
                original.clipRect(slice.left, slice.top, slice.right, slice.bottom);
                // Returning rows can overlap another swipe's band. Composite those pixels
                // once, rather than adding the same filtered stack a second time.
                for (Slice previous : rows.values()) if (previous.active && previous != slice) {
                    if (previous.clearLeft < previous.left)
                        original.clipOutRect(previous.clearLeft, previous.top, previous.left, previous.bottom);
                    if (previous.clearRight > previous.right)
                        original.clipOutRect(previous.right, previous.top, previous.clearRight, previous.bottom);
                    if (previous.drawn)
                        original.clipOutRect(previous.left, previous.top, previous.right, previous.bottom);
                }
                original.drawRenderNode(slice.node);
                slice.drawn = true;
            } finally { original.restoreToCount(saved); }
        }
    }

    @Override public void abort() {
        if (!recording) return;
        recording = false;
        try { content.endRecording(); } catch (Throwable ignored) {}
    }

    @Override public void recover(Canvas original) {
        if (outputStarted || content == null || !content.hasDisplayList()) return;
        try { original.drawRenderNode(content); } catch (Throwable ignored) {}
    }

    @Override public void failed(Throwable error) {
        unavailable = true;
        Xp.w(Main.TAG + "notification alpha edge unavailable; retaining native draw: " + error);
        if (owner != null) owner.invalidate();
    }

    private void clearEffects() {
        shader = null;
        for (RenderEffect[] direction : effects) java.util.Arrays.fill(direction, null);
    }

    void release() {
        abort();
        if (content != null) content.discardDisplayList();
        for (Slice slice : rows.values()) slice.release();
        rows.clear(); content = null; clearEffects(); owner = null;
    }
}
