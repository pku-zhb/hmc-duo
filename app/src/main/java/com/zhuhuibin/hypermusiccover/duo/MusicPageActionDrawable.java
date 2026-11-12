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
    private int lyricsAvailability = LyricsButtonPolicy.AVAILABLE;
    private int alpha = 255;
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
    void setLyricsAvailability(int availability) {
        if (lyricsAvailability == availability) return;
        lyricsAvailability = availability;
        invalidateSelf();
    }
    @Override public void draw(Canvas canvas) {
        int availability = LyricsButtonPolicy.actionAvailability(page, lyricsAvailability);
        if (page == RailMusicPagePolicy.LYRICS) {
            lyrics.setAlpha(availability == LyricsButtonPolicy.AVAILABLE ? alpha : Math.round(alpha * .42f));
            lyrics.setBounds(getBounds()); lyrics.draw(canvas);
            if (availability == LyricsButtonPolicy.AVAILABLE) return;
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
        } else if (page == RailMusicPagePolicy.ARTWORK_AND_LYRICS) {
            canvas.drawRoundRect(1.5f, 4f, 11f, 20f, 1.5f, 1.5f, paint);
            canvas.drawLine(3.5f, 16f, 6f, 12f, paint);
            canvas.drawLine(6f, 12f, 9f, 16f, paint);
            canvas.drawLine(14f, 6f, 22f, 6f, paint);
            canvas.drawLine(14f, 10f, 22f, 10f, paint);
            canvas.drawLine(14f, 14f, 20f, 14f, paint);
            canvas.drawLine(14f, 18f, 22f, 18f, paint);
        } else if (page != RailMusicPagePolicy.LYRICS) {
            canvas.drawRoundRect(2.5f, 3.5f, 21.5f, 20.5f, 2.8f, 2.8f, paint);
            canvas.drawCircle(16f, 8f, 1.5f, paint);
            canvas.drawPath(mountain, paint);
        }
        if (availability == LyricsButtonPolicy.UNAVAILABLE) {
            // A diagonal strike distinguishes a completed miss from a still-loading lyric.
            canvas.drawLine(3f, 3f, 21f, 21f, paint);
        } else if (availability == LyricsButtonPolicy.LOADING) {
            // Static pending dots add no frame loop to the lock screen or AOD.
            paint.setStyle(Paint.Style.FILL);
            for (int x = 8; x <= 16; x += 4) canvas.drawCircle(x, 21f, 1.1f, paint);
            paint.setStyle(Paint.Style.STROKE);
        }
        canvas.restoreToCount(save);
    }
    @Override public void setAlpha(int alpha) { this.alpha = alpha; paint.setAlpha(alpha); lyrics.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); lyrics.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
