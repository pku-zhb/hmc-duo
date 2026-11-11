package com.os4.musiccover;

import org.junit.Test;
import static org.junit.Assert.*;

public class RailAodArtworkTest {
    private final FoldMusicLayout.Box content = new FoldMusicLayout.Box(0, 126, 953.75f, 1712);
    private final FoldMusicLayout.Box old = new FoldMusicLayout.Box(334, 650, 834, 1150);
    @Test public void desktopEntryUsesTheLeftViewportEvenWithoutAnAwakePose() {
        FoldMusicLayout.Box p = RailAodArtwork.fit(content, old, Float.NaN, 0, 2.75f, 1);
        assertEquals(476.875f, (p.left + p.right) / 2, .001f);
        assertEquals(old.width(), p.width(), .001f);
    }
    @Test public void retractedMediaExpandsArtworkIntoTheNewRoom() {
        FoldMusicLayout.Box before = RailAodArtwork.fit(content, old, 600, 1200, 2.75f, 1);
        FoldMusicLayout.Box after = RailAodArtwork.fit(content, old, 600, 1400, 2.75f, 1);
        assertTrue(after.width() > before.width());
        assertEquals(476.875f, (after.left + after.right) / 2, .001f);
        assertTrue(after.top >= 644);
        assertTrue(after.bottom <= 1356);
    }
    @Test public void largeRoomIsCappedByTheLeftViewportWidth() {
        FoldMusicLayout.Box p = RailAodArtwork.fit(content, old, 300, 1600, 2.75f, 1);
        assertEquals(865.75f, p.width(), .001f);
        assertTrue(p.left >= 44);
        assertTrue(p.right <= 909.75f);
    }
    @Test public void nonsquareArtUsesItsDrawnHeightForTheAvailableRoom() {
        FoldMusicLayout.Box p = RailAodArtwork.fit(content, old, 600, 1200, 2.75f, 2);
        assertEquals(865.75f, p.width(), .001f);
        assertTrue(p.width() / 2 <= 512);
    }
}
