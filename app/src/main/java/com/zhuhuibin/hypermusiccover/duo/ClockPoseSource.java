package com.zhuhuibin.hypermusiccover.duo;

import java.lang.ref.WeakReference;

/** A pose belongs to measured native nodes, not merely their still-live SystemUI window. */
final class ClockPoseSource {
    private WeakReference<Object> window = new WeakReference<>(null);
    private WeakReference<Object> clock = new WeakReference<>(null);
    private WeakReference<Object> date = new WeakReference<>(null);
    private long revision = -1L;
    private boolean hadDate;

    void capture(Object window, long revision, Object clock, Object date) {
        this.window = new WeakReference<>(window);
        this.revision = revision;
        this.clock = new WeakReference<>(clock);
        this.date = new WeakReference<>(date);
        hadDate = date != null;
    }

    boolean matches(Object window, long revision, Object clock, Object date) {
        return window != null && clock != null && this.window.get() == window
                && this.revision == revision && this.clock.get() == clock
                && (date == null ? !hadDate : this.date.get() == date);
    }
}
