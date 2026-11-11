package com.os4.musiccover;

import org.junit.Test;
import static org.junit.Assert.*;

public class LyricsButtonPolicyTest {
    @Test public void defaultArtworkAndNativePlayerKeepAnAvailableLyricsButton() {
        assertTrue(LyricsButtonPolicy.offer(true, true, true, true, false));
        assertTrue(LyricsButtonPolicy.offer(true, false, true, true, false));
        assertFalse(LyricsButtonPolicy.offer(true, true, true, false, true));
        assertFalse(LyricsButtonPolicy.offer(true, true, false, true, true));
    }
    @Test public void railNeverChoosesTheLegacyHiddenSlot() {
        for (boolean canvas : new boolean[]{false, true}) for (boolean compact : new boolean[]{false, true})
            assertFalse(LyricsButtonPolicy.beside(true, canvas, compact));
        assertFalse(LyricsButtonPolicy.animate(true));
        assertTrue(LyricsButtonPolicy.animate(false));
        assertTrue(LyricsButtonPolicy.beside(false, true, false));
        assertFalse(LyricsButtonPolicy.offer(false, true, true, true, false));
    }
}
