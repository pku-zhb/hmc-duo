package com.zhuhuibin.hypermusiccover.duo;

import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.TextUtils;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.Locale;

/** Read-only adapters for native date drawing. Neither the date strings nor its icons are replaced. */
final class CompactClockRowDate {
    private CompactClockRowDate() {}
    private static final String CLASSIC = "com.miui.clock.classic.ClassicTextAreaView";
    private static final ClassValue<Method> POINT_ICON = new ClassValue<Method>() {
        @Override protected Method computeValue(Class<?> type) {
            try {
                Class<?> style = Class.forName("com.miui.clock.module.ClassicContentStyle", false,
                        type.getClassLoader());
                Method method = style.getDeclaredMethod("isNeedPointIcon", int.class);
                method.setAccessible(true);
                return method;
            } catch (ReflectiveOperationException failure) { return null; }
        }
    };

    static boolean classic(TextView text) {
        for (Class<?> type = text.getClass(); type != null; type = type.getSuperclass())
            if (CLASSIC.equals(type.getName())) return true;
        return false;
    }

    static final class Measurement {
        final CompactClockRowLayout.DateInk ink;
        final float top, bottom;
        final String description;
        Measurement(CompactClockRowLayout.DateInk ink, float top, float bottom, String description) {
            this.ink = ink; this.top = top; this.bottom = bottom; this.description = description;
        }
    }

    static Measurement read(TextView text) {
        if (classic(text)) {
            try { return readClassic(text); }
            catch (Throwable failure) {
                return unavailable("classic-canvas unavailable=" + failure.getClass().getSimpleName());
            }
        }
        Layout layout = text.getLayout();
        if (layout == null) return unavailable("text-layout awaiting layout");
        if (layout.getLineCount() != 1) return unavailable("text-layout multiline");
        Paint.FontMetrics fm = text.getPaint().getFontMetrics();
        return new Measurement(new CompactClockRowLayout.DateInk(
                text.getCompoundPaddingLeft() + layout.getLineLeft(0),
                layout.getLineRight(0) - layout.getLineLeft(0), text.getBaseline()),
                fm.ascent, fm.descent, "text-layout");
    }

    /** Matches stock 17.03 ClassicTextAreaView.onDraw/onMeasure: onMeasure never constructs Layout,
     * and onDraw uses getPaint/getCurrentTextColor, dateForeground + getText, and a centered baseline. */
    private static Measurement readClassic(TextView text) throws ReflectiveOperationException {
        CharSequence foreground = (CharSequence) Xp.getObjectField(text, "dateForeground");
        CharSequence body = text.getText();
        if (foreground == null || body == null) return unavailable("classic-canvas text unavailable");
        Paint paint = text.getPaint();
        Paint.FontMetrics fm = paint.getFontMetrics();
        float prefixWidth = paint.measureText(foreground.toString());
        float bodyWidth = paint.measureText(body.toString());
        boolean needIcon = flag(text, "mNeedIcon");
        Drawable point = (Drawable) Xp.getObjectField(text, "mPointIcon");
        Drawable weather = (Drawable) Xp.getObjectField(text, "mWeatherIcon");
        Drawable health = (Drawable) Xp.getObjectField(text, "mHealthIcon");
        float healthSize = number(text, "mIconWH");
        float weatherSize = number(text, "mWeatherIconWH");
        float weatherMargin = number(text, "mWeatherIconMargin");
        boolean drawPoint = false;
        if (!needIcon && point != null) {
            Method method = POINT_ICON.get(text.getClass());
            if (method == null) return unavailable("classic-canvas point rule unavailable");
            drawPoint = Boolean.TRUE.equals(method.invoke(null, (int) number(text, "mCurrentLineType")));
        }
        float pointWidth = drawPoint ? number(text, "mPointIconWidth") : 0f;
        float pointHeight = drawPoint ? number(text, "mPointIconHeight") : 0f;
        float measuredHealth = flag(text, "mHasHealthIcon") && health != null ? healthSize : 0f;
        float measuredWeather = flag(text, "mHasWeatherIcon") && weather != null
                ? weatherSize + weatherMargin * 2f : 0f;
        // DeviceConfig.isRTL uses the default locale, not this TextView's resolved layout direction.
        boolean rtl = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == 1;
        float advance = 0f, extent = 0f, iconHeight = 0f;
        if (drawPoint) {
            advance = pointWidth;
            extent = Math.max(pointWidth, point.getIntrinsicHeight() > 0
                    ? pointHeight * point.getIntrinsicWidth() / point.getIntrinsicHeight() : pointWidth);
            iconHeight = pointHeight;
        } else if (!needIcon && point == null && weather != null) {
            advance = extent = weatherSize + weatherMargin * 2f;
            iconHeight = weatherSize;
        } else if (needIcon && health != null) {
            advance = rtl ? measuredHealth : healthSize;
            extent = iconHeight = healthSize;
        }
        boolean rect = flag(text, "needDrawRect");
        float rectangleWidth = rect ? number(text, "rectWidth") : 0f;
        float rectangleHeight = rect ? Math.max(0f,
                number(text, "rectHeight") - 2f * number(text, "rectPadding")) : 0f;
        CompactClockRowLayout.DateInk ink = CompactClockRowLayout.classicDate(
                text.getMeasuredWidth(), text.getMeasuredHeight(), text.getPaddingStart(),
                text.getPaddingEnd(), prefixWidth, bodyWidth,
                measuredHealth + measuredWeather + pointWidth, advance, extent,
                rectangleWidth, rtl, fm.ascent, fm.descent);
        if (ink == null) return unavailable("classic-canvas geometry unavailable");
        float center = text.getMeasuredHeight() / 2f - ink.baseline;
        float halfIcon = Math.max(iconHeight, rectangleHeight) / 2f;
        return new Measurement(ink, Math.min(fm.ascent, center - halfIcon),
                Math.max(fm.descent, center + halfIcon),
                "classic-canvas textLength=" + body.length() + "/" + foreground.length()
                        + " measured=" + text.getMeasuredWidth() + "x" + text.getMeasuredHeight()
                        + " baseline=" + Main.r1(ink.baseline) + "/" + text.getBaseline()
                        + " paint=" + Main.r1(paint.getTextSize())
                        + " color=" + Integer.toHexString(text.getCurrentTextColor())
                        + " ink=" + Main.r1(ink.left) + ":" + Main.r1(ink.width)
                        + " icons=" + Main.r1(advance) + " rtl=" + rtl);
    }

    private static Measurement unavailable(String reason) { return new Measurement(null, 0, 0, reason); }
    private static boolean flag(Object target, String field) { return (Boolean) Xp.getObjectField(target, field); }
    private static float number(Object target, String field) {
        return ((Number) Xp.getObjectField(target, field)).floatValue();
    }
}
