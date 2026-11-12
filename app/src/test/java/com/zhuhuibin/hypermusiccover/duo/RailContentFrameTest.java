package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class RailContentFrameTest {
    @Test public void outerCardHasAnAbsoluteSlotRatherThanAProportionOfTheNativeWidth() {
        RightRailLayout.Plan p = RightRailLayout.plan(1168, 1712, 2.75f, 0, 126, 0, 0, 0);
        RailContentFrame frame = RailContentFrame.in(p.content, 2.75f, 0);
        assertEquals(31, frame.left);
        assertEquals(900, frame.width);
        assertEquals(931, frame.right());
        assertFalse(frame.matches(44, 875));
        assertTrue(frame.matches(31, 900));
    }

    @Test public void changingParentOriginDoesNotChangeWidthOrAccumulateOffsets() {
        FoldMusicLayout.Box content = new FoldMusicLayout.Box(0, 0, 961.75f, 1712);
        for (int parentLeft : new int[]{0, 44, 88, -44, 198264, 0}) {
            RailContentFrame frame = RailContentFrame.in(content, 2.75f, parentLeft);
            assertEquals(31, frame.left + parentLeft);
            assertEquals(900, frame.width);
        }
    }

    @Test public void newWindowUsesItsOwnInsetsAndDoesNotReuseAnOuterWidth() {
        RightRailLayout.Plan inner = RightRailLayout.plan(2364, 1672, 2.75f, 0, 164, 0, 0, 0);
        RailContentFrame frame = RailContentFrame.in(inner.content, 2.75f, 0);
        assertEquals(2096, frame.width);
        assertEquals(31, frame.left);
        RailContentFrame inset = RailContentFrame.in(new FoldMusicLayout.Box(30, 0, 500, 850), 1, 0);
        assertEquals(41, inset.left);
        assertEquals(489, inset.right());
    }

    @Test public void unmeasuredAndInvalidAreasNeverProduceAForcedFrame() {
        assertNull(RailContentFrame.in(null, 1, 0));
        assertNull(RailContentFrame.in(new FoldMusicLayout.Box(0, 0, 20, 800), 1, 0));
        assertNull(RailContentFrame.in(new FoldMusicLayout.Box(0, 0, 900, 800), Float.NaN, 0));
    }
}
