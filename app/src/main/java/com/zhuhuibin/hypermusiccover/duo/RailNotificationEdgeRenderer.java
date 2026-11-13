package com.zhuhuibin.hypermusiccover.duo;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.RuntimeShader;
import android.view.View;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A GPU edge on right-swiping native rows, never a window/background blur. Native dispatchDraw
 * records once. The sharp body replays directly; only each moving row's narrow edge is filtered.
 * There is no bitmap, readback, per-frame reflection or replacement of the OEM's render effect.
 */
final class RailNotificationEdgeRenderer implements RailEdgeDrawPass.Target<Canvas> {
    private static final String SHADER = "uniform shader content;\n"
            + "uniform float start;\n"
            + "uniform float width;\n"
            + "uniform float radius;\n"
            + "uniform float strength;\n"
            + "half4 main(float2 p) {\n"
            + "  float t = clamp((p.x - start) / width, 0.0, 1.0);\n"
            + "  float s = t * t * (3.0 - 2.0 * t) * strength;\n"
            + "  float r = radius * s;\n"
            + "  half4 c = content.eval(p) * 0.25;\n"
            + "  c += (content.eval(p + float2(r, 0.0))\n"
            + "      + content.eval(p - float2(r, 0.0))\n"
            + "      + content.eval(p + float2(0.0, r))\n"
            + "      + content.eval(p - float2(0.0, r))) * 0.125;\n"
            + "  c += (content.eval(p + float2(r, r))\n"
            + "      + content.eval(p + float2(-r, r))\n"
            + "      + content.eval(p + float2(r, -r))\n"
            + "      + content.eval(p - float2(r, r))) * 0.0625;\n"
            + "  return c * half(1.0 - s);\n"
            + "}\n";

    private static final class Slice {
        final Rect clip = new Rect();
        float translation;
        int top, bottom, sourceTop, sourceBottom;
        boolean active;
        RenderNode node;
        RenderEffect effect;

        void prepare(RailNotificationEdge edge, RailNotificationEdge.Region region, int height,
                     RenderEffect effect) {
            top = region.top;
            bottom = region.bottom;
            sourceTop = Math.max(0, top - edge.radius);
            sourceBottom = Math.min(height, bottom + edge.radius);
            if (node == null) node = new RenderNode("Duo swiping notification edge");
            if (this.effect != effect) { node.setRenderEffect(effect); this.effect = effect; }
            node.setPosition(edge.sourceLeft, sourceTop, edge.right, sourceBottom);
            active = true;
        }

        void record(RenderNode content, RailNotificationEdge edge) {
            Canvas target = node.beginRecording(edge.sourceWidth(), sourceBottom - sourceTop);
            try {
                target.translate(-edge.sourceLeft, -sourceTop);
                target.drawRenderNode(content);
            } finally {
                node.endRecording();
            }
        }

        void release() {
            if (node != null) node.discardDisplayList();
            node = null; effect = null; active = false;
        }
    }

    // Values contain no reference to their row. Retired/detached native rows cannot be retained.
    private final WeakHashMap<View, Slice> rows = new WeakHashMap<>();
    private final Rect clip = new Rect();
    private RenderNode content;
    private RuntimeShader shader;
    private final RenderEffect[] effects = new RenderEffect[RailNotificationEdge.STRENGTH_STEPS + 1];
    private RailNotificationEdge edge;
    private View owner;
    private int width, height;
    private float density;
    private boolean recording, outputStarted, unavailable;

    void configure(View owner, int width, int height, float density) {
        if (this.owner == owner && this.width == width && this.height == height && this.density == density) return;
        this.owner = owner;
        this.width = width;
        this.height = height;
        this.density = density;
        edge = RailNotificationEdge.resolve(width, height, density);
        java.util.Arrays.fill(effects, null);
        shader = null;
    }

    /** Called after the exact OEM setTranslation(float), including its return-to-zero animation. */
    void translated(View row, float translation) {
        if (unavailable) return;
        if (row.getParent() != owner || !Float.isFinite(translation)) {
            Slice old = rows.remove(row);
            if (old != null) old.release();
            return;
        }
        Slice slice = rows.get(row);
        if (translation <= 0f) {
            if (slice != null) {
                slice.translation = 0f;
                slice.active = false;
                if (slice.node != null) slice.node.discardDisplayList();
            }
            if (content != null) content.discardDisplayList();
            return;
        }
        if (slice == null) { slice = new Slice(); rows.put(row, slice); }
        slice.translation = translation;
    }

    @Override public Canvas begin(Canvas original) {
        if (unavailable) { release(); return null; }
        if (edge == null || !original.isHardwareAccelerated()) return null;
        boolean any = false;
        Iterator<Map.Entry<View, Slice>> iterator = rows.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<View, Slice> entry = iterator.next();
            View row = entry.getKey();
            Slice slice = entry.getValue();
            slice.active = false;
            if (row.getParent() != owner || !row.isShown()) {
                slice.release(); iterator.remove(); continue;
            }
            if (slice.translation <= 0f) continue;
            // ExpandableView.updateClipping uses actualHeight - clipBottom here. Its measured
            // height can span another collapsed row, so an unknown/null clip stays fully native.
            if (!row.getClipBounds(slice.clip)) continue;
            RailNotificationEdge.Region region = edge.region(row.getY(), slice.clip.top,
                    slice.clip.bottom, height, slice.translation);
            if (region == null) continue;
            slice.prepare(edge, region, height, effect(region.level));
            any = true;
        }
        if (!any) return null; // At dx=0 all pixels and rendering stay exactly native.
        if (content == null) content = new RenderNode("Duo native notification children");
        outputStarted = false;
        content.setPosition(0, 0, width, height);
        content.setClipToBounds(false);
        Canvas target = content.beginRecording(width, height);
        recording = true;
        if (original.getClipBounds(clip)) target.clipRect(clip);
        return target;
    }

    private RenderEffect effect(int level) {
        RenderEffect effect = effects[level];
        if (effect != null) return effect;
        if (shader == null) {
            shader = new RuntimeShader(SHADER);
            shader.setFloatUniform("start", (float) (edge.start() - edge.sourceLeft));
            shader.setFloatUniform("width", (float) edge.width);
            shader.setFloatUniform("radius", (float) edge.radius);
        }
        shader.setFloatUniform("strength", (float) level / RailNotificationEdge.STRENGTH_STEPS);
        // RenderEffect snapshots its shader uniforms. Cache those immutable snapshots instead
        // of mutating a shader behind one effect or allocating a new effect on every frame.
        effect = RenderEffect.createRuntimeShaderEffect(shader, "content");
        effects[level] = effect;
        return effect;
    }

    @Override public void complete(Canvas original) {
        content.endRecording();
        recording = false;
        // All GPU setup finishes before output starts, so an error can replay native commands.
        for (Slice slice : rows.values()) if (slice.active) slice.record(content, edge);
        int saved = original.save();
        try {
            for (Slice slice : rows.values()) if (slice.active)
                original.clipOutRect(edge.start(), slice.top, edge.right, slice.bottom);
            outputStarted = true;
            original.drawRenderNode(content);
        } finally {
            original.restoreToCount(saved);
        }
        for (Slice slice : rows.values()) if (slice.active) {
            saved = original.save();
            try {
                original.clipRect(edge.start(), slice.top, edge.right, slice.bottom);
                original.drawRenderNode(slice.node);
            } finally {
                original.restoreToCount(saved);
            }
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
        Xp.w(Main.TAG + "notification soft edge unavailable; retaining native draw: " + error);
        // The caller may already have recorded a reference to our recovery node this frame.
        // Retain its commands until the next native frame replaces them; discarding now would
        // erase that recovery as well. Ownership release can still retire the entire session.
        if (owner != null) owner.invalidate();
    }

    void release() {
        abort();
        if (content != null) content.discardDisplayList();
        for (Slice slice : rows.values()) slice.release();
        rows.clear();
        content = null;
        shader = null;
        java.util.Arrays.fill(effects, null);
        owner = null;
    }
}
