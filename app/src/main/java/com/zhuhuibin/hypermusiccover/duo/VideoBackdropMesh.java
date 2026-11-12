package com.zhuhuibin.hypermusiccover.duo;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.RuntimeShader;
import android.os.SystemClock;
import android.view.Choreographer;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

/** Local artwork presentation, including OEMs with no separate keyguard wallpaper renderer. */
final class VideoBackdropMesh extends View implements Choreographer.FrameCallback {
    private static final String SHADER =
            "uniform float2 size; uniform float time; uniform float grid[48];\n"
            + "float4 bern(float t) { float s = 1.0-t; return float4(s*s*s,3*s*s*t,3*s*t*t,t*t*t); }\n"
            + "half4 main(float2 xy) {\n"
            + " float2 p=xy/size;\n"
            + " float2 q=clamp(p+0.14*float2(sin(p.y*3.1+time*.19)+.5*sin(p.x*4.3+time*.13),"
            + "sin(p.x*2.7+time*.17)+.5*sin(p.y*3.7+time*.11)),0.0,1.0);\n"
            + " float4 u=bern(q.x),v=bern(q.y); float3 c=float3(0);\n"
            + " c+=v.x*(u.x*float3(grid[0],grid[1],grid[2])"
            + "+u.y*float3(grid[3],grid[4],grid[5])"
            + "+u.z*float3(grid[6],grid[7],grid[8])"
            + "+u.w*float3(grid[9],grid[10],grid[11]));\n"
            + " c+=v.y*(u.x*float3(grid[12],grid[13],grid[14])"
            + "+u.y*float3(grid[15],grid[16],grid[17])"
            + "+u.z*float3(grid[18],grid[19],grid[20])"
            + "+u.w*float3(grid[21],grid[22],grid[23]));\n"
            + " c+=v.z*(u.x*float3(grid[24],grid[25],grid[26])"
            + "+u.y*float3(grid[27],grid[28],grid[29])"
            + "+u.z*float3(grid[30],grid[31],grid[32])"
            + "+u.w*float3(grid[33],grid[34],grid[35]));\n"
            + " c+=v.w*(u.x*float3(grid[36],grid[37],grid[38])"
            + "+u.y*float3(grid[39],grid[40],grid[41])"
            + "+u.z*float3(grid[42],grid[43],grid[44])"
            + "+u.w*float3(grid[45],grid[46],grid[47]));\n"
            + " return half4(c,1); }";
    private static VideoBackdropMesh view;
    private static String failure;
    private final ArtworkBackdropState state = new ArtworkBackdropState();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF bounds = new RectF();
    private RuntimeShader shader;
    private float[] colors;
    private boolean wanted, dynamic, scheduled;
    private boolean revealPending;
    private long fadeStarted, frames, lastDrawAt;
    private float renderAlpha;
    private float fullArtworkAlpha;
    private long lastFrameAt;
    private float lastVideoAlpha = Float.NaN;
    private ViewGroup ownerLayer;
    private ViewTreeObserver observer;
    private ViewTreeObserver.OnPreDrawListener guard;
    private Runnable reveal;

    private VideoBackdropMesh(android.content.Context context) {
        super(context);
        setTag("HMCVideoBackdropMesh");
        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setVisibility(INVISIBLE);
    }

    /** Content-free evidence of actual drawing, distinct from the requested settings. */
    static String describe() {
        VideoBackdropMesh v = view;
        if (v == null) return "unattached";
        int[] location = new int[2];
        v.getLocationOnScreen(location);
        android.graphics.Bitmap source = CoverCardLayer.currentBackdrop();
        return "attached=" + v.isAttachedToWindow() + " visible=" + (v.getVisibility() == VISIBLE)
                + " shown=" + v.isShown() + " mode=" + (v.dynamic ? "dynamic" : "artwork")
                + " source=" + v.state.source() + " drawn=" + v.state.drawn()
                + " ready=" + v.state.revealed() + " epoch=" + v.state.epoch()
                + " alpha=" + v.renderAlpha + " frames=" + v.frames + " lastDraw=" + v.lastDrawAt
                + " host=" + (v.getParent() == null ? "none" : v.getParent().getClass().getSimpleName())
                + " bounds=" + location[0] + "," + location[1] + "," + v.getWidth() + "x" + v.getHeight()
                + " bitmap=" + (source == null ? "none" : source.getWidth() + "x" + source.getHeight())
                + " shader=" + (v.shader == null ? "none" : "ready")
                + " shape=" + (CoverCardLayer.fullScene() ? "full-band" : "card")
                + " bandAlpha=" + v.fullArtworkAlpha
                + (failure == null ? "" : " fallback=" + failure);
    }

    static void show(boolean on, boolean moving, float[] spec) {
        if (!on) {
            if (view != null) { view.wanted = false; view.hide(); }
            return;
        }
        ViewGroup layer = CoverPush.coverLayer();
        if (layer == null || !layer.isAttachedToWindow()) {
            if (view != null) view.hide();
            return;
        }
        try {
            if (view == null) view = new VideoBackdropMesh(layer.getContext());
            view.attach(layer);
            boolean nextDynamic = moving && spec != null && spec.length == WallpaperGlow.MESH * 3;
            boolean changed = view.dynamic != nextDynamic;
            view.dynamic = nextDynamic;
            if (nextDynamic && view.colors != spec) {
                view.colors = spec;
                changed = true;
                view.prepareShader();
            }
            view.wanted = true;
            refreshVisibility();
            if (changed && view.getVisibility() == VISIBLE) view.invalidate();
        } catch (Throwable error) {
            if (view != null) view.hide();
            Xp.w(Main.TAG + "local backdrop attach failed: " + error);
        }
    }

    private void attach(ViewGroup layer) {
        View below = fullScreenRoot(layer);
        ViewGroup host = below == null ? layer : (ViewGroup) below.getParent();
        ownerLayer = layer;
        if (getParent() == host) { watch(host); return; }
        hide();
        unwatch();
        if (getParent() instanceof ViewGroup) ((ViewGroup) getParent()).removeView(this);
        int index = below == null ? host.getChildCount() : host.indexOfChild(below);
        // Explicit backdrop -> dim -> foreground ordering, regardless of creation order.
        for (int i = 0; i < index; i++) {
            View child = host.getChildAt(i);
            if ("HMCWallpaperDim".equals(child.getTag()) || "HMCCoverWash".equals(child.getTag())
                    || child instanceof CoverCardLayer) {
                index = i;
                break;
            }
        }
        host.addView(this, index, new ViewGroup.LayoutParams(-1, -1));
        watch(host);
    }

    private static View fullScreenRoot(ViewGroup layer) {
        // The doze scales/clips the keyguard root. Still artwork must use its full-size sibling,
        // just like the existing AOD wash. Video's original overlay is inside the root instead.
        if (!Main.sVideoWallpaper) {
            int id = layer.getResources().getIdentifier("keyguard_root_view", "id", "com.android.systemui");
            for (Object at = layer; id != 0 && at instanceof View; at = ((View) at).getParent()) {
                View candidate = (View) at;
                if (candidate.getId() == id && candidate.getParent() instanceof ViewGroup) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /** The morph lands on this receiver, which can be outside the zoomed cover-layer subtree. */
    static CoverMorphMotion.Box fullSceneTarget(android.graphics.Bitmap artwork) {
        if (artwork == null || artwork.isRecycled()) return null;
        ViewGroup layer = CoverPush.coverLayer();
        if (layer == null) return null;
        View below = fullScreenRoot(layer);
        ViewGroup host = below == null ? layer : (ViewGroup) below.getParent();
        VideoBackdropMesh v = view;
        View receiver = v != null && v.ownerLayer == layer && v.getParent() == host
                && v.getWidth() > 0 && v.getHeight() > 0 ? v : host;
        int insetX = receiver == host ? host.getPaddingLeft() : 0;
        int insetY = receiver == host ? host.getPaddingTop() : 0;
        int width = receiver.getWidth() - insetX
                - (receiver == host ? host.getPaddingRight() : 0);
        int height = receiver.getHeight() - insetY
                - (receiver == host ? host.getPaddingBottom() : 0);
        CoverFullScenePolicy.Band band = CoverFullScenePolicy.band(width, height,
                artwork.getWidth(), artwork.getHeight(), Main.sBias);
        if (band == null) return null;
        RectF destination = new RectF(insetX + band.left, insetY + band.top,
                insetX + band.left + band.width, insetY + band.top + band.height);
        ClockCollapse.boundsOnScreen(receiver, destination);
        return new CoverMorphMotion.Box(destination.left, destination.top,
                destination.width(), destination.height());
    }

    private void watch(ViewGroup host) {
        if (observer == host.getViewTreeObserver() && guard != null) return;
        unwatch();
        observer = host.getViewTreeObserver();
        guard = () -> { refreshVisibility(); return true; };
        observer.addOnPreDrawListener(guard);
    }

    private void unwatch() {
        if (observer != null && observer.isAlive() && guard != null)
            observer.removeOnPreDrawListener(guard);
        observer = null;
        guard = null;
    }

    private void prepareShader() {
        if (failure != null || colors == null) return;
        try {
            if (shader == null) shader = new RuntimeShader(SHADER);
            shader.setFloatUniform("grid", colors);
        } catch (Throwable error) {
            shader = null;
            failure = error.getClass().getSimpleName();
            Xp.w(Main.TAG + "local mesh unavailable; retaining static artwork: " + error);
        }
    }

    private boolean eligible() {
        return wanted && isAttachedToWindow() && ownerLayer == CoverPush.coverLayer()
                && ownerLayer.isAttachedToWindow() && CoverCardLayer.currentBackdrop() != null
                && ArtworkBackdropState.eligible(!CoverBackdrop.hidden() || CoverCardLayer.fullScene(),
                        Main.coverModeOn(),
                        Main.screenOnCached() && ClockCollapse.phase() != ClockCollapse.Phase.AOD,
                        Main.keyguardLocked(), MiniPlayerScene.INSTANCE.getKeyguardGoingAway(),
                        Main.onKeyguardNow());
    }

    static void refreshVisibility() {
        VideoBackdropMesh v = view;
        if (v == null) return;
        if (!v.eligible()) { v.hide(); return; }
        int generation = CoverCardLayer.backdropGeneration();
        boolean changed = v.state.source() != generation;
        if (Main.sVideoWallpaper && Main.sCover != null
                && Main.sCover.getAlpha() != v.lastVideoAlpha) changed = true;
        v.state.select(generation);
        if (v.getVisibility() != VISIBLE) {
            // Keep the View drawable while waiting. Alpha zero on its RenderNode can skip
            // onDraw forever, so the first source is submitted at one alpha step instead.
            v.renderAlpha = 1f / 255f;
            v.setVisibility(VISIBLE);
            changed = true;
        }
        if (changed) v.invalidate();
        if (v.needsFrames()) v.schedule();
    }

    private boolean needsFrames() {
        return !state.revealed() || renderAlpha < 0.999f || dynamic && shader != null
                || CoverCardLayer.backdropTransitioning()
                || Math.abs(fullArtworkAlpha - fullArtworkTarget()) > 0.001f;
    }

    private static float fullArtworkTarget() {
        return CoverCardLayer.fullSceneArtworkVisible() ? 1f : 0f;
    }

    static boolean fullSceneHandoffReady(int generation) {
        VideoBackdropMesh v = view;
        return v != null && v.eligible() && v.state.source() == generation
                && v.state.drawn() == generation && v.state.revealed()
                && v.renderAlpha >= .995f && v.fullArtworkAlpha >= .995f;
    }

    static boolean artworkAt(float x, float y) {
        VideoBackdropMesh v = view;
        if (v == null || !v.eligible() || !v.isShown() || v.fullArtworkAlpha < .5f) return false;
        int[] at = new int[2];
        v.getLocationOnScreen(at);
        return x >= at[0] && y >= at[1] && x < at[0] + v.getWidth()
                && y < at[1] + v.getHeight();
    }

    private void schedule() {
        if (scheduled || !isAttachedToWindow()) return;
        scheduled = true;
        Choreographer.getInstance().postFrameCallbackDelayed(this, 32L);
    }

    private void hide() {
        if (scheduled) Choreographer.getInstance().removeFrameCallback(this);
        scheduled = false;
        if (reveal != null) removeCallbacks(reveal);
        reveal = null;
        revealPending = false;
        fadeStarted = 0L;
        renderAlpha = 0f;
        fullArtworkAlpha = 0f;
        lastFrameAt = 0L;
        lastVideoAlpha = Float.NaN;
        state.hide();
        if (getVisibility() != INVISIBLE) setVisibility(INVISIBLE);
    }

    @Override public void doFrame(long now) {
        scheduled = false;
        if (!eligible()) { hide(); return; }
        float dt = lastFrameAt == 0L ? 1f / 60f
                : Math.min(.05f, Math.max(0f, (now - lastFrameAt) / 1e9f));
        lastFrameAt = now;
        float target = fullArtworkTarget();
        fullArtworkAlpha += (target - fullArtworkAlpha) * Math.min(1f, dt / .06f);
        if (Math.abs(target - fullArtworkAlpha) < .001f) fullArtworkAlpha = target;
        if (state.revealed()) {
            float t = Math.min(1f, (SystemClock.uptimeMillis() - fadeStarted) / 240f);
            renderAlpha = t * t * (3f - 2f * t);
        }
        invalidate();
        if (needsFrames()) schedule();
    }

    @Override protected void onDraw(Canvas canvas) {
        if (!eligible() || getWidth() <= 0 || getHeight() <= 0) { hide(); return; }
        int generation = CoverCardLayer.backdropGeneration();
        state.select(generation);
        bounds.set(0f, 0f, getWidth(), getHeight());
        float alpha = renderAlpha;
        // Preserve the video's existing first-frame hold/fade as well as our local draw gate.
        if (Main.sVideoWallpaper && Main.sCover != null) {
            lastVideoAlpha = Main.sCover.getAlpha();
            if (state.revealed()) alpha *= lastVideoAlpha;
        }
        int saved = alpha < 0.999f
                ? canvas.saveLayerAlpha(bounds, Math.round(255f * alpha)) : canvas.save();
        boolean drawn = false;
        try {
            // FULL consists of a sharp feathered band over a separate fill. Native-background
            // mode omits only the fill; it does not remove the user's foreground artwork.
            drawn = CoverBackdrop.hidden() || CoverCardLayer.drawBackdrop(canvas, bounds, paint);
            if (drawn && !CoverBackdrop.hidden() && dynamic && shader != null) {
                try {
                    shader.setFloatUniform("size", getWidth(), getHeight());
                    shader.setFloatUniform("time", (SystemClock.uptimeMillis() % 600_000L) / 1000f);
                    paint.setShader(shader);
                    paint.setAlpha(255);
                    canvas.drawRect(bounds, paint);
                } catch (Throwable error) {
                    shader = null;
                    failure = error.getClass().getSimpleName();
                    Xp.w(Main.TAG + "local mesh draw failed; retaining static artwork: " + error);
                } finally { paint.setShader(null); }
            }
            if (CoverCardLayer.fullScene())
                CoverCardLayer.drawFullSceneArtwork(canvas, bounds, paint, fullArtworkAlpha);
        } finally { canvas.restoreToCount(saved); }
        if (!drawn) return;
        frames++;
        lastDrawAt = SystemClock.uptimeMillis();
        state.drawn(generation);
        if (!state.revealed() && !revealPending) {
            long epoch = state.epoch();
            revealPending = true;
            reveal = () -> {
                reveal = null;
                revealPending = false;
                if (!eligible() || !state.reveal(epoch, generation)) return;
                fadeStarted = SystemClock.uptimeMillis();
                schedule();
            };
            // A source being prepared is insufficient: wait for the draw above and then a frame.
            postOnAnimation(reveal);
        }
    }

    @Override protected void onDetachedFromWindow() {
        hide();
        unwatch();
        super.onDetachedFromWindow();
    }
}
