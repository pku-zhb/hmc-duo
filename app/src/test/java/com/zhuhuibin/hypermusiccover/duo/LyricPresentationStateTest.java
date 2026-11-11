package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class LyricPresentationStateTest {
    @Test public void sleepUsesAmbientTextBeforeTheScreenOffBroadcast() {
        LyricPresentationState state = new LyricPresentationState();
        assertFalse(state.update(true, false, true));
        assertTrue(state.update(true, true, false));
        assertTrue(state.update(false, true, false));
    }

    @Test public void wakeKeepsOneTextTreatmentUntilClockAndScreenAgree() {
        LyricPresentationState state = new LyricPresentationState();
        assertTrue(state.update(false, true, false));
        assertTrue(state.update(true, true, false));
        assertTrue(state.update(true, false, false));
        assertFalse(state.update(true, false, true));
        assertFalse(state.update(true, false, false));
    }

    @Test public void lateScreenOnAndInterruptedWakeDoNotRearmWordEffects() {
        LyricPresentationState state = new LyricPresentationState();
        assertTrue(state.update(false, true, false));
        assertTrue(state.update(false, false, true));
        assertTrue(state.update(true, false, false));
        assertTrue(state.update(true, true, false));
        assertTrue(state.update(false, true, false));
        assertFalse(state.update(true, false, true));
    }

    @Test public void ordinaryAwakePageEntryKeepsKaraokeAnimation() {
        LyricPresentationState state = new LyricPresentationState();
        assertFalse(state.update(true, false, false));
        assertFalse(state.update(true, false, true));
    }

    @Test public void hiddenArtworkDoesNotDelayLyricsForItsBlurAcknowledgement() {
        assertFalse(LyricPresentationState.waitsForBlur(true, true));
        assertTrue(LyricPresentationState.waitsForBlur(true, false));
        assertFalse(LyricPresentationState.waitsForBlur(false, false));
    }
}
