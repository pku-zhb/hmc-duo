package com.zhuhuibin.hypermusiccover.duo;

/** Native media time and frame time share a rate only while playback is advancing. */
final class LyricPositionClock {
    private static final float JUMP_MS = 250f;
    private float smoothMs, previousSpeed;
    private long smoothAt, jumpedAt = Long.MIN_VALUE;
    private boolean initialized, wasPlaying;

    static int sessionPosition(long position, boolean playing, float speed, long updatedAt,
                               long elapsedNow) {
        double value = position;
        if (playing && updatedAt > 0L && elapsedNow > updatedAt && Float.isFinite(speed))
            value += (elapsedNow - updatedAt) * (double) speed;
        return value <= 0d ? 0 : (int) Math.min(value, Integer.MAX_VALUE);
    }

    int atFrame(int position, boolean playing, float speed, long frameUptime, long sampledUptime) {
        float rate = Float.isFinite(speed) ? speed : 0f;
        // A paused position is exact. Dispatch latency cannot move it across a lyric boundary.
        float raw = position + (playing ? (frameUptime - sampledUptime) * rate : 0f);
        if (!initialized || !playing || !wasPlaying || rate != previousSpeed) {
            // Paused seeks still animate, but a long pause never becomes elapsed playback.
            if (initialized && Math.abs(raw - smoothMs) > JUMP_MS) jumpedAt = frameUptime;
            smoothMs = raw;
        } else {
            long dt = Math.max(0L, frameUptime - smoothAt);
            float predicted = smoothMs + dt * rate;
            float error = raw - predicted;
            if (Math.abs(error) > JUMP_MS) {
                jumpedAt = frameUptime;
                smoothMs = raw;
            } else smoothMs = predicted + error * Math.min(1f, dt / 300f);
        }
        initialized = true;
        wasPlaying = playing;
        previousSpeed = rate;
        smoothAt = frameUptime;
        return smoothMs < 0f ? 0 : Math.round(smoothMs);
    }

    boolean jumpedRecently(long now, long windowMs) {
        return jumpedAt != Long.MIN_VALUE && now >= jumpedAt && now - jumpedAt < windowMs;
    }
}
