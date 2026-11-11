package com.zhuhuibin.hypermusiccover.duo

import kotlin.math.*

/** Tangent Bezier bridges between the round ends of native glass shells. No raster mask. */
internal object RailSurfaceContour {
    data class Point(val x: Float, val y: Float)
    data class Cubic(val start: Point, val control1: Point, val control2: Point, val end: Point)
    data class Neck(val left: Cubic, val right: Cubic)
    data class Shape(val boxes: List<RailFusionMotion.Box>, val neck: Neck?) {
        val bounds: RailFusionMotion.Box get() {
            val x = boxes.minOf { it.x }; val y = boxes.minOf { it.y }
            return RailFusionMotion.Box(x, y, boxes.maxOf { it.x + it.w } - x,
                boxes.maxOf { it.y + it.h } - y)
        }
    }

    fun shape(a: RailFusionMotion.Box, b: RailFusionMotion.Box?, tension: Float): Shape {
        if (b == null) return Shape(listOf(a), null)
        val top = if (a.cy <= b.cy) a else b
        val bottom = if (a.cy <= b.cy) b else a
        val r1 = top.w / 2f; val r2 = bottom.w / 2f
        val c1 = Point(top.cx, top.y + top.h - r1)
        val c2 = Point(bottom.cx, bottom.y + r2)
        // The bridge only joins facing round ends; distant/wrapping seats remain independent.
        val dx = c2.x - c1.x; val dy = c2.y - c1.y
        val d = hypot(dx, dy)
        val reach = min(r1, r2) * .95f * tension.coerceIn(0f, 1f)
        val gap = d - r1 - r2
        if (dy <= 0 || d <= abs(r1 - r2) + .01f || reach <= .001f || gap >= reach)
            return Shape(listOf(a, b), null)
        val join = (1f - max(0f, gap) / reach).coerceIn(0f, 1f)
        // Widen the neck with proximity, while preserving tangents at both round boundaries.
        val theta = join * 1.32f
        val sn = sin(theta); val cs = cos(theta)
        val axisX = dx / d; val axisY = dy / d
        val sideX = axisY; val sideY = -axisX
        fun point(c: Point, axial: Float, side: Float) = Point(c.x + axisX * axial + sideX * side,
            c.y + axisY * axial + sideY * side)
        val l1 = point(c1, r1 * cs, -r1 * sn)
        val l2 = point(c2, -r2 * cs, -r2 * sn)
        val rTop = point(c1, r1 * cs, r1 * sn)
        val rBottom = point(c2, -r2 * cs, r2 * sn)
        // Handle length cannot cross the axis, even at the first hairline connection.
        val handle = min(max(0f, d - (r1 + r2) * cs) * .42f,
            min(r1, r2) * sn * .92f / cs.coerceAtLeast(.12f))
        fun control(p: Point, axis: Float, side: Float) = Point(p.x + axisX * axis + sideX * side,
            p.y + axisY * axis + sideY * side)
        val left = Cubic(l1, control(l1, sn * handle, cs * handle),
            control(l2, -sn * handle, cs * handle), l2)
        val right = Cubic(rBottom, control(rBottom, -sn * handle, -cs * handle),
            control(rTop, sn * handle, -cs * handle), rTop)
        return Shape(listOf(a, b), Neck(left, right))
    }
}
