package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class ArtworkThumbnailPolicyTest {
    @Test public void onlyOneActionOccupiesTheArtworkSlotAcrossAllThreePages() {
        for (int current : new int[]{RailMusicPagePolicy.CLOCK, RailMusicPagePolicy.ARTWORK,
                RailMusicPagePolicy.LYRICS}) {
            int next = RailMusicPagePolicy.next(current, true);
            assertEquals(current != RailMusicPagePolicy.CLOCK,
                    ArtworkThumbnailPolicy.replaceWithPageAction(true, next));
            assertFalse(ArtworkThumbnailPolicy.replaceWithPageAction(false, next));
        }
    }
    @Test public void railThumbnailAndTitleSlotStayFixedThroughTheWholeMorph() {
        assertTrue(ArtworkThumbnailPolicy.fixedControls(true, true, false));
        for (float progress : new float[]{0f, .2f, .5f, .9f, 1f}) {
            assertEquals(0f, ArtworkThumbnailPolicy.hideProgress(true, progress), 0f);
            assertEquals(progress, ArtworkThumbnailPolicy.hideProgress(false, progress), 0f);
        }
    }

    @Test public void ordinaryLayoutsAndPreviewCapturesRetainTheirExistingTreatment() {
        assertFalse(ArtworkThumbnailPolicy.fixedControls(false, true, false));
        assertFalse(ArtworkThumbnailPolicy.fixedControls(true, false, false));
        assertFalse(ArtworkThumbnailPolicy.fixedControls(true, true, true));
    }

    @Test public void veilOnlyMarksTheExpandedArtworkPageOnTheAwakeLockScreen() {
        assertTrue(ArtworkThumbnailPolicy.veil(true, true, true, false, false));
        assertFalse(ArtworkThumbnailPolicy.veil(true, false, true, false, false));
        assertFalse(ArtworkThumbnailPolicy.veil(false, true, true, false, false));
        assertFalse(ArtworkThumbnailPolicy.veil(true, true, false, false, false));
        assertFalse(ArtworkThumbnailPolicy.veil(true, true, true, true, false));
        assertFalse(ArtworkThumbnailPolicy.veil(true, true, true, false, true));
    }
}
