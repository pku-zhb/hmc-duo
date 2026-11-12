package com.zhuhuibin.hypermusiccover.duo

import org.junit.Assert.*
import org.junit.Test

class GlassContourLightingTest {
    @Test fun nativeRoundedEdgesAreNotDoubledByTheContourBevel() {
        fun c(x: Float, y: Float) = GlassContourLighting.coverage(x, y, 46f, 221f, 23f, 1f)
        assertEquals(0f, c(0f, 100f), .001f)
        assertEquals(0f, c(46f, 100f), .001f)
        assertEquals(0f, c(23f, 0f), .001f)
        assertEquals(0f, c(23f, 221f), .001f)
        assertEquals(1f, c(10f, 100f), .001f)
        assertTrue(c(1f, 100f) in .01f.. .99f)
    }

    @Test fun concaveNecksGainAnEdgeThatFadesIntoTheNativeSides() {
        val capsule = RailFusionMotion.Box(0f, 0f, 46f, 170f)
        val bubble = RailFusionMotion.Box(0f, 178f, 46f, 46f)
        val shape = RailSurfaceContour.shape(capsule, bubble, 1f)
        val neck = shape.neck!!
        val x = (neck.left.start.x + 3f * neck.left.control1.x +
            3f * neck.left.control2.x + neck.left.end.x) / 8f
        val y = (neck.left.start.y + 3f * neck.left.control1.y +
            3f * neck.left.control2.y + neck.left.end.y) / 8f
        assertTrue(GlassContourLighting.coverage(x, y, 46f, 224f, 23f, 1f) > .9f)
        assertEquals(0f, GlassContourLighting.coverage(0f, 50f, 46f, 224f, 23f, 1f), 0f)
    }

    @Test fun lightingRespondsToTheContourNormalOnBothSidesOfANeck() {
        val top = GlassContourLighting.color(0f, -1f, 1f)
        val bottom = GlassContourLighting.color(0f, 1f, 1f)
        assertEquals(0x00ffffff, top and 0x00ffffff)
        assertEquals(0, bottom and 0x00ffffff)
        assertTrue(top ushr 24 > 0)
        assertTrue(bottom ushr 24 > 0)
        assertEquals(0, GlassContourLighting.color(0f, -1f, 0f) ushr 24)
    }

    @Test fun densityDoesNotChangeTheNativeToCustomEdgeHandoff() {
        val low = GlassContourLighting.coverage(1f, 100f, 46f, 224f, 23f, 1f)
        val high = GlassContourLighting.coverage(2.75f, 275f, 126.5f, 616f, 63.25f, 2.75f)
        assertEquals(low, high, .001f)
        assertEquals(0f, GlassContourLighting.coverage(Float.NaN, 0f, 1f, 1f, 1f, 1f), 0f)
    }
}
