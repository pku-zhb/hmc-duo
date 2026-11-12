package com.zhuhuibin.hypermusiccover.duo

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import kotlin.math.ceil

/** A translucent, normal-lit bevel follows the actual union outline; the fill stays native glass. */
internal class GlassContourRim(private val density: Float) {
    private class Ribbon {
        var vertices = FloatArray(0)
        var colors = IntArray(0)
        var count = 0
        fun size(samples: Int) {
            count = (samples + 1) * 4
            if (vertices.size < count) {
                val capacity = Integer.highestOneBit(count - 1) shl 1
                vertices = FloatArray(capacity)
                colors = IntArray(capacity / 2)
            }
        }
    }
    private val measure = PathMeasure()
    private val ribbons = ArrayList<Ribbon>()
    private var used = 0
    private val position = FloatArray(2)
    private val tangent = FloatArray(2)
    private var points = FloatArray(0)
    private var tangents = FloatArray(0)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun update(path: Path, width: Float, height: Float, radius: Float) {
        used = 0
        if (path.isEmpty || width <= 0f || height <= 0f) return
        measure.setPath(path, true)
        do {
            val length = measure.length
            if (!length.isFinite() || length <= 1f) continue
            val count = ceil(length / (density * .8f).coerceAtLeast(1f)).toInt().coerceIn(12, 2048)
            if (points.size < count * 2) {
                val capacity = Integer.highestOneBit(count * 2 - 1) shl 1
                points = FloatArray(capacity); tangents = FloatArray(capacity)
            }
            for (i in 0 until count) {
                measure.getPosTan(length * i / count, position, tangent)
                points[2 * i] = position[0]; points[2 * i + 1] = position[1]
                tangents[2 * i] = tangent[0]; tangents[2 * i + 1] = tangent[1]
            }
            var area = 0f
            for (i in 0 until count) {
                val j = (i + 1) % count
                area += points[2 * i] * points[2 * j + 1] - points[2 * j] * points[2 * i + 1]
            }
            // Path.Op may reverse winding. Lighting and the inset must agree on the outside.
            val winding = if (area >= 0f) 1f else -1f
            val ribbon = ribbons.getOrNull(used) ?: Ribbon().also(ribbons::add)
            ribbon.size(count)
            for (i in 0..count) {
                val j = i % count
                val x = points[2 * j]; val y = points[2 * j + 1]
                val nx = tangents[2 * j + 1] * winding
                val ny = -tangents[2 * j] * winding
                val coverage = GlassContourLighting.coverage(x, y, width, height, radius, density)
                val color = GlassContourLighting.color(nx, ny, coverage)
                ribbon.vertices[4 * i] = x
                ribbon.vertices[4 * i + 1] = y
                ribbon.vertices[4 * i + 2] = x - nx * density * 1.6f
                ribbon.vertices[4 * i + 3] = y - ny * density * 1.6f
                ribbon.colors[2 * i] = color
                ribbon.colors[2 * i + 1] = color and 0x00ffffff
            }
            used++
        } while (measure.nextContour())
    }

    fun clear() { used = 0 }

    fun draw(canvas: Canvas) {
        for (i in 0 until used) {
            val r = ribbons[i]
            // Canvas expects a count of coordinate values, two for each logical vertex.
            canvas.drawVertices(Canvas.VertexMode.TRIANGLE_STRIP, r.count, r.vertices, 0,
                null, 0, r.colors, 0, null, 0, 0, paint)
        }
    }
}
