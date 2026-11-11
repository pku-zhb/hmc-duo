package com.zhuhuibin.hypermusiccover.duo;

/** Text's real horizontal interval after visible native controls have reserved their bounds. */
final class MediaTextBoundsPolicy {
    static final class Bounds {
        final float left, top, right, bottom;
        Bounds(float left, float top, float right, float bottom) {
            this.left = left; this.top = top; this.right = right; this.bottom = bottom;
        }
        boolean intersects(Bounds b) {
            return left < b.right && right > b.left && top < b.bottom && bottom > b.top;
        }
    }
    static final class Insets {
        final int left, right;
        Insets(int left, int right) { this.left = left; this.right = right; }
    }
    private MediaTextBoundsPolicy() {}
    static Insets reserve(Bounds text, int baseLeft, int baseRight, float leading,
                           Iterable<Bounds> controls) {
        float left = text.left + baseLeft + Math.max(0f, leading);
        float right = text.right - baseRight;
        for (Bounds control : controls) {
            if (!text.intersects(control)) continue;
            // Controls overlapping the existing leading slot reserve that side; the other
            // controls cap the trailing edge. Rows below the text reserve no horizontal room.
            if (control.left < left && control.right > left) left = control.right;
            else if (control.left >= left) right = Math.min(right, control.left);
        }
        int width = Math.max(0, Math.round(text.right - text.left));
        int l = Math.min(width, Math.max(0, (int) Math.ceil(left - text.left)));
        int r = Math.min(width - l, Math.max(baseRight, (int) Math.ceil(text.right - right)));
        return new Insets(l, Math.max(0, r));
    }
}
