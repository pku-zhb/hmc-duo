package com.zhuhuibin.hypermusiccover.duo;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RuntimeShader;
import android.os.SystemClock;
import android.view.Choreographer;
import android.view.View;
import android.view.ViewGroup;

/** Local dynamic-colour fallback for video wallpaper engines without an image GL callback. */
final class VideoBackdropMesh extends View implements Choreographer.FrameCallback {
    private static final String SHADER =
            "uniform float2 size; uniform float time; uniform float grid[48];\n"
            + "float4 bern(float t) { float s = 1.0-t; return float4(s*s*s,3*s*s*t,3*s*t*t,t*t*t); }\n"
            + "half4 main(float2 xy) {\n"
            + " float2 p=xy/size;\n"
            + " float2 q=clamp(p+0.14*float2(sin(p.y*3.1+time*.19)+.5*sin(p.x*4.3+time*.13),"
            + "sin(p.x*2.7+time*.17)+.5*sin(p.y*3.7+time*.11)),0.0,1.0);\n"
            + " float4 u=bern(q.x),v=bern(q.y); float3 c=float3(0);\n"
            + " for(int j=0;j<4;j++){float3 row=float3(0);"
            + "for(int i=0;i<4;i++){int n=(j*4+i)*3;row+=u[i]*float3(grid[n],grid[n+1],grid[n+2]);} c+=v[j]*row;}\n"
            + " return half4(c,1); }";
    private static VideoBackdropMesh view;
    private static String failure;
    private final RuntimeShader shader = new RuntimeShader(SHADER);
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float[] colors;
    private boolean wanted, scheduled;

    private VideoBackdropMesh(android.content.Context context) {
        super(context);
        setTag("HMCVideoBackdropMesh");
        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setVisibility(INVISIBLE);
        paint.setShader(shader);
    }

    static String describe() {
        if (failure != null) return "static-fallback:" + failure;
        return view != null && view.getVisibility() == VISIBLE ? "local-video-mesh" : "inactive";
    }

    static void show(boolean on, float[] spec) {
        if (!on) {
            if (view != null) { view.wanted = false; view.hide(); }
            return;
        }
        if (failure != null || spec == null || spec.length != WallpaperGlow.MESH * 3) return;
        ViewGroup host = CoverPush.coverLayer();
        if (host == null || !host.isAttachedToWindow()) return;
        try {
            if (view == null) view = new VideoBackdropMesh(host.getContext());
            if (view.getParent() != host) {
                if (view.getParent() instanceof ViewGroup) ((ViewGroup) view.getParent()).removeView(view);
                int index = host.getChildCount();
                for (int i = 0; i < host.getChildCount(); i++) {
                    View child = host.getChildAt(i);
                    if (child instanceof CoverCardLayer || "HMCWallpaperDim".equals(child.getTag())) {
                        index = i;
                        break;
                    }
                }
                host.addView(view, index, new ViewGroup.LayoutParams(-1, -1));
            }
            if (view.colors != spec) {
                view.shader.setFloatUniform("grid", spec);
                view.colors = spec;
            }
            view.wanted = true;
            refreshVisibility();
        } catch (Throwable error) {
            failure = error.getClass().getSimpleName();
            if (view != null) view.hide();
            Xp.w(Main.TAG + "video mesh unavailable; keeping static artwork: " + error);
        }
    }

    private boolean eligible() {
        return wanted && Main.sVideoWallpaper && Main.sGlowDrift && Main.sMeshBackdrop
                && !CoverBackdrop.hidden() && Main.coverModeOn() && Main.screenOnCached()
                && Main.sCover != null && Main.sCover.getVisibility() == VISIBLE
                && ClockCollapse.phase() != ClockCollapse.Phase.AOD && Main.onKeyguardNow()
                && LockscreenSurfacePolicy.visible(Main.keyguardLocked(),
                        MiniPlayerScene.INSTANCE.getKeyguardGoingAway());
    }

    static void refreshVisibility() {
        VideoBackdropMesh v = view;
        if (v == null) return;
        if (!v.eligible()) v.hide();
        else {
            v.setVisibility(VISIBLE);
            // Keep the existing wallpaper first-frame hold and fade, including interrupted wake.
            v.setAlpha(Main.sCover.getAlpha());
            v.invalidate();
            v.schedule();
        }
    }

    private void schedule() {
        if (scheduled || !isAttachedToWindow()) return;
        scheduled = true;
        Choreographer.getInstance().postFrameCallbackDelayed(this, 32L);
    }

    private void hide() {
        if (scheduled) Choreographer.getInstance().removeFrameCallback(this);
        scheduled = false;
        setVisibility(INVISIBLE);
    }

    @Override public void doFrame(long now) {
        scheduled = false;
        if (!eligible()) { hide(); return; }
        setAlpha(Main.sCover.getAlpha());
        invalidate();
        schedule();
    }

    @Override protected void onDraw(Canvas canvas) {
        if (!eligible() || getWidth() <= 0 || getHeight() <= 0) { hide(); return; }
        try {
            shader.setFloatUniform("size", getWidth(), getHeight());
            shader.setFloatUniform("time", (SystemClock.uptimeMillis() % 600_000L) / 1000f);
            canvas.drawRect(0f, 0f, getWidth(), getHeight(), paint);
        } catch (Throwable error) {
            failure = error.getClass().getSimpleName();
            wanted = false;
            hide();
            Xp.w(Main.TAG + "video mesh draw failed; keeping static artwork: " + error);
        }
    }

    @Override protected void onDetachedFromWindow() {
        hide();
        super.onDetachedFromWindow();
    }
}
