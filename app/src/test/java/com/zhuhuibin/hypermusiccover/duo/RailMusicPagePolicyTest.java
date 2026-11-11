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
}
