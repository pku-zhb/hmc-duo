package com.zhuhuibin.hypermusiccover.duo;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

/** Fixed-size next-page glyphs; lyrics reuse the project's existing attributed icon. */
final class MusicPageActionDrawable extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final LyricsSheetDrawable lyrics = new LyricsSheetDrawable();
    private final Path mountain = new Path();
    private int page = RailMusicPagePolicy.ARTWORK;
    MusicPageActionDrawable() {
        paint.setColor(0xFFFFFFFF);
        paint.setStrokeWidth(1.8f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        mountain.moveTo(4f, 17f); mountain.lineTo(9f, 11f);
        mountain.lineTo(13f, 15f); mountain.lineTo(16f, 12f); mountain.lineTo(20f, 17f);
    }
    void setPage(int page) {
        if (this.page == page) return;
        this.page = page;
        invalidateSelf();
    }
    @Override public void draw(Canvas canvas) {
        if (page == RailMusicPagePolicy.LYRICS) {
            lyrics.setBounds(getBounds()); lyrics.draw(canvas); return;
        }
        float size = Math.min(getBounds().width(), getBounds().height());
        if (size <= 0f) return;
        int save = canvas.save();
        canvas.translate(getBounds().exactCenterX(), getBounds().exactCenterY());
        canvas.scale(size / 24f, size / 24f);
        canvas.translate(-12f, -12f);
        if (page == RailMusicPagePolicy.CLOCK) {
            canvas.drawCircle(12f, 12f, 9f, paint);
            canvas.drawLine(12f, 6f, 12f, 12f, paint);
            canvas.drawLine(12f, 12f, 16f, 14f, paint);
        } else {
            canvas.drawRoundRect(2.5f, 3.5f, 21.5f, 20.5f, 2.8f, 2.8f, paint);
            canvas.drawCircle(16f, 8f, 1.5f, paint);
            canvas.drawPath(mountain, paint);
        }
        canvas.restoreToCount(save);
    }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); lyrics.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); lyrics.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
