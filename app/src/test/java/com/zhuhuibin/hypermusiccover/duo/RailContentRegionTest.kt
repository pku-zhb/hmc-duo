package com.zhuhuibin.hypermusiccover.duo

import org.junit.Assert.*
import org.junit.Test

class RailContentRegionTest {
    @Test fun onlyTheClockBandAboveTheFirstCardOwnsIslandGestures() {
        assertTrue(RailContentRegion.clockSpace(120f, 24f, 800f, 340f))
        for (y in listOf(340f, 400f, 600f, 799f))
            assertFalse(RailContentRegion.clockSpace(y, 24f, 800f, 340f))
        assertFalse(RailContentRegion.clockSpace(20f, 24f, 800f, 340f))
    }
    @Test fun fullyFoldedContentLeavesTheClockSpaceAvailableAndBoundsStaySafe() {
        assertTrue(RailContentRegion.clockSpace(700f, 24f, 800f, null))
        assertFalse(RailContentRegion.clockSpace(800f, 24f, 800f, null))
        assertFalse(RailContentRegion.clockSpace(100f, 24f, 800f, -30f))
        assertFalse(RailContentRegion.clockSpace(Float.NaN, 24f, 800f, null))
    }
}
