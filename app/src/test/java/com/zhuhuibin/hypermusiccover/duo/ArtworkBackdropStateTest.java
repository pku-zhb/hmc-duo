package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class ArtworkBackdropStateTest {
    @Test public void fullScreenAodBackgroundDoesNotRequireAClockHoldOrVisibleClockContainer() {
        // A native full-AOD scene is sufficient even when the foreground clock never
        // acquired a custom pose or its own container is hidden by native notifications.
        assertTrue(ArtworkBackdropState.eligibleAod(true, false, true, true, false, true));
    }

    @Test public void aodBackgroundCannotLeakIntoNativeOnlyAodOrAnUnlockedWindow() {
        assertFalse(ArtworkBackdropState.eligibleAod(true, false, false, true, false, true));
        assertFalse(ArtworkBackdropState.eligibleAod(true, true, true, true, false, true));
        assertFalse(ArtworkBackdropState.eligibleAod(true, false, true, false, false, true));
        assertFalse(ArtworkBackdropState.eligibleAod(true, false, true, true, true, true));
        assertFalse(ArtworkBackdropState.eligibleAod(true, false, true, true, false, false));
        assertFalse(ArtworkBackdropState.eligibleAod(false, false, true, true, false, true));
    }

    @Test public void aodSamplingRequiresCurrentVisiblePixelsRatherThanAnOldVisibleWash() {
        assertTrue(ArtworkBackdropState.presentedFrame(4, 4, 64f / 255f));
        assertFalse(ArtworkBackdropState.presentedFrame(5, 4, 64f / 255f));
        assertFalse(ArtworkBackdropState.presentedFrame(-1, -1, 1f));
        assertFalse(ArtworkBackdropState.presentedFrame(4, 4, 0f));
        assertFalse(ArtworkBackdropState.presentedFrame(4, 4, Float.NaN));
        assertFalse(ArtworkBackdropState.presentedFrame(4, 4, Float.POSITIVE_INFINITY));
    }

    @Test public void preparingASourceDoesNotRevealItBeforeAnActualDraw() {
        ArtworkBackdropState state = new ArtworkBackdropState();
        state.select(4);
        assertFalse(state.reveal(state.epoch(), 4));
        state.drawn(4);
        assertTrue(state.reveal(state.epoch(), 4));
    }

    @Test public void aNewGenerationCannotBeRevealedByThePreviousFramesCallback() {
        ArtworkBackdropState state = new ArtworkBackdropState();
        state.select(4);
        state.drawn(4);
        long callback = state.epoch();
        state.select(5);
        assertFalse(state.reveal(callback, 4));
        assertFalse(state.reveal(callback, 5));
        state.drawn(5);
        assertTrue(state.reveal(state.epoch(), 5));
    }

    @Test public void unlockingOrDozingCancelsAQueuedFirstFrameFade() {
        ArtworkBackdropState state = new ArtworkBackdropState();
        state.select(7);
        state.drawn(7);
        long oldScene = state.epoch();
        state.hide();
        assertFalse(state.reveal(oldScene, 7));
        state.select(7);
        state.drawn(7);
        assertFalse(state.reveal(oldScene, 7));
        assertTrue(state.reveal(state.epoch(), 7));
    }

    @Test public void allUnlockAndNativeBackgroundGatesTakePriorityOverCachedArtwork() {
        assertTrue(ArtworkBackdropState.eligible(true, true, true, true, false, true));
        assertFalse(ArtworkBackdropState.eligible(false, true, true, true, false, true));
        assertFalse(ArtworkBackdropState.eligible(true, false, true, true, false, true));
        assertFalse(ArtworkBackdropState.eligible(true, true, false, true, false, true));
        assertFalse(ArtworkBackdropState.eligible(true, true, true, false, false, true));
        assertFalse(ArtworkBackdropState.eligible(true, true, true, true, true, true));
        assertFalse(ArtworkBackdropState.eligible(true, true, true, true, false, false));
    }

    @Test public void preparingTheReplacementKeepsOnlyTheActuallyDrawnOutgoingRenderer() {
        ArtworkBackdropState.Handoff handoff = new ArtworkBackdropState.Handoff();
        Object window = new Object();
        handoff.select(1L, ArtworkBackdropState.MESH, window, 7L, 4);
        assertFalse(handoff.local());
        handoff.presented(1L, ArtworkBackdropState.MESH, window, 7L, 4, 1f);
        handoff.select(2L, ArtworkBackdropState.STILL, window, 7L, 4);
        assertTrue(handoff.local());
        assertTrue(handoff.retains(ArtworkBackdropState.MESH));
        handoff.presented(2L, ArtworkBackdropState.STILL, window, 7L, 4, .25f);
        assertEquals(ArtworkBackdropState.MESH, handoff.owner());
        handoff.presented(2L, ArtworkBackdropState.STILL, window, 7L, 4, 1f);
        assertEquals(ArtworkBackdropState.STILL, handoff.owner());
        assertFalse(handoff.retains(ArtworkBackdropState.MESH));
    }

    @Test public void wakingKeepsTheStaticPixelsUntilTheMeshHasActuallyCoveredThem() {
        ArtworkBackdropState.Handoff handoff = new ArtworkBackdropState.Handoff();
        Object window = new Object();
        handoff.select(8L, ArtworkBackdropState.STILL, window, 2L, 6);
        handoff.presented(8L, ArtworkBackdropState.STILL, window, 2L, 6, 1f);
        handoff.select(9L, ArtworkBackdropState.MESH, window, 2L, 6);
        assertTrue(handoff.retains(ArtworkBackdropState.STILL));
        assertTrue(handoff.local());
        handoff.presented(9L, ArtworkBackdropState.MESH, window, 2L, 6, .9f);
        assertEquals(ArtworkBackdropState.STILL, handoff.owner());
        handoff.presented(9L, ArtworkBackdropState.MESH, window, 2L, 6, 1f);
        assertEquals(ArtworkBackdropState.MESH, handoff.owner());
    }

    @Test public void aNativeSceneRevokesOwnershipAndRejectsLateFramesEvenForTheSameBitmap() {
        ArtworkBackdropState.Handoff handoff = new ArtworkBackdropState.Handoff();
        Object window = new Object();
        handoff.select(1L, ArtworkBackdropState.MESH, window, 0L, 4);
        handoff.presented(1L, ArtworkBackdropState.MESH, window, 0L, 4, 1f);
        handoff.select(2L, ArtworkBackdropState.NATIVE, window, 0L, 4);
        assertFalse(handoff.local());
        handoff.select(3L, ArtworkBackdropState.MESH, window, 0L, 4);
        assertFalse(handoff.presented(1L, ArtworkBackdropState.MESH, window, 0L, 4, 1f));
        assertFalse(handoff.local());
        assertTrue(handoff.presented(3L, ArtworkBackdropState.MESH, window, 0L, 4, 1f));
        assertTrue(handoff.local());
    }

    @Test public void windowGeometryAndSourceChangesCannotRetainAStaleDisplayList() {
        ArtworkBackdropState.Handoff handoff = new ArtworkBackdropState.Handoff();
        Object first = new Object(), second = new Object();
        handoff.select(1L, ArtworkBackdropState.MESH, first, 0L, 4);
        handoff.presented(1L, ArtworkBackdropState.MESH, first, 0L, 4, 1f);
        handoff.select(2L, ArtworkBackdropState.STILL, second, 0L, 4);
        assertFalse(handoff.local());
        assertFalse(handoff.presented(2L, ArtworkBackdropState.STILL, first, 0L, 4, 1f));
        handoff.presented(2L, ArtworkBackdropState.STILL, second, 0L, 4, 1f);
        handoff.select(3L, ArtworkBackdropState.MESH, second, 1L, 4);
        assertFalse(handoff.local());
        handoff.presented(3L, ArtworkBackdropState.MESH, second, 1L, 4, 1f);
        handoff.select(4L, ArtworkBackdropState.STILL, second, 1L, 5);
        assertFalse(handoff.local());
        assertFalse(handoff.presented(4L, ArtworkBackdropState.STILL, second, 1L, 4, 1f));
    }

    @Test public void aSameBitmapPageChangeKeepsValidPixelsButRejectsOldCallbacks() {
        ArtworkBackdropState.Handoff handoff = new ArtworkBackdropState.Handoff();
        Object window = new Object();
        handoff.select(1L, ArtworkBackdropState.MESH, window, 0L, 4);
        handoff.presented(1L, ArtworkBackdropState.MESH, window, 0L, 4, 1f);
        handoff.select(2L, ArtworkBackdropState.MESH, window, 0L, 4);
        assertTrue(handoff.local());
        assertFalse(handoff.presented(1L, ArtworkBackdropState.STILL, window, 0L, 4, 1f));
        assertEquals(ArtworkBackdropState.MESH, handoff.owner());
    }

    @Test public void aHiddenOrZeroAlphaOwnerDoesNotAuthorizeLocalSampling() {
        ArtworkBackdropState.Handoff handoff = new ArtworkBackdropState.Handoff();
        Object window = new Object();
        handoff.select(1L, ArtworkBackdropState.MESH, window, 0L, 4);
        handoff.presented(1L, ArtworkBackdropState.MESH, window, 0L, 4, 1f);
        handoff.presented(1L, ArtworkBackdropState.MESH, window, 0L, 4, 0f);
        assertFalse(handoff.local());
        handoff.presented(1L, ArtworkBackdropState.MESH, window, 0L, 4, 1f);
        handoff.withdraw(ArtworkBackdropState.MESH);
        assertFalse(handoff.local());
    }

    @Test public void anUnrequestedRendererCannotStealTheSamplingOwner() {
        ArtworkBackdropState.Handoff handoff = new ArtworkBackdropState.Handoff();
        Object window = new Object();
        handoff.select(1L, ArtworkBackdropState.STILL, window, 0L, 4);
        handoff.presented(1L, ArtworkBackdropState.MESH, window, 0L, 4, 1f);
        assertFalse(handoff.local());
        handoff.presented(1L, ArtworkBackdropState.STILL, window, 0L, 4, .1f);
        assertEquals(ArtworkBackdropState.STILL, handoff.owner());
    }
}
