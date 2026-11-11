package com.zhuhuibin.hypermusiccover.duo

import org.junit.Assert.*
import org.junit.Test

class PillAxisGeometryTest {
    @Test fun verticalWakeAndAppearancePreserveThe149By608Aspect() {
        for (scale in listOf(.6f, .8f, 1f, 1.02f)) {
            val w = PillAxisGeometry.rowWidth(149f, 608f, true) * scale
            val h = 608f * scale
            assertEquals(149f / 608f, w / h, .0001f)
            assertTrue(w < h)
        }
        assertEquals(608f, PillAxisGeometry.rowWidth(149f, 608f, false), 0f)
    }

    @Test fun upAndDownSwitchInOppositeDirectionsAndHorizontalPullsDoNothing() {
        assertEquals(1, swipe(10f, -160f))
        assertEquals(-1, swipe(-10f, 160f))
        assertEquals(0, swipe(-300f, 20f))
        assertEquals(0, swipe(300f, -20f))
        assertEquals(0, swipe(160f, -160f))
        assertEquals(0, swipe(0f, -20f))
    }

    @Test fun flingThresholdUsesTheLongAxisAndPhysicalDensity() {
        assertEquals(1, swipe(2f, -40f, 0f, -3600f))
        assertEquals(0, swipe(2f, -40f, -5000f, 0f))
        for (d in listOf(1f, 1.5f, 2.75f, 3f)) {
            assertEquals(1, PillAxisGeometry.swipe(true, 2*d, -60*d, 0f, 0f, 221*d, d))
            assertEquals(0, PillAxisGeometry.swipe(true, 2*d, -20*d, 0f, 0f, 221*d, d))
        }
        assertEquals(0, swipe(Float.NaN, -160f))
    }

    private fun swipe(dx: Float, dy: Float, vx: Float = 0f, vy: Float = 0f) =
        PillAxisGeometry.swipe(true, dx, dy, vx, vy, 608f, 2.75f)
}
