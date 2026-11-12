package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class RightRailLayoutTest {
    private FoldMusicLayout.Box button(float x, float y) { return new FoldMusicLayout.Box(x, y, x + 54, y + 54); }
    private RightRailLayout.Plan plan(float w, float h, FoldMusicLayout.Box a, FoldMusicLayout.Box b) {
        return RightRailLayout.plan(w, h, 1, 0, 24, 0, 24, a, b);
    }
    @Test public void originalHorizontalShortcutsDoNotActivateTheAdapter() {
        assertNull(plan(400, 850, button(20, 730), button(326, 730)));
        assertNull(plan(400, 850, button(20, 620), button(20, 704)));
    }
    @Test public void rightColumnHasAVerticalCapsuleAboveBothNativeButtons() {
        RightRailLayout.Plan p = plan(400, 850, button(328, 620), button(328, 704));
        assertNotNull(p);
        assertEquals(46, p.pill.width(), 0);
        assertEquals(221, p.pill.height(), 0);
        assertEquals(599, p.pill.bottom, 0);
        assertEquals(24 + 72, p.strip.top, 0);
        assertTrue(p.pill.bottom < 620 - 12);
        assertTrue(p.companion.bottom < p.pill.top);
        assertTrue(p.content.right < p.pill.left);
        assertEquals(316, p.content.right, 0);
        // The left content's center moves left while retaining its own geometry.
        assertEquals(158, (p.content.left + p.content.right) / 2, 0);
    }
    @Test public void shortcutOrderDoesNotChangeTheRail() {
        RightRailLayout.Plan a = plan(400, 850, button(328, 620), button(328, 704));
        RightRailLayout.Plan b = plan(400, 850, button(328, 704), button(328, 620));
        assertEquals(a.pill.top, b.pill.top, 0);
        assertEquals(a.content.right, b.content.right, 0);
    }
    @Test public void outerRailLowersTowardTheNativeButtonsWithoutMutatingInputBoxes() {
        float d = 2.75f;
        FoldMusicLayout.Box torch = new FoldMusicLayout.Box(1000, 1380, 1118, 1498);
        FoldMusicLayout.Box camera = new FoldMusicLayout.Box(1000, 1530, 1118, 1648);
        RightRailLayout.Plan p = RightRailLayout.plan(1168, 1712, d, 0, 0, 0, 0, torch, camera);
        assertNotNull(p);
        assertEquals(torch.top - (12 + 9) * d, p.pill.bottom, .001f);
        assertEquals(p.pill.bottom, p.strip.bottom, .001f);
        assertTrue(p.pill.bottom + 12*d < torch.top);
        assertTrue(p.companion.top >= 12*d);
        assertEquals(1380, torch.top, 0);
        assertEquals((1439 + 1589) / 2f, p.controlsY, .001f);
        assertEquals(0, p.firstOffsetY, 0);
        assertEquals(0, p.secondOffsetY, 0);
    }
    @Test public void cutoutsAndInsufficientRoomFailClosed() {
        assertNull(plan(400, 850, button(328, 190), button(328, 274)));
        assertNull(plan(400, 850, button(380, 620), button(380, 704)));
        assertNull(plan(400, 850, button(328, 780), button(328, 864)));
        assertNull(RightRailLayout.plan(400, 850, 0, 0, 24, 0, 24, button(328, 620), button(328, 704)));
        assertNull(plan(400, 850, null, button(328, 704)));
        assertNull(plan(Float.NaN, 850, button(328, 620), button(328, 704)));
    }
    @Test public void crampedNativeButtonsAreNeverMovedByTheCapsule() {
        RightRailLayout.Plan p = RightRailLayout.plan(1168, 1712, 2.75f, 0, 126, 0, 66,
                new FoldMusicLayout.Box(995, 1388, 1127, 1520),
                new FoldMusicLayout.Box(995, 1538, 1127, 1670 - 24));
        assertNotNull(p);
        assertEquals(0, p.firstOffsetY, 0);
        assertEquals(0, p.secondOffsetY, 0);
        assertTrue(p.pill.bottom + 9 * 2.75f + 12 * 2.75f <= 1388);
    }
    @Test public void faceHasUprightArtworkTextAndActionsWithoutOverlapping() {
        for (boolean second : new boolean[]{false, true}) {
            RightRailLayout.VerticalFace f = RightRailLayout.face(48, 221, 1, second);
            assertNotNull(f);
            assertTrue(f.artwork.bottom < f.text.top);
            assertTrue(f.text.bottom < f.action.top);
            assertTrue(f.action.bottom <= 221 - 8);
            assertTrue(f.text.width() >= 28);
            if (second) {
                assertTrue(f.text.bottom < f.secondAction.top);
                assertTrue(f.secondAction.bottom < f.action.top);
            }
        }
        assertNull(RightRailLayout.face(54, 140, 1, true));
        assertNotNull(RightRailLayout.face(54, 140, 1, false));
    }
    @Test public void innerLandscapeCanCombineTheReservedRailAndMusicColumns() {
        RightRailLayout.Plan rail = plan(850, 650, button(775, 460), button(775, 544));
        assertNotNull(rail);
        FoldMusicLayout.Columns music = FoldMusicLayout.columns(rail.content.right, 650,
                1, 650, 0, 24, 0, 24, 110, 130, 560);
        assertNotNull(music);
        assertTrue(music.lyrics.right < rail.pill.left);
        assertTrue(music.player.right < music.lyrics.left);
    }
    @Test public void railIsScaleIndependentAndAlwaysInsideTheSafeArea() {
        for (float d : new float[]{1f, 1.5f, 2.5f, 3f}) {
            RightRailLayout.Plan p = RightRailLayout.plan(400*d, 850*d, d, 0, 24*d, 0, 24*d,
                    new FoldMusicLayout.Box(328*d, 620*d, 382*d, 674*d),
                    new FoldMusicLayout.Box(328*d, 704*d, 382*d, 758*d));
            assertNotNull(p);
            assertEquals(221, p.pill.height()/d, .001);
            assertEquals(316, p.content.right/d, .001);
            assertTrue(p.companion.top >= 24*d);
        }
    }
    @Test public void outerAndInnerLandscapeKeepTheCapsuleOutsideNativeShortcutHitTargets() {
        float d = 2.75f, disc = Math.round(48f * d * .85f), bow = 9f * d;
        float[][] scenes = {{1168, 1712, 995, 1388, 1538}, {2364, 1672, 2180, 1348, 1498}};
        for (float[] scene : scenes) {
            FoldMusicLayout.Box torch = new FoldMusicLayout.Box(scene[2], scene[3], scene[2]+132, scene[3]+132);
            FoldMusicLayout.Box camera = new FoldMusicLayout.Box(scene[2], scene[4], scene[2]+132, scene[4]+132);
            RightRailLayout.Plan p = RightRailLayout.plan(scene[0], scene[1], d, 0, 0, 0, 0, torch, camera, disc, bow);
            assertNotNull(p);
            float torchCenter = (torch.top + torch.bottom) / 2 + p.firstOffsetY;
            float cameraCenter = (camera.top + camera.bottom) / 2 + p.secondOffsetY;
            float pillGap = torchCenter - disc/2 - (p.pill.bottom + bow);
            float buttonGap = cameraCenter - torchCenter - disc;
            assertTrue(p.pill.bottom + bow + 12*d <= torch.top);
            assertEquals(0f, p.firstOffsetY, 0f);
            assertEquals(0f, p.secondOffsetY, 0f);
            assertTrue(pillGap >= 0);
            assertEquals(46*d, p.pill.width(), .001f);
            assertEquals((camera.top + camera.bottom) / 2, cameraCenter, 0);
        }
    }
    @Test public void innerPortraitPreservesItsExistingCapsuleWidthAndCenter() {
        RightRailLayout.Plan p = plan(650, 850, button(575, 620), button(575, 704));
        assertNotNull(p);
        assertEquals(48, p.pill.width(), .001f);
        assertEquals(413, (p.pill.top+p.pill.bottom)/2, .001f);
    }

}
