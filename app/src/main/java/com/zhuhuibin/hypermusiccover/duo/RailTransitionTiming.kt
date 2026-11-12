package com.zhuhuibin.hypermusiccover.duo

/** Preserve ColorOS timing; leave enough time to read HyperOS's connected glass surfaces. */
internal object RailTransitionTiming {
    fun cardExit(colorOs: Boolean): Long = if (colorOs) 320L else 420L
    fun minimumBirth(colorOs: Boolean): Long = if (colorOs) 240L else 360L

    fun birthLimit(elapsedMs: Long, minimumMs: Long): Float =
        (elapsedMs.coerceAtLeast(0L).toFloat() / minimumMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
}
