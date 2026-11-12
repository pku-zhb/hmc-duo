package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class RailStatusTest {
    @Test public void referenceClusterAlignsWithCapsulesAndClearsTheFirstBubble() {
        RightRailLayout.Plan rail = RightRailLayout.plan(1168, 1712, 2.75f, 0, 126, 0, 0, 0);
        FoldMusicLayout.Box status = RailStatusLayout.place((rail.pill.left+rail.pill.right)/2, 126, 2.75f);
        assertEquals(963f, status.left, .001f);
        assertEquals(158f, status.top, .001f);
        assertEquals(205f, status.width(), .001f);
        assertTrue(status.right <= 1168);
        assertTrue(rail.strip.top-status.bottom >= 10*2.75f);
    }

    @Test public void statusAndCapsuleReservationsScaleAcrossDisplays() {
        for (float d : new float[]{1, 1.5f, 2.75f, 3}) {
            for (float[] window : new float[][]{{425, 700}, {650, 850}, {850, 650}, {850, 500}}) {
                RightRailLayout.Plan rail = RightRailLayout.plan(window[0]*d, window[1]*d, d,
                        0, 46*d, 10*d, 24*d, 0);
                assertNotNull(rail);
                FoldMusicLayout.Box status = RailStatusLayout.place((rail.pill.left+rail.pill.right)/2, 46*d, d);
                // The reference snaps the surface origin to the nearest physical pixel.
                assertTrue(status.right <= window[0]*d-10*d+.5f);
                assertTrue(status.bottom+10*d <= rail.strip.top);
                assertTrue(rail.companion.top >= rail.strip.top);
            }
        }
    }

    @Test public void invalidGeometryIsNotDrawn() {
        assertNull(RailStatusLayout.place(Float.NaN, 126, 2.75f));
        assertNull(RailStatusLayout.place(1000, -1, 2.75f));
        assertNull(RailStatusLayout.place(1000, 126, 0));
    }

    @Test public void statusRingSharesThePinnedCapsuleCenterAcrossAodAndWakeScales() {
        FoldMusicLayout.Box box = RailStatusLayout.place(1061, 126, 2.75f);
        float inkCenter = box.left + RailStatusLayout.CENTER_X;
        assertEquals(1061, inkCenter, 0);
        assertNotEquals(inkCenter, (box.left + box.right) / 2, .001f);
        for (float scale : new float[]{.85f, .95f, .97f, 1f}) {
            float nativeX = 584 + (inkCenter - 584) * scale;
            RailStatusLayout.Offset correction = RailStatusLayout.pinX(inkCenter, nativeX,
                    scale, 0, 0, scale);
            assertNotNull(correction);
            assertEquals(1061, nativeX + correction.x * scale, .001f);
            assertEquals(0, correction.y, 0);
            if (scale == 1) assertEquals(0, correction.x, 0);
        }
    }

    @Test public void translatedAndScrolledParentsDoNotIntroduceAnotherOffset() {
        float canonicalX = 1061;
        // Rest position includes the ancestor's layout offset and scroll exactly once.
        float parentLeft = 40, parentScroll = 12, nativeTranslation = 7;
        float localX = canonicalX - (parentLeft - parentScroll);
        for (float scale : new float[]{.85f, 1f}) {
            float nativeX = parentLeft - parentScroll + nativeTranslation + scale * localX;
            RailStatusLayout.Offset correction = RailStatusLayout.pinX(canonicalX, nativeX,
                    scale, 0, 0, scale);
            assertEquals(canonicalX, nativeX + scale * correction.x, .001f);
        }
    }

    @Test public void affineCompensationKeepsNativeVerticalPlacement() {
        float nativeX = 1037, nativeY = 175;
        float a = .95f, b = .08f, c = -.03f, d = .97f;
        RailStatusLayout.Offset correction = RailStatusLayout.pinX(1061, nativeX, a, b, c, d);
        assertNotNull(correction);
        assertEquals(1061, nativeX + a * correction.x + b * correction.y, .001f);
        assertEquals(nativeY, nativeY + c * correction.x + d * correction.y, .001f);
    }

    @Test public void repeatedFramesUseCanonicalInkAndNeverAccumulateThePriorCorrection() {
        float canonicalX = RailStatusLayout.place(1061.4f, 126, 2.75f).left + RailStatusLayout.CENTER_X;
        float nativeX = 584 + (canonicalX - 584) * .95f;
        for (int frame = 0; frame < 100; frame++) {
            RailStatusLayout.Offset correction = RailStatusLayout.pinX(canonicalX, nativeX, .95f, 0, 0, .95f);
            assertEquals(canonicalX, nativeX + .95f * correction.x, .001f);
            // Retain the pre-existing subpixel rounding rather than changing the rest layout.
            assertEquals(1061, canonicalX, 0);
        }
    }

    @Test public void singularOrInvalidParentTransformsCannotProduceInvalidTranslations() {
        assertNull(RailStatusLayout.pinX(1061, Float.NaN, 1, 0, 0, 1));
        assertNull(RailStatusLayout.pinX(1061, 1037, 0, 0, 0, 0));
        assertNull(RailStatusLayout.pinX(1061, 1037, 1, 1, 1, 1));
    }

    @Test public void batteryScalesAndUnknownValuesDoNotBecomeFull() {
        assertEquals(53, RailStatusValues.batteryPercent(53, 100));
        assertEquals(53, RailStatusValues.batteryPercent(106, 200));
        assertEquals(0, RailStatusValues.batteryPercent(0, 100));
        assertEquals(100, RailStatusValues.batteryPercent(120, 100));
        assertEquals(-1, RailStatusValues.batteryPercent(-1, 100));
        assertEquals(-1, RailStatusValues.batteryPercent(80, 0));
    }

    @Test public void lossOfServiceCannotLeaveOldFullBars() {
        assertEquals(0, RailStatusValues.signalLevel(4, false));
        assertEquals(-1, RailStatusValues.signalLevel(-1, true));
        assertEquals(2, RailStatusValues.signalLevel(2, true));
        assertEquals(4, RailStatusValues.signalLevel(8, true));
    }

    @Test public void missingWifiDoesNotDrawAConnectedWedge() {
        assertEquals(-1, RailStatusValues.wifiLevel(false, -40));
        assertEquals(-1, RailStatusValues.wifiLevel(true, -127));
        assertEquals(-1, RailStatusValues.wifiLevel(true, 10));
        assertEquals(0, RailStatusValues.wifiLevel(true, -90));
        assertEquals(1, RailStatusValues.wifiLevel(true, -70));
        assertEquals(2, RailStatusValues.wifiLevel(true, -50));
    }
}
