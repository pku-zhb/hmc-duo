package com.zhuhuibin.hypermusiccover.duo

import kotlin.math.abs

/** Claim an upward content-area drag before keyguard, then commit only on a deliberate release. */
internal class RailExpandGesture {
    var pending = false
        private set
    var claimed = false
        private set

    fun start(eligible: Boolean) { pending = eligible; claimed = false }
    fun move(dx: Float, dy: Float, slop: Float): Boolean {
        if (!pending || claimed) return false
        if (dy > slop || abs(dx) > slop && abs(dx) >= abs(dy)) { pending = false; return false }
        if (-dy > slop && -dy > abs(dx) * 1.2f) {
            claimed = true
            return true
        }
        return false
    }
    fun commit(dx: Float, dy: Float, velocityY: Float, density: Float, cancelled: Boolean): Boolean =
        claimed && !cancelled && density > 0f && listOf(dx, dy, velocityY, density).all { it.isFinite() } &&
            -dy > abs(dx) && (-dy >= 48f * density || -dy >= 16f * density && velocityY < -1000f * density)
    fun reset() { pending = false; claimed = false }
}
