package com.zhuhuibin.hypermusiccover.duo

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class RailSurfaceContourTest {
    private val a = RailFusionMotion.Box(0f, 0f, 46f, 180f)
    private val b = RailFusionMotion.Box(0f, 192f, 46f, 46f)
    @Test fun proximityCreatesACurvedNeckButRestAndDistantSeatsStaySeparate() {
        assertNull(RailSurfaceContour.shape(a, b, 0f).neck)
        val neck = RailSurfaceContour.shape(a, b, 1f).neck!!
        assertTrue(neck.left.control1.x > neck.left.start.x)
        assertTrue(neck.left.control2.x > neck.left.end.x)
        assertTrue(neck.right.control1.x < neck.right.start.x)
        assertNull(RailSurfaceContour.shape(a, b.copy(y = 320f), 1f).neck)
    }
    @Test fun neckJoinsEachRoundEndTangentiallyAndRemainsInsideTheBounds() {
        for (t in 1..100) {
            val shape = RailSurfaceContour.shape(a, b, t / 100f)
            val neck = shape.neck ?: continue
            val topCenter = RailSurfaceContour.Point(23f, 157f)
            val bottomCenter = RailSurfaceContour.Point(23f, 215f)
            val l = neck.left
            fun dot(p: RailSurfaceContour.Point, c: RailSurfaceContour.Point, q: RailSurfaceContour.Point) =
                (p.x - c.x) * (q.x - p.x) + (p.y - c.y) * (q.y - p.y)
            assertEquals(0f, dot(l.start, topCenter, l.control1), .01f)
            assertEquals(0f, dot(l.end, bottomCenter, l.control2), .01f)
            for (v in listOf(l.start, l.control1, l.control2, l.end,
                neck.right.start, neck.right.control1, neck.right.control2, neck.right.end)) {
                assertTrue(v.x.isFinite() && v.y.isFinite())
                assertTrue(v.x in 0f..46f && v.y in 0f..238f)
            }
        }
    }
    @Test fun leftAndRightContoursAreMirrorSymmetricWithoutAZeroWidthWaist() {
        val n = RailSurfaceContour.shape(a, b, 1f).neck!!
        assertEquals(46f, n.left.start.x + n.right.end.x, .001f)
        assertEquals(46f, n.left.control1.x + n.right.control2.x, .001f)
        fun midpoint(c: RailSurfaceContour.Cubic) = (c.start.x + 3*c.control1.x + 3*c.control2.x + c.end.x) / 8
        assertTrue(midpoint(n.left) < midpoint(n.right))
    }
}
