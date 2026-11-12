package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class LyricsButtonPolicyTest {
    @Test public void newTrackTransitionsThroughPendingMissingAndLateProviderContent() {
        assertEquals(LyricsButtonPolicy.AVAILABLE, LyricsButtonPolicy.availability(true, true, false));
        // Presentation can retain the previous page, but this song has no answer yet.
        assertEquals(LyricsButtonPolicy.LOADING, LyricsButtonPolicy.availability(true, false, true));
        assertEquals(LyricsButtonPolicy.UNAVAILABLE, LyricsButtonPolicy.availability(true, false, false));
        assertEquals(LyricsButtonPolicy.AVAILABLE, LyricsButtonPolicy.availability(true, true, false));
    }
    @Test public void rereadingAUsableLyricDoesNotReplaceItsIconWithPending() {
        assertEquals(LyricsButtonPolicy.AVAILABLE, LyricsButtonPolicy.availability(true, true, true));
        assertEquals(LyricsButtonPolicy.UNAVAILABLE, LyricsButtonPolicy.availability(false, true, true));
    }
    @Test public void unavailableLyricsDoNotDimArtworkOrBlockTheReturnToClock() {
        for (int availability : new int[]{LyricsButtonPolicy.LOADING, LyricsButtonPolicy.UNAVAILABLE}) {
            assertEquals(LyricsButtonPolicy.AVAILABLE, LyricsButtonPolicy.actionAvailability(RailMusicPagePolicy.ARTWORK, availability));
            assertEquals(LyricsButtonPolicy.AVAILABLE, LyricsButtonPolicy.actionAvailability(RailMusicPagePolicy.CLOCK, availability));
            assertEquals(availability, LyricsButtonPolicy.actionAvailability(RailMusicPagePolicy.LYRICS, availability));
            assertEquals(availability, LyricsButtonPolicy.actionAvailability(RailMusicPagePolicy.ARTWORK_AND_LYRICS, availability));
            assertEquals(RailMusicPagePolicy.CLOCK, RailMusicPagePolicy.next(RailMusicPagePolicy.LYRICS, true));
        }
    }
    @Test public void defaultArtworkAndNativePlayerKeepAnAvailableLyricsButton() {
        assertTrue(LyricsButtonPolicy.offer(true, true, true, true, false));
        assertTrue(LyricsButtonPolicy.offer(true, false, true, true, false));
        assertFalse(LyricsButtonPolicy.offer(true, true, true, false, true));
        assertFalse(LyricsButtonPolicy.offer(true, true, false, true, true));
    }
    @Test public void railNeverChoosesTheLegacyHiddenSlot() {
        for (boolean compact : new boolean[]{false, true})
            assertFalse(LyricsButtonPolicy.beside(true, compact));
        assertFalse(LyricsButtonPolicy.animate(true));
        assertTrue(LyricsButtonPolicy.animate(false));
        assertTrue(LyricsButtonPolicy.beside(false, true));
        assertFalse(LyricsButtonPolicy.offer(false, true, true, true, false));
    }
}
