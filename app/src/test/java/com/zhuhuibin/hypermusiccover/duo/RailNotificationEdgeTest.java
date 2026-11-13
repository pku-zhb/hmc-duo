package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class RailNotificationEdgeTest {
    @Test public void evenTheSmallestSwipeHasATransparentTerminalEdge() {
        RailNotificationEdge edge = RailNotificationEdge.resolve(900, 1800, 3f);
        for (float dx : new float[]{.01f, .5f, 1f, 12f, 36f, 72f, 300f}) {
            for (float direction : new float[]{-1f, 1f}) {
                RailNotificationEdge.Region r = edge.region(0, 100, 900, 340, 0, 0, 900, 1800, dx * direction);
                assertNotNull(r);
                assertEquals(0f, RailNotificationEdge.opacity(r.leftSide ? 0f : r.width(), r.width(), r.leftSide), 0f);
                assertEquals(1f, RailNotificationEdge.opacity(r.leftSide ? r.width() : 0f, r.width(), r.leftSide), 0f);
            }
        }
        assertEquals(1, edge.featherWidth(.01f));
        assertEquals(12, edge.featherWidth(12f));
        assertEquals(72, edge.featherWidth(300f));
    }

    @Test public void leftAndRightUseTheSameCurveAndWidthDuringTheReturn() {
        RailNotificationEdge edge = RailNotificationEdge.resolve(900, 1800, 3f);
        for (int dx = 1; dx <= 90; dx++) {
            assertEquals(edge.featherWidth(dx), edge.featherWidth(-dx));
            int width = edge.featherWidth(dx);
            for (int x = 0; x <= width; x++)
                assertEquals(RailNotificationEdge.opacity(x, width, true),
                        RailNotificationEdge.opacity(width - x, width, false), .00001f);
        }
        assertTrue(edge.featherWidth(5f) < edge.featherWidth(30f));
        for (float dx : new float[]{0f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY})
            assertNull(edge.region(0, 100, 900, 340, 0, 0, 900, 1800, dx));
    }

    @Test public void horizontalAndVerticalClipsDefineTheActualCutInsteadOfTheNominalStackWidth() {
        RailNotificationEdge edge = RailNotificationEdge.resolve(900, 1800, 3f);
        RailNotificationEdge.Region right = edge.region(35, 120, 870, 360, 50, 150, 840, 350, 24f);
        assertEquals(816, right.left);
        assertEquals(840, right.right);
        assertEquals(150, right.top);
        assertEquals(350, right.bottom);
        RailNotificationEdge.Region left = edge.region(35, 120, 870, 360, 50, 150, 840, 350, -24f);
        assertEquals(50, left.left);
        assertEquals(74, left.right);
        assertEquals(right.width(), left.width());
    }

    @Test public void contentCannotReappearOutsideTheFeatherWhenNativeExpansionChangesItsClipping() {
        RailNotificationEdge edge = RailNotificationEdge.resolve(900, 1800, 3f);
        RailNotificationEdge.Region looseLeft = edge.region(-100, 100, 1000, 340, -100, 0, 1000, 1800, -24f);
        RailNotificationEdge.Region tightLeft = edge.region(-100, 100, 1000, 340, 0, 0, 900, 1800, -24f);
        assertEquals(0, looseLeft.left);
        assertEquals(tightLeft.left, looseLeft.left);
        assertEquals(tightLeft.right, looseLeft.right);
        assertEquals(-100, looseLeft.clearLeft);
        RailNotificationEdge.Region looseRight = edge.region(-100, 100, 1000, 340, -100, 0, 1000, 1800, 24f);
        assertEquals(900, looseRight.right);
        assertEquals(1000, looseRight.clearRight);
    }

    @Test public void scrollingUsesTheMappedActualRowClipAndLeavesOtherVerticalRegionsAlone() {
        RailNotificationEdge edge = RailNotificationEdge.resolve(900, 1800, 3f);
        RailNotificationEdge.Region before = edge.region(0, 600, 900, 840, 0, 0, 900, 1800, 30);
        RailNotificationEdge.Region after = edge.region(0, 480, 900, 720, 0, 0, 900, 1800, 30);
        assertEquals(600, before.top); assertEquals(840, before.bottom);
        assertEquals(480, after.top); assertEquals(720, after.bottom);
        assertNull(edge.region(0, 1800, 900, 2040, 0, 0, 900, 1800, 30));
        assertNull(edge.region(0, 100, 900, 100, 0, 0, 900, 1800, 30));
        assertNull(edge.region(Float.NaN, 100, 900, 340, 0, 0, 900, 1800, 30));
    }

    @Test public void foldAndExtremeDensitiesRemainLocalAndBoundTheEffectCache() {
        RailNotificationEdge inner = RailNotificationEdge.resolve(1246, 1200, 2.75f);
        assertEquals(66, inner.maxWidth);
        assertEquals(10, RailNotificationEdge.resolve(40, 80, 3f).maxWidth);
        assertEquals(RailNotificationEdge.MAX_FEATHER_PX,
                RailNotificationEdge.resolve(5000, 5000, Float.MAX_VALUE).maxWidth);
        assertNull(RailNotificationEdge.resolve(0, 80, 3f));
        assertNull(RailNotificationEdge.resolve(40, 0, 3f));
        assertNull(RailNotificationEdge.resolve(40, 80, Float.NaN));
    }
}
