package com.zhuhuibin.hypermusiccover.duo

import kotlin.math.abs
import kotlin.math.max

/** Axis-dependent capsule rules; horizontal legacy geometry must not reshape a vertical rail. */
internal object PillAxisGeometry {
    fun rowWidth(requested: Float, height: Float, vertical: Boolean): Float =
        if (vertical) requested.coerceAtLeast(1f) else requested.coerceAtLeast(height)

    /** -1 previous, +1 next, 0 insufficient/cross-axis motion. Up means next on the rail. */
    fun swipe(vertical: Boolean, dx: Float, dy: Float, vx: Float, vy: Float,
              length: Float, density: Float): Int {
        if (!listOf(dx, dy, vx, vy, length, density).all { it.isFinite() } || density <= 0f) return 0
        val along = if (vertical) dy else dx
        val across = if (vertical) dx else dy
        val velocity = if (vertical) vy else vx
        if (abs(along) <= abs(across)) return 0
        if (abs(along) < max(48f * density, length * .15f) && abs(velocity) <= 1200f * density) return 0
        return if (along < 0f) 1 else -1
    }
}
