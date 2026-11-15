package com.zhuhuibin.hypermusiccover.duo;

/** Observe a short stationary tap; drags and native long presses keep their original stream. */
final class RailStatusTap {
    private boolean tracking;
    private float x, y, slop;
    private long downTime;

    void start(boolean inside, float x, float y, float slop, long downTime) {
        tracking = inside && FoldMusicLayout.finite(x, y, slop) && slop >= 0f;
        this.x = x; this.y = y; this.slop = slop; this.downTime = downTime;
    }

    void move(float x, float y, int pointers, long stream) {
        if (pointers != 1 || stream != downTime || !FoldMusicLayout.finite(x, y)
                || Math.hypot(x - this.x, y - this.y) > slop) tracking = false;
    }

    boolean finish(boolean inside, float x, float y, int pointers, long stream,
                   long eventTime, long longPressTimeout) {
        move(x, y, pointers, stream);
        boolean tap = tracking && inside && eventTime >= downTime
                && eventTime - downTime < longPressTimeout;
        cancel();
        return tap;
    }

    void cancel() { tracking = false; }
}
