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
}
