package com.zhuhuibin.hypermusiccover.duo;

import java.lang.reflect.Field;

/** Lease the fixed native shell's vertical anchor without importing SystemUI's AndroidX classes. */
final class CompactPlayerBackgroundAnchor {
    private Object target;
    private Field bias, topToTop, topToBottom, bottomToTop, bottomToBottom;
    private final ClockPropertyLease value = new ClockPropertyLease();

    boolean apply(Object params) {
        if (params != target) {
            release();
            target = params;
            bias = topToTop = topToBottom = bottomToTop = bottomToBottom = null;
            if (params != null) try {
                Class<?> type = params.getClass();
                bias = type.getField("verticalBias");
                topToTop = type.getField("topToTop");
                topToBottom = type.getField("topToBottom");
                bottomToTop = type.getField("bottomToTop");
                bottomToBottom = type.getField("bottomToBottom");
            } catch (ReflectiveOperationException ignored) { bias = null; }
        }
        if (bias == null) return false;
        try {
            // Only the retained native fixed-height, parent-to-parent shell is supported.
            if (topToTop.getInt(target) != 0 || bottomToBottom.getInt(target) != 0
                    || topToBottom.getInt(target) != -1 || bottomToTop.getInt(target) != -1)
                return release();
            float current = bias.getFloat(target);
            if (!Float.isFinite(current)) return release();
            float next = value.write(current, 0f);
            if (next == current) return false;
            bias.setFloat(target, next);
            return true;
        } catch (ReflectiveOperationException | IllegalArgumentException ignored) { return false; }
    }

    boolean restore(Object params) {
        boolean current = params == target;
        boolean changed = release();
        target = null;
        bias = topToTop = topToBottom = bottomToTop = bottomToBottom = null;
        return current && changed;
    }

    float bias(Object params) {
        if (params == null) return Float.NaN;
        try {
            Field field = params == target && bias != null ? bias
                    : params.getClass().getField("verticalBias");
            return field.getFloat(params);
        } catch (ReflectiveOperationException | IllegalArgumentException ignored) { return Float.NaN; }
    }

    private boolean release() {
        if (target == null || bias == null) return false;
        try {
            float current = bias.getFloat(target);
            float next = value.restore(current);
            if (next == current) return false;
            bias.setFloat(target, next);
            return true;
        } catch (ReflectiveOperationException | IllegalArgumentException ignored) { return false; }
    }
}
