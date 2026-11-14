package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class RailContentFrameTest {
    @Test public void outerCardKeepsItsFixedRailSlot() {
        RightRailLayout.Plan p = RightRailLayout.plan(1168, 1712, 2.75f, 0, 126, 0, 0, 0);
        RailContentFrame frame = RailContentFrame.inWindow(p.content, 2.75f, 0, 1168, 1712, false);
        assertEquals(31, frame.left);
        assertEquals(900, frame.width);
        assertEquals(931, frame.right());
    }

    @Test public void parentOriginDoesNotChangeOuterWidthOrAccumulateOffsets() {
        FoldMusicLayout.Box content = new FoldMusicLayout.Box(0, 0, 961.75f, 1712);
        for (int parentLeft : new int[]{0, 44, 88, -44, 198264, 0}) {
            RailContentFrame frame = RailContentFrame.in(content, 2.75f, parentLeft);
            assertEquals(31, frame.left + parentLeft);
            assertEquals(900, frame.width);
        }
    }

    @Test public void innerBaseFrameDoesNotRewriteTheSystemWidth() {
        for (int[] window : new int[][]{{1672,2364},{2364,1672},{750,650}}) {
            float density = window[0] == 750 ? 1f : 2.75f;
            RightRailLayout.Plan p = RightRailLayout.plan(window[0], window[1], density, 0, 24, 0, 24, 0);
            for (boolean combined : new boolean[]{false, true}) {
                for (int parent : new int[]{0,559,-44})
                    assertNull(RailContentFrame.inWindow(p.content, density, parent, window[0], window[1], combined));
            }
        }
    }

    @Test public void everyInnerPageKeepsTheRightSidebarOutsideItsCenterAxis() {
        for (int[] window : new int[][]{{1672,2364},{2364,1672}}) {
            RightRailLayout.Plan p = RightRailLayout.plan(window[0], window[1], 2.75f, 0, 126, 0, 0, 0);
            FoldMusicLayout.Box single = RailContentFrame.presentation(p.content, window[0], window[1], 2.75f, 0, false);
            assertEquals((window[0] - 75f * 2.75f) / 2f, (single.left + single.right) / 2f, .01f);
            assertSame(p.content, RailContentFrame.presentation(p.content, window[0], window[1], 2.75f, 0, true));
        }
        RightRailLayout.Plan outer = RightRailLayout.plan(1168,1712,2.75f,0,126,0,0,0);
        assertSame(outer.content, RailContentFrame.presentation(outer.content,1168,1712,2.75f,0,false));
    }

    @Test public void innerSinglePageCenterHonorsAsymmetricSafeInsets() {
        FoldMusicLayout.Box rail = new FoldMusicLayout.Box(30,24,680,1000);
        FoldMusicLayout.Box content = RailContentFrame.presentation(rail,800,1000,1,10,false);
        assertEquals(355f,(content.left + content.right)/2f,0f);
    }

    @Test public void combinedColumnTranslationIsStableAtTheNativeAndZoomedScale() {
        for (float scale : new float[]{1f,.95f,1.05f}) {
            float first = RailContentFrame.columnTranslation(0,590,44,scale);
            float nextLeft = 590 + first * scale;
            assertEquals(44, nextLeft, .001f);
            for (int i = 0; i < 100; i++) {
                float next = RailContentFrame.columnTranslation(first,nextLeft,44,scale);
                assertEquals(first,next,.001f);
                nextLeft += (next - first) * scale;
                first = next;
            }
        }
        assertTrue(Float.isNaN(RailContentFrame.columnTranslation(0,590,44,0)));
    }

    @Test public void combinedColumnOwnsOnlyItsRequestedCardFrame() {
        FoldMusicLayout.Box player = new FoldMusicLayout.Box(212.5f, 400, 612.5f, 700);
        for (int parent : new int[]{0, 31, 559}) {
            RailContentFrame frame = RailContentFrame.aroundCard(player, parent, 0, 0);
            assertEquals(213, frame.left + parent);
            assertEquals(400, frame.width);
        }
        assertNull(RailContentFrame.aroundCard(null, 0, 0, 0));
        assertNull(RailContentFrame.aroundCard(player, 0, -1, 0));
    }

    @Test public void invalidWindowsNeverForceNativeMeasurement() {
        assertNull(RailContentFrame.in(null,1,0));
        assertNull(RailContentFrame.in(new FoldMusicLayout.Box(0,0,20,800),1,0));
        assertFalse(RailContentFrame.ownsNativeBounds(0,800,1));
        assertFalse(RailContentFrame.ownsNativeBounds(500,800,Float.NaN));
    }
}
