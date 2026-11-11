package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class FoldArtworkLayoutTest {
    @Test public void portraitRuleDoesNotAffectTheOuterOrInnerLandscape() {
        assertTrue(FoldArtworkLayout.innerPortrait(1672,2364,2.75f));
        assertFalse(FoldArtworkLayout.innerPortrait(1168,1712,2.75f));
        assertFalse(FoldArtworkLayout.innerPortrait(2364,1672,2.75f));
        assertTrue(FoldArtworkLayout.innerLandscape(2364,1672,2.75f));
        assertFalse(FoldArtworkLayout.innerLandscape(2364,1672,0));
    }
    @Test public void artworkNeverExceedsPlayerWidthAndRetainsItsVisualCenter() {
        CoverCardStyle.Rect r = FoldArtworkLayout.cap(new CoverCardStyle.Rect(20,100,1400),1100);
        assertEquals(1100,r.side,0);
        assertEquals(720,r.x+r.side/2,0);
        assertEquals(800,r.y+r.side/2,0);
        assertEquals(500,FoldArtworkLayout.cap(new CoverCardStyle.Rect(0,0,500),1100).side,0);
        assertNull(FoldArtworkLayout.cap(r,Float.NaN));
    }
    @Test public void clockCanGrowButFitsWidthAndSpaceReservedForTheCappedCover() {
        assertEquals(250,FoldArtworkLayout.clockUnit(400,400,100,400,100,1000,300,36),.001);
        assertEquals(120,FoldArtworkLayout.clockUnit(400,400,100,400,100,1000,120,36),.001);
        assertEquals(80,FoldArtworkLayout.clockUnit(80,400,100,400,100,1000,300,36),.001);
        assertEquals(36,FoldArtworkLayout.clockUnit(400,400,100,400,100,1000,10,36),.001);
    }
}
