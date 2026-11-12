package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class RailShortcutLayoutTest {
    @Test public void standaloneButtonsMatchTheAcceptedOuterColumn() {
        RailShortcutLayout p = RailShortcutLayout.plan(1168, 1712, 2.75f, 0, 0);
        assertNotNull(p);
        assertEquals(1060.75f, centerX(p.torch), .001f);
        assertEquals(centerX(p.camera), centerX(p.torch), 0);
        assertEquals(132f, p.torch.width(), 0);
        assertEquals(1379.25f, p.torch.top, .001f);
        assertEquals(1529.125f, p.camera.top, .001f);
        assertEquals(1345.5625f, p.capsuleBottom, .001f);
    }

    @Test public void systemInsetsKeepEveryPartOfTheColumnTogether() {
        for (float d : new float[]{1, 1.5f, 2.75f, 3}) {
            for (float bottom : new float[]{0, 24, 48}) {
                RailShortcutLayout p = RailShortcutLayout.plan(425*d, 750*d, d, 10*d, bottom*d);
                RightRailLayout.Plan rail = RightRailLayout.plan(425*d, 750*d, d, 0, 46*d,
                        10*d, bottom*d, 9*d);
                assertNotNull(p); assertNotNull(rail);
                assertEquals(48, p.camera.width()/d, .001f);
                assertEquals(6.5f, (p.camera.top-p.torch.bottom)/d, .001f);
                assertEquals(12.25f+9, (p.torch.top-rail.pill.bottom)/d, .001f);
                assertEquals(centerX(rail.pill), centerX(p.camera), .001f);
                assertEquals(Math.max(18.5f, bottom), (750*d-p.camera.bottom)/d, .001f);
            }
        }
    }

    @Test public void foldAndRotationKeepControlsInsideTheWindow() {
        for (float[] size : new float[][]{{650, 850}, {850, 650}, {425, 700}}) {
            RailShortcutLayout p = RailShortcutLayout.plan(size[0], size[1], 1, 20, 24);
            assertNotNull(p);
            assertTrue(p.torch.top > 0);
            assertTrue(p.camera.right < size[0]-20);
            assertTrue(p.camera.bottom <= size[1]-24);
        }
    }

    @Test public void invalidAndInsufficientWindowsDoNotMoveNativeViews() {
        assertNull(RailShortcutLayout.plan(Float.NaN, 700, 1, 0, 0));
        assertNull(RailShortcutLayout.plan(425, 700, 0, 0, 0));
        assertNull(RailShortcutLayout.plan(425, 700, 1, -1, 0));
        assertNull(RailShortcutLayout.plan(425, 100, 1, 0, 0));
        assertNull(RailShortcutLayout.plan(40, 700, 1, 0, 0));
        assertNull(RailShortcutLayout.plan(425, 700, 1, 0, 700));
    }

    private static float centerX(FoldMusicLayout.Box b) { return (b.left+b.right)/2f; }
}
