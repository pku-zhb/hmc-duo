package com.zhuhuibin.hypermusiccover.duo;

import java.lang.ref.WeakReference;

/** Absolute AOD progress from native events, independent of artwork/page animation clocks. */
final class AodTransitionProgress {
    static final class Snapshot {
        final long epoch;
        final float fraction;
        final boolean running, into;
        Snapshot(long epoch, float fraction, boolean running, boolean into) {
            this.epoch = epoch; this.fraction = fraction; this.running = running; this.into = into;
        }
    }

    private WeakReference<Object> window = new WeakReference<>(null);
    private long revision = -1L, epoch;
    private float fraction, from;
    private boolean running, into, interrupted, completed;

    private boolean same(Object nextWindow, long nextRevision) {
        return nextWindow != null && window.get() == nextWindow && revision == nextRevision;
    }

    private void domain(Object nextWindow, long nextRevision, boolean aod) {
        if (same(nextWindow, nextRevision)) return;
        window = new WeakReference<>(nextWindow); revision = nextRevision;
        fraction = aod ? 1f : 0f; from = fraction;
        running = interrupted = completed = false; into = aod; epoch++;
    }

    synchronized void scene(Object nextWindow, long nextRevision, boolean aod) {
        scene(nextWindow, nextRevision, aod, false);
    }

    synchronized void scene(Object nextWindow, long nextRevision, boolean aod, boolean nativeAnimationPlanned) {
        domain(nextWindow, nextRevision, aod);
        // setIsDozing can precede the native animation's final frame. It is an endpoint only
        // when no native run owns the fraction; cold entry needs no synthetic frame/timer.
        if (nativeAnimationPlanned) {
            if (!running || into != aod) begin(nextWindow, nextRevision, aod);
        } else if (!running) {
            fraction = aod ? 1f : 0f; into = aod; epoch++;
            interrupted = false;
        }
    }

    synchronized void begin(Object nextWindow, long nextRevision, boolean targetAod) {
        domain(nextWindow, nextRevision, !targetAod);
        from = running || interrupted ? fraction : targetAod ? 0f : 1f;
        fraction = from; into = targetAod; running = true; interrupted = completed = false; epoch++;
    }

    synchronized void frame(Object nextWindow, long nextRevision, boolean targetAod, float localFraction) {
        if (!Float.isFinite(localFraction)) return;
        if (same(nextWindow, nextRevision) && completed && into == targetAod) return;
        if (!same(nextWindow, nextRevision) || !running || into != targetAod)
            begin(nextWindow, nextRevision, targetAod);
        float t = Math.max(0f, Math.min(1f, localFraction));
        fraction = from + ((targetAod ? 1f : 0f) - from) * t;
        epoch++;
    }

    synchronized void complete(Object nextWindow, long nextRevision, boolean targetAod) {
        domain(nextWindow, nextRevision, targetAod);
        fraction = targetAod ? 1f : 0f; into = targetAod;
        running = interrupted = false; completed = true; epoch++;
    }

    synchronized void cancel(Object nextWindow, long nextRevision) {
        if (!same(nextWindow, nextRevision) || !running) return;
        // A reversal starts from the last native frame, rather than completing the outgoing
        // direction and jumping back on the following onBegin.
        running = false; interrupted = true; epoch++;
    }

    synchronized Snapshot snapshot(Object nextWindow, long nextRevision, boolean aod) {
        return same(nextWindow, nextRevision)
                ? new Snapshot(epoch, fraction, running, into)
                : new Snapshot(epoch, aod ? 1f : 0f, false, aod);
    }
}
