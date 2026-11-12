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
        RailContentFrame frame = RailContentFrame.inWindow(inner.content, 2.75f, 0, 2364, 1672, false);
        assertEquals(1246, frame.width);
        assertEquals(456, frame.left);
        RailContentFrame inset = RailContentFrame.in(new FoldMusicLayout.Box(30, 0, 500, 850), 1, 0);
        assertEquals(41, inset.left);
        assertEquals(489, inset.right());
    }

    @Test public void unmeasuredAndInvalidAreasNeverProduceAForcedFrame() {
        assertNull(RailContentFrame.in(null, 1, 0));
        assertNull(RailContentFrame.in(new FoldMusicLayout.Box(0, 0, 20, 800), 1, 0));
        assertNull(RailContentFrame.in(new FoldMusicLayout.Box(0, 0, 900, 800), Float.NaN, 0));
    }
    @Test public void combinedSceneMovesTheWholeNativeStackToTheLeftWithoutStretchingIt() {
        RightRailLayout.Plan inner = RightRailLayout.plan(2364, 1672, 2.75f, 0, 164, 0, 0, 0);
        for (int parent : new int[]{0, 559, -44, 10000}) {
            RailContentFrame frame = RailContentFrame.inWindow(inner.content, 2.75f, parent, 2364, 1672, true);
            assertEquals(1246, frame.width);
            assertEquals(31, frame.left + parent);
            assertTrue(inner.content.right - (frame.right() + parent) >= (16 + 260) * 2.75f);
        }
    }

    @Test public void smallerInnerWindowsReserveAUsefulLyricColumnWithAFixedBound() {
        RightRailLayout.Plan inner = RightRailLayout.plan(750, 650, 1, 0, 24, 0, 24, 0);
        RailContentFrame frame = RailContentFrame.inWindow(inner.content, 1, 0, 750, 650, true);
        assertTrue(frame.width >= 240);
        assertTrue(frame.width <= 453);
        assertTrue(inner.content.right - 16 - frame.right() - 16 >= 260);
        assertEquals(frame.width, RailContentFrame.inWindow(inner.content, 1, 0, 750, 650, true).width);
    }

    @Test public void outerAndInnerPortraitDoNotAcquireTheLandscapeWidthLimit() {
        for (int[] size : new int[][]{{1168,1712}, {1672,2364}}) {
            RightRailLayout.Plan plan = RightRailLayout.plan(size[0], size[1], 2.75f, 0, 126, 0, 0, 0);
            RailContentFrame old = RailContentFrame.in(plan.content, 2.75f, 0);
            for (boolean combined : new boolean[]{false, true}) {
                RailContentFrame frame = RailContentFrame.inWindow(plan.content, 2.75f, 0, size[0], size[1], combined);
                assertEquals(old.width, frame.width);
                assertEquals(old.left, frame.left);
            }
        }
    }

}
