package com.zhuhuibin.hypermusiccover.duo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;
import android.text.format.DateFormat;
import android.view.MotionEvent;
import android.view.View;

import java.util.Calendar;

/** A single-line time with the native date's typography, hosted inside the keyguard overlay. */
final class CompactClockRowView extends View {
    final TextPaint ink = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private String time = "";
    private long minute = Long.MIN_VALUE;
    private float baseline;
    private String styleKey;
    private android.graphics.Typeface typeface;
    private android.graphics.Shader shader;
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            refreshTime();
            if (getVisibility() == VISIBLE && isAttachedToWindow()) {
                postDelayed(this, 60_000L - Math.floorMod(System.currentTimeMillis(), 60_000L));
            }
        }
    };

    CompactClockRowView(Context context) {
        super(context);
        setClickable(false); setLongClickable(false); setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    void refreshTime() {
        long now = System.currentTimeMillis();
        if (minute == now / 60_000L) return;
        minute = now / 60_000L;
        time = DateFormat.format(DateFormat.is24HourFormat(getContext()) ? "HH:mm" : "h:mm",
                Calendar.getInstance()).toString();
        invalidate();
    }

    float timeWidth() { refreshTime(); return ink.measureText(time); }
    float timeWidth(Paint paint) { refreshTime(); return paint.measureText(time); }

    void style(TextPaint source, float scaleX, float scaleY, int color) {
        float size = source.getTextSize() * scaleY;
        float width = source.getTextScaleX() * scaleX / scaleY;
        String key = size + ":" + color + ":" + width + ":"
                + source.getTextSkewX() + ":" + source.getFlags() + ":"
                + source.getFontFeatureSettings() + ":" + source.getFontVariationSettings();
        if (key.equals(styleKey) && typeface == source.getTypeface() && shader == source.getShader()) return;
        styleKey = key; typeface = source.getTypeface(); shader = source.getShader();
        ink.set(source); ink.setTextAlign(Paint.Align.LEFT); ink.setTextSize(size); ink.setColor(color);
        ink.setTextScaleX(width);
        invalidate();
    }

    void show(float alpha, float textBaseline) {
        if (baseline != textBaseline) { baseline = textBaseline; invalidate(); }
        setAlpha(alpha);
        if (getVisibility() != VISIBLE) {
            setVisibility(VISIBLE);
            removeCallbacks(tick); tick.run();
        }
    }

    void hide() { setVisibility(INVISIBLE); removeCallbacks(tick); }
    @Override protected void onDetachedFromWindow() { removeCallbacks(tick); super.onDetachedFromWindow(); }
    @Override public boolean onTouchEvent(MotionEvent event) { return false; }
    @Override protected void onDraw(Canvas canvas) { canvas.drawText(time, 0f, baseline, ink); }
}
