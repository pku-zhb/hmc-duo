package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class RightRailLayoutTest {
    @Test public void outerScreenKeepsTheAcceptedPositionsWithoutShortcutMeasurements() {
        RightRailLayout.Plan p = RightRailLayout.plan(1168, 1712, 2.75f, 0, 126, 0, 0, 0);
        assertNotNull(p);
        assertEquals(997.5f, p.pill.left, .001f);
        assertEquals(126.5f, p.pill.width(), .001f);
        assertEquals(607.75f, p.pill.height(), .001f);
        assertEquals(1345.5625f, p.pill.bottom, .001f);
        assertEquals(961.75f, p.content.right, .001f);
        assertEquals(390f, p.strip.top, .001f);
        assertTrue(p.pill.bottom + 12 * 2.75f < 1379);
    }

    @Test public void colorOsBracketsKeepTheirOwnClearance() {
        RightRailLayout.Plan hyper = RightRailLayout.plan(1168, 1712, 2.75f, 0, 126, 0, 0, 0);
        RightRailLayout.Plan color = RightRailLayout.plan(1168, 1712, 2.75f, 0, 126, 0, 0, 24.75f);
        assertEquals(hyper.pill.bottom, color.pill.bottom + 24.75f, .001f);
        assertEquals(hyper.content.right, color.content.right, 0);
    }

    @Test public void densityAndDisplayChangesRecomputeFromTheWindowOnly() {
        for (float d : new float[]{1f, 1.5f, 2.75f, 3f}) {
            RightRailLayout.Plan p = RightRailLayout.plan(425*d, 700*d, d, 0, 46*d, 0, 24*d, 0);
            assertNotNull(p);
            assertEquals(46, p.pill.width()/d, .001f);
            assertEquals(221, p.pill.height()/d, .001f);
            assertEquals(350, p.content.right/d, .001f);
            assertEquals(16, (425*d-p.pill.right)/d, .001f);
            assertTrue(p.companion.top >= p.strip.top);
        }
        RightRailLayout.Plan portrait = RightRailLayout.plan(650, 850, 1, 0, 24, 0, 24, 0);
        assertEquals(48, portrait.pill.width(), 0);
        assertEquals(413, (portrait.pill.top+portrait.pill.bottom)/2, .001f);
        RightRailLayout.Plan landscape = RightRailLayout.plan(850, 650, 1, 0, 24, 0, 24, 0);
        FoldMusicLayout.Columns music = FoldMusicLayout.columns(landscape.content.right, 650,
                1, 650, 0, 24, 0, 24, 110, new FoldMusicLayout.Box(11.25f, 340, 464.25f, 470), 560);
        assertNotNull(music);
        assertTrue(music.lyrics.right < landscape.pill.left);
    }

    @Test public void unsafeWindowsFailClosed() {
        assertNull(RightRailLayout.plan(Float.NaN, 700, 1, 0, 0, 0, 0, 0));
        assertNull(RightRailLayout.plan(425, 700, 0, 0, 0, 0, 0, 0));
        assertNull(RightRailLayout.plan(300, 700, 1, 0, 0, 0, 0, 0));
        assertNull(RightRailLayout.plan(425, 350, 1, 0, 46, 0, 0, 0));
        assertNull(RightRailLayout.plan(425, 700, 1, -1, 0, 0, 0, 0));
        assertNull(RightRailLayout.plan(425, 700, 1, 0, 0, 0, 700, 0));
    }

    @Test public void faceKeepsUprightContentWithinTheCapsule() {
        for (boolean second : new boolean[]{false, true}) {
            RightRailLayout.VerticalFace face = RightRailLayout.face(48, 221, 1, second);
            assertNotNull(face);
            assertTrue(face.artwork.bottom < face.text.top);
            assertTrue(face.text.bottom < face.action.top);
            assertTrue(face.action.bottom <= 221 - 8);
            if (second) assertTrue(face.secondAction.bottom < face.action.top);
        }
        assertNull(RightRailLayout.face(54, 140, 1, true));
        assertNotNull(RightRailLayout.face(54, 140, 1, false));
    }
}
