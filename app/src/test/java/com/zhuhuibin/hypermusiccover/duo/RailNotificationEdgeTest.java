package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class RailNotificationEdgeTest {
    @Test public void edgeHasAnExactSharpJoinAndAnEmptyRightBoundary() {
        RailNotificationEdge edge = RailNotificationEdge.resolve(900, 1800, 3f);
        assertNotNull(edge);
        assertEquals(72, edge.width);
        assertEquals(24, edge.radius);
        assertEquals(1f, edge.opacity(edge.start()), 0f);
        assertEquals(0f, edge.blur(edge.start()), 0f);
        assertEquals(.5f, edge.opacity(edge.start() + 36), .0001f);
        assertEquals(0f, edge.opacity(edge.right), 0f);
        assertEquals(24f, edge.blur(edge.right), 0f);
        assertEquals(edge.radius, edge.start() - edge.sourceLeft);
        assertEquals(edge.width + edge.radius, edge.sourceWidth());
    }

    @Test public void normalLeftwardAndReturnedRowsHaveNoEffect() {
        RailNotificationEdge edge = RailNotificationEdge.resolve(900, 1800, 3f);
        for (float dx : new float[]{0f, -1f, -300f, Float.NaN, Float.POSITIVE_INFINITY}) {
            assertEquals(0f, edge.strength(dx), 0f);
            assertNull(edge.region(100, 0, 240, 1800, dx));
        }
        assertEquals(.5f, edge.strength(36), .0001f);
        assertEquals(1f, edge.strength(72), 0f);
        assertEquals(1f, edge.strength(500), 0f);
        // Returning through the same native setter unwinds the treatment continuously.
        assertTrue(edge.strength(10) < edge.strength(20));
        for (int dx = 1; dx < 72; dx++) {
            RailNotificationEdge.Region region = edge.region(100, 0, 240, 1800, dx);
            if (region != null) assertEquals(edge.strength(dx), region.strength,
                    .5f / RailNotificationEdge.STRENGTH_STEPS + .00001f);
        }
    }

    @Test public void scrollingUsesActualClipHeightAndDoesNotReachTheNextRow() {
        RailNotificationEdge edge = RailNotificationEdge.resolve(900, 1800, 3f);
        RailNotificationEdge.Region before = edge.region(600, 0, 240, 1800, 72);
        RailNotificationEdge.Region scrolled = edge.region(480, 0, 240, 1800, 72);
        assertEquals(600, before.top);
        assertEquals(840, before.bottom);
        assertEquals(480, scrolled.top);
        assertEquals(720, scrolled.bottom);
        assertEquals(1f, scrolled.strength, 0f);
        RailNotificationEdge.Region top = edge.region(-100, 0, 240, 1800, 72);
        assertEquals(0, top.top);
        assertEquals(140, top.bottom);
        assertNull(edge.region(1800, 0, 240, 1800, 72));
        assertNull(edge.region(100, 0, 0, 1800, 72));
    }

    @Test public void foldAndNarrowSlotsResolveLocallyWithoutAWindowSizedBlur() {
        RailNotificationEdge outer = RailNotificationEdge.resolve(900, 1800, 3f);
        RailNotificationEdge inner = RailNotificationEdge.resolve(1246, 1200, 2.75f);
        assertEquals(66, inner.width);
        assertTrue(inner.sourceWidth() < inner.right / 10);
        assertNotEquals(outer.start(), inner.start());
        RailNotificationEdge tiny = RailNotificationEdge.resolve(40, 80, 3f);
        assertEquals(10, tiny.width);
        assertEquals(5, tiny.radius);
        assertNull(RailNotificationEdge.resolve(0, 80, 3f));
        assertNull(RailNotificationEdge.resolve(40, 0, 3f));
        assertNull(RailNotificationEdge.resolve(40, 80, Float.NaN));
    }
}
