package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;

import static org.junit.Assert.*;

public class ArtworkFrameLifetimeTest {
    @Test public void preparationWithoutDrawingCannotReleaseAnActivePreviousFrame() {
        ArtworkFrameLifetime frame = new ArtworkFrameLifetime();
        assertFalse(frame.recorded());
        assertFalse(frame.canRetirePrevious(true, true));
        assertFalse(frame.canRetirePrevious(true, false));
        assertFalse(frame.canRetirePrevious(false, true));
    }

    @Test public void aPartialDrawRecordsBitmapUseButCannotRetireThePreviousFrame() {
        ArtworkFrameLifetime frame = new ArtworkFrameLifetime();
        assertFalse(frame.record(true, .5f));
        assertTrue(frame.recorded());
        assertFalse(frame.canRetirePrevious(true, false));
    }

    @Test public void aFullCanvasDrawMustWaitForItsAcknowledgement() {
        ArtworkFrameLifetime frame = new ArtworkFrameLifetime();
        assertTrue(frame.record(true, 1f));
        assertTrue(frame.recorded());
        assertFalse(frame.canRetirePrevious(true, false));
        frame.acknowledge(true);
        assertTrue(frame.canRetirePrevious(true, false));
    }

    @Test public void acknowledgingTheBackdropDoesNotRetireANeededForeground() {
        ArtworkFrameLifetime frame = new ArtworkFrameLifetime();
        assertTrue(frame.record(true, 1f));
        assertTrue(frame.record(false, 1f));
        frame.acknowledge(true);
        assertFalse(frame.canRetirePrevious(true, true));
        frame.acknowledge(false);
        assertTrue(frame.canRetirePrevious(true, true));
    }

    @Test public void duplicateFullDrawsCoalesceWhilePendingAndAfterAcknowledgement() {
        ArtworkFrameLifetime frame = new ArtworkFrameLifetime();
        assertTrue(frame.record(false, .995f));
        assertFalse(frame.record(false, 1f));
        assertFalse(frame.record(false, 1f));
        frame.acknowledge(false);
        assertFalse(frame.record(false, 1f));
        assertTrue(frame.canRetirePrevious(false, true));
    }

    @Test public void aSurfaceNotUsedByTheCurrentSceneDoesNotBlockRetirement() {
        ArtworkFrameLifetime frame = new ArtworkFrameLifetime();
        frame.record(false, 1f);
        frame.acknowledge(false);
        assertTrue(frame.canRetirePrevious(false, true));
        assertFalse(frame.canRetirePrevious(true, true));
    }

    @Test public void anOldFrameAcknowledgementCannotMarkItsReplacementReady() {
        ArtworkFrameLifetime oldFrame = new ArtworkFrameLifetime();
        ArtworkFrameLifetime replacement = new ArtworkFrameLifetime();
        oldFrame.record(true, 1f);
        oldFrame.acknowledge(true);
        assertTrue(oldFrame.canRetirePrevious(true, false));
        assertFalse(replacement.recorded());
        assertFalse(replacement.canRetirePrevious(true, false));
    }

    @Test public void invisibleOrInvalidDrawsDoNotRecordBitmapUse() {
        ArtworkFrameLifetime frame = new ArtworkFrameLifetime();
        for (float coverage : new float[]{Float.NaN, Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY, -1f, 0f, .001f}) {
            assertFalse(frame.record(true, coverage));
            assertFalse(frame.record(false, coverage));
        }
        assertFalse(frame.recorded());
        assertFalse(frame.canRetirePrevious(true, true));
        assertFalse(frame.record(true, Math.nextUp(.001f)));
        assertTrue(frame.recorded());
    }

    @Test public void aSceneUsingOnlyNativeSurfacesHasNoPreparedSurfaceDependency() {
        ArtworkFrameLifetime frame = new ArtworkFrameLifetime();
        assertTrue(frame.canRetirePrevious(false, false));
        assertFalse(frame.recorded());
    }
    @Test public void aMixedDisplayListRetainsBothBitmapsUntilACleanFrame() {
        ArtworkFrameLifetime frame = new ArtworkFrameLifetime();
        frame.used();
        assertTrue(frame.recorded());
        assertFalse(frame.canRetirePrevious(true, false));
        assertTrue(frame.record(true, 1f));
        frame.acknowledge(true);
        assertTrue(frame.canRetirePrevious(true, false));
    }

    @Test public void aReparentedViewCanReplaceItsCanceledFrameAcknowledgement() {
        ArtworkFrameLifetime frame = new ArtworkFrameLifetime();
        assertTrue(frame.record(true, 1f));
        frame.cancel(true);
        assertFalse(frame.canRetirePrevious(true, false));
        assertTrue(frame.record(true, 1f));
        frame.acknowledge(true);
        assertTrue(frame.canRetirePrevious(true, false));
    }

}
