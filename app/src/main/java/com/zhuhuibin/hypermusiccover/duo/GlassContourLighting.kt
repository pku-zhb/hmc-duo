package com.zhuhuibin.hypermusiccover.duo

import kotlin.math.*

/** Lighting only where the fused silhouette departs from the native rounded carrier's rim. */
internal object GlassContourLighting {
    fun coverage(x: Float, y: Float, width: Float, height: Float, radius: Float, density: Float): Float {
        if (!listOf(x, y, width, height, radius, density).all { it.isFinite() } ||
            width <= 0f || height <= 0f || density <= 0f) return 0f
        val r = radius.coerceIn(0f, min(width, height) / 2f)
        val qx = abs(x - width / 2f) - (width / 2f - r)
        val qy = abs(y - height / 2f) - (height / 2f - r)
        val distance = abs(hypot(max(qx, 0f), max(qy, 0f)) + min(max(qx, qy), 0f) - r)
        val t = ((distance / density - .35f) / 1.65f).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    fun color(normalX: Float, normalY: Float, coverage: Float): Int {
        if (!normalX.isFinite() || !normalY.isFinite() || !coverage.isFinite()) return 0
        val light = (-.6f * normalX - .8f * normalY).coerceIn(-1f, 1f)
        val shine = .08f + .48f * abs(light).pow(2f)
        val alpha = (255f * shine * coverage.coerceIn(0f, 1f)).roundToInt().coerceIn(0, 255)
        return (alpha shl 24) or if (light >= 0f) 0x00ffffff else 0x00000000
    }
}
