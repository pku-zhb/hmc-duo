package com.zhuhuibin.hypermusiccover.duo

import org.junit.Assert.*
import org.junit.Test

class RailContentRegionTest {
    @Test fun emptySpaceAboveBetweenAndBelowCardsCanExpandContent() {
        for (y in listOf(120f, 340f, 600f, 799f))
            assertTrue(RailContentRegion.emptySpace(y, 24f, 800f, false))
        assertFalse(RailContentRegion.emptySpace(20f, 24f, 800f, false))
    }
    @Test fun fullyFoldedContentOwnsTheEntireLeftSideWithinItsBounds() {
        assertTrue(RailContentRegion.emptySpace(700f, 24f, 800f, false))
        assertFalse(RailContentRegion.emptySpace(800f, 24f, 800f, false))
        assertFalse(RailContentRegion.emptySpace(Float.NaN, 24f, 800f, false))
    }
    @Test fun everyActualCardWinsEvenNearTheBottom() {
        for (y in listOf(120f, 340f, 600f, 799f))
            assertFalse(RailContentRegion.emptySpace(y, 24f, 800f, true))
    }
}
