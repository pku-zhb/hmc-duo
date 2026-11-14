package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class LyricPositionClockTest {
    @Test public void pausedPositionNeverTracksDispatchLatencyAcrossAnAdjacentLyricBoundary() {
        LyricPositionClock clock = new LyricPositionClock();
        long start = 536_000_000L;
        for (int frame = 0; frame < 2000; frame++) {
            long now = start + frame * 8L;
            int time = clock.atFrame(1530, false, 1f, now, now + frame % 26);
            assertEquals(1530, time);
            assertTrue("the line starting at 1525 must remain focused", time >= 1525);
            assertFalse(clock.jumpedRecently(now, 250L));
        }
    }

    @Test public void pausedNativeTimestampAndSpeedCannotAdvanceThePlayhead() {
        assertEquals(1530, LyricPositionClock.sessionPosition(1530, false, 1f, 1000, 600_000));
        assertEquals(1530, LyricPositionClock.sessionPosition(1530, false, 2f, 500, 900_000));
        assertEquals(1530, LyricPositionClock.sessionPosition(1530, false, 0f, 0, 900_000));
    }

    @Test public void pausedSeeksTakeTheirExactNewTimeAndKeepTheFiniteSeekAnimationWindow() {
        LyricPositionClock clock = new LyricPositionClock();
        clock.atFrame(1530, false, 0f, 1000, 1017);
        assertEquals(10_000, clock.atFrame(10_000, false, 0f, 1100, 1125));
        assertTrue(clock.jumpedRecently(1100, 250));
        assertTrue(clock.jumpedRecently(1349, 250));
        assertFalse(clock.jumpedRecently(1350, 250));
        assertEquals(900, clock.atFrame(900, false, 0f, 2000, 2025));
        assertTrue(clock.jumpedRecently(2000, 250));
    }

    @Test public void resumingAfterAnIdlePauseDoesNotPredictPlaybackDuringThePause() {
        LyricPositionClock clock = new LyricPositionClock();
        clock.atFrame(1530, false, 0f, 1000, 1020);
        assertEquals(1538, clock.atFrame(1538, true, 1f, 601_008, 601_008));
        assertFalse(clock.jumpedRecently(601_008, 250));
        assertEquals(1554, clock.atFrame(1554, true, 1f, 601_024, 601_024));
    }

    @Test public void playingProjectionUsesTheNativeTimestampAndOnlyThenTheFrameOffset() {
        LyricPositionClock clock = new LyricPositionClock();
        int sample = LyricPositionClock.sessionPosition(10_000, true, 1f, 100_000, 100_123);
        assertEquals(10_123, sample);
        assertEquals(10_115, clock.atFrame(sample, true, 1f, 100_115, 100_123));
        // A fresh native timestamp describing the same timeline must not introduce a seek.
        sample = LyricPositionClock.sessionPosition(10_120, true, 1f, 100_120, 100_139);
        assertEquals(10_131, clock.atFrame(sample, true, 1f, 100_131, 100_139));
        assertFalse(clock.jumpedRecently(100_131, 250));
    }

    @Test public void playbackRateAppliesToBothNativeProjectionAndFramePrediction() {
        LyricPositionClock clock = new LyricPositionClock();
        assertEquals(10_200, LyricPositionClock.sessionPosition(10_000, true, 2f, 1000, 1100));
        assertEquals(10_184, clock.atFrame(10_200, true, 2f, 1092, 1100));
        assertEquals(10_216, clock.atFrame(10_232, true, 2f, 1108, 1116));
        assertFalse(clock.jumpedRecently(1108, 250));
    }

    @Test public void smallPlayingCorrectionsEaseButASeekMovesToItsNewTimelineImmediately() {
        LyricPositionClock clock = new LyricPositionClock();
        clock.atFrame(1000, true, 1f, 1000, 1000);
        int eased = clock.atFrame(1040, true, 1f, 1016, 1016);
        assertTrue(eased > 1016 && eased < 1040);
        assertFalse(clock.jumpedRecently(1016, 250));
        assertEquals(5000, clock.atFrame(5000, true, 1f, 1032, 1032));
        assertTrue(clock.jumpedRecently(1032, 250));
    }

    @Test public void anUninitializedOrFutureNativeTimestampNeverInventsElapsedPlayback() {
        assertEquals(1530, LyricPositionClock.sessionPosition(1530, true, 1f, 0, 600_000));
        assertEquals(1530, LyricPositionClock.sessionPosition(1530, true, 1f, 600_001, 600_000));
        assertEquals(0, LyricPositionClock.sessionPosition(-1, false, 1f, 1000, 2000));
        assertEquals(Integer.MAX_VALUE, LyricPositionClock.sessionPosition(Integer.MAX_VALUE - 2,
                true, 2f, 1000, 2000));
    }
}
