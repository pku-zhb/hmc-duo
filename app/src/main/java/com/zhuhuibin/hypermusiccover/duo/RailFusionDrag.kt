package com.zhuhuibin.hypermusiccover.duo

import kotlin.math.abs
import kotlin.math.max

/** A finger owns one exchange. Holding at the waist never advances another island. */
internal object RailFusionDrag {
    const val HANDOFF = .5f
    fun span(length: Float, density: Float): Float = max(72f * density, length * .34f).coerceAtLeast(1f)

    fun progress(start: Float, dy: Float, up: Boolean, distance: Float): Float {
        if (!listOf(start, dy, distance).all { it.isFinite() } || distance <= 0f) return 0f
        // A newly caught settling animation keeps its exact frame, including after the waist.
        val ceiling = max(HANDOFF, start.coerceIn(0f, 1f))
        return (start + (if (up) -dy else dy) / distance * HANDOFF).coerceIn(0f, ceiling)
    }

    fun commit(progress: Float, dy: Float, vy: Float, up: Boolean, density: Float,
               cancelled: Boolean): Boolean {
        if (cancelled || !listOf(progress, dy, vy, density).all { it.isFinite() } || density <= 0) return false
        val toward = if (up) -1f else 1f
        // A deliberate reversal wins over distance accumulated earlier in the gesture.
        if (vy * toward < -450f * density) return false
        return progress >= .32f || (progress > .04f && vy * toward > 1000f * density && dy * toward > 0f)
    }

    fun settleDuration(from: Float, to: Float): Long =
        (RailFusionMotion.DURATION_MS * abs(to - from)).toLong().coerceIn(90L, RailFusionMotion.DURATION_MS)
}
