package com.os4.musiccover

import kotlin.math.tanh

/** Motion inside a fixed rail shell. Neither the frame nor its glass follows a swipe. */
internal object RailContentMotion {
    data class Frame(val y: Float, val alpha: Float, val incoming: Boolean)

    fun pull(dy: Float, density: Float): Float =
        if (!dy.isFinite() || !density.isFinite() || density <= 0f) 0f
        else 8f * density * tanh(dy / (64f * density))

    fun frame(progress: Float, next: Boolean, density: Float, startY: Float = 0f): Frame {
        val p = progress.coerceIn(0f, 1f)
        val direction = if (next) -1f else 1f
        val travel = 28f * density
        val incoming = p >= .5f
        val t = if (incoming) (p - .5f) * 2f else p * 2f
        val eased = t * t * (3f - 2f * t)
        return if (incoming) Frame(-direction * travel * (1f - eased), eased, true)
            else Frame(startY + (direction * travel - startY) * eased, 1f - eased, false)
    }
}
