package com.zhuhuibin.hypermusiccover.duo;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;

/** A removable selection veil; it never replaces the image or participates in layout/input. */
final class ArtworkThumbnailVeil implements View.OnAttachStateChangeListener {
    private View host;
    private Veil drawable;

    void update(View artwork, boolean show, float radius) {
        if (!show || artwork == null || !artwork.isAttachedToWindow()
                || artwork.getWidth() <= 0 || artwork.getHeight() <= 0) {
            clear();
            return;
        }
        if (host != artwork) {
            clear();
            host = artwork;
            drawable = new Veil();
            host.getOverlay().add(drawable);
            host.addOnAttachStateChangeListener(this);
        }
        drawable.geometry(artwork.getWidth(), artwork.getHeight(), radius);
    }

    void clear() {
        View previous = host;
        Veil old = drawable;
        host = null;
        drawable = null;
        if (previous == null) return;
        previous.removeOnAttachStateChangeListener(this);
        if (old != null) previous.getOverlay().remove(old);
    }

    @Override public void onViewAttachedToWindow(View view) {}

    @Override public void onViewDetachedFromWindow(View view) {
        if (view == host) clear();
    }

    private static final class Veil extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private float radius;

        Veil() {
            paint.setColor(Color.BLACK);
            paint.setAlpha(42);
        }

        void geometry(int width, int height, float cornerRadius) {
            float next = Float.isFinite(cornerRadius)
                    ? Math.max(0f, Math.min(cornerRadius, Math.min(width, height) / 2f)) : 0f;
            if (getBounds().width() == width && getBounds().height() == height && radius == next) return;
            radius = next;
            setBounds(0, 0, width, height);
            rect.set(0f, 0f, width, height);
            invalidateSelf();
        }

        @Override public void draw(Canvas canvas) { canvas.drawRoundRect(rect, radius, radius, paint); }
        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
        @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
