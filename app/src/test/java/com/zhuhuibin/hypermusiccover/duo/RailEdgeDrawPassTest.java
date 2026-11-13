package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class RailEdgeDrawPassTest {
    private static final class Target implements RailEdgeDrawPass.Target<String> {
        String recording = "recording", drawn;
        boolean failBegin, failComplete, completed, outputStarted;
        int aborts, recoveries, failures;
        @Override public String begin(String original) {
            if (failBegin) throw new IllegalStateException("shader unavailable");
            return recording;
        }
        @Override public void complete(String original) {
            if (failComplete) throw new IllegalStateException("GPU setup unavailable");
            completed = true;
        }
        @Override public void abort() { aborts++; }
        @Override public void recover(String original) { if (!outputStarted) recoveries++; }
        @Override public void failed(Throwable error) { failures++; }
    }

    @Test public void supportedPassInvokesNativeOnceAndKeepsItsReturnValue() throws Throwable {
        Target target = new Target();
        int[] calls = {0};
        Object expected = new Object();
        Object result = RailEdgeDrawPass.draw("original", target, canvas -> {
            calls[0]++; target.drawn = canvas; return expected;
        });
        assertSame(expected, result);
        assertEquals(1, calls[0]);
        assertEquals("recording", target.drawn);
        assertTrue(target.completed);
    }

    @Test public void zeroMotionSoftwareAndUnknownGeometryStayOnOriginalCanvas() throws Throwable {
        Target target = new Target();
        target.recording = null;
        int[] calls = {0};
        RailEdgeDrawPass.draw("original", target, canvas -> {
            calls[0]++; assertEquals("original", canvas); return null;
        });
        assertEquals(1, calls[0]);
        assertFalse(target.completed);
        assertEquals(0, target.failures);
    }

    @Test public void setupFailureFallsBackBeforeEnteringNative() throws Throwable {
        Target target = new Target();
        target.failBegin = true;
        int[] calls = {0};
        RailEdgeDrawPass.draw("original", target, canvas -> {
            calls[0]++; assertEquals("original", canvas); return null;
        });
        assertEquals(1, calls[0]);
        assertEquals(1, target.aborts);
        assertEquals(1, target.failures);
    }

    @Test public void postNativeFailureReplaysCommandsWithoutReenteringNative() throws Throwable {
        Target target = new Target();
        target.failComplete = true;
        int[] calls = {0};
        RailEdgeDrawPass.draw("original", target, canvas -> { calls[0]++; return null; });
        assertEquals(1, calls[0]);
        assertEquals(1, target.recoveries);
        assertEquals(1, target.failures);
    }

    @Test public void partialOutputDoesNotReplayOnTopOfNativeAlpha() throws Throwable {
        Target target = new Target();
        target.failComplete = target.outputStarted = true;
        int[] calls = {0};
        RailEdgeDrawPass.draw("original", target, canvas -> { calls[0]++; return null; });
        assertEquals(1, calls[0]);
        assertEquals(0, target.recoveries);
    }

    @Test public void nativeFailurePropagatesWithoutASecondDraw() throws Throwable {
        Target target = new Target();
        RuntimeException expected = new RuntimeException("native draw");
        int[] calls = {0};
        try {
            RailEdgeDrawPass.draw("original", target, canvas -> { calls[0]++; throw expected; });
            fail("native exception must propagate");
        } catch (RuntimeException actual) { assertSame(expected, actual); }
        assertEquals(1, calls[0]);
        assertEquals(1, target.aborts);
        assertEquals(0, target.recoveries);
        assertEquals(0, target.failures);
    }
}
