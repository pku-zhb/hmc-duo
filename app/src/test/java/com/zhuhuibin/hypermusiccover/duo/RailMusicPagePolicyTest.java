package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class RailMusicPagePolicyTest {
    @Test public void fixedActionCyclesFromClockThroughArtworkAndLyricsBackToClock() {
        int page = RailMusicPagePolicy.current(false, false);
        assertEquals(RailMusicPagePolicy.ARTWORK, page = RailMusicPagePolicy.next(page, true));
        assertEquals(RailMusicPagePolicy.LYRICS, page = RailMusicPagePolicy.next(page, true));
        assertEquals(RailMusicPagePolicy.CLOCK, page = RailMusicPagePolicy.next(page, true));
        assertEquals(RailMusicPagePolicy.ARTWORK, RailMusicPagePolicy.next(page, true));
    }
    @Test public void pendingLyricsStillOfferAReturnToTheClock() {
        int requested = RailMusicPagePolicy.current(true, true);
        assertEquals(RailMusicPagePolicy.LYRICS, requested);
        assertEquals(RailMusicPagePolicy.CLOCK, RailMusicPagePolicy.next(requested, true));
    }
    @Test public void disablingLyricsKeepsArtworkAndClockReachable() {
        assertEquals(RailMusicPagePolicy.ARTWORK, RailMusicPagePolicy.next(RailMusicPagePolicy.CLOCK, false));
        assertEquals(RailMusicPagePolicy.CLOCK, RailMusicPagePolicy.next(RailMusicPagePolicy.ARTWORK, false));
        assertEquals(RailMusicPagePolicy.CLOCK, RailMusicPagePolicy.next(RailMusicPagePolicy.LYRICS, false));
    }
    @Test public void leftOverLyricIntentCannotOverrideTheNativeClockPage() {
        assertEquals(RailMusicPagePolicy.CLOCK, RailMusicPagePolicy.current(false, true));
    }
    @Test public void innerLandscapeCyclesAllFourExplicitScenes() {
        int page = RailMusicPagePolicy.CLOCK;
        for (int expected : new int[]{1, 2, 3, 0, 1, 2, 3, 0}) {
            page = RailMusicPagePolicy.next(page, true, true);
            assertEquals(expected, page);
        }
    }
    @Test public void pendingCombinedPageStillReturnsToClockWithoutWaitingForMeasurementOrLyrics() {
        int page = RailMusicPagePolicy.current(true, true, true);
        assertEquals(RailMusicPagePolicy.ARTWORK_AND_LYRICS, page);
        assertEquals(RailMusicPagePolicy.CLOCK, RailMusicPagePolicy.next(page, true, true));
        assertTrue(RailMusicPagePolicy.showsLyrics(page));
        assertTrue(RailMusicPagePolicy.showsLyrics(RailMusicPagePolicy.LYRICS));
        assertFalse(RailMusicPagePolicy.showsLyrics(RailMusicPagePolicy.ARTWORK));
    }
    @Test public void rotationOrDisabledLyricsNeverLeavesAnUnreachablePageInTheCycle() {
        assertEquals(RailMusicPagePolicy.CLOCK, RailMusicPagePolicy.next(3, true, false));
        assertEquals(RailMusicPagePolicy.CLOCK, RailMusicPagePolicy.next(3, false, true));
        assertEquals(RailMusicPagePolicy.CLOCK, RailMusicPagePolicy.next(1, false, true));
        assertEquals(RailMusicPagePolicy.ARTWORK, RailMusicPagePolicy.current(true, false, true));
        assertEquals(RailMusicPagePolicy.CLOCK, RailMusicPagePolicy.current(false, true, true));
    }

}
