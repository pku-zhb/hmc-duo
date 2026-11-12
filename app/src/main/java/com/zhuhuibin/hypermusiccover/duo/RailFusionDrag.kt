package com.zhuhuibin.hypermusiccover.duo

import kotlin.math.abs
import kotlin.math.max

/** A finger owns one exchange. Holding at the waist never advances another island. */
internal object RailFusionDrag {
    const val HANDOFF = .5f
    fun span(length: Float, density: Float): Float = max(72f * density, length * .34f).coerceAtLeast(1f)

    /** The old nudge's rubber band starts near 1:1 and reaches exact contact at this pull. */
    fun contactSpan(travel: Float, density: Float): Float =
        if (!travel.isFinite() || !density.isFinite() || density <= 0f) 1f
        else max(72f * density, abs(travel) * 1.8f).coerceAtLeast(1f)

    fun progress(start: Float, dy: Float, up: Boolean, distance: Float,
                 elastic: Boolean = false): Float {
        if (!listOf(start, dy, distance).all { it.isFinite() } || distance <= 0f) return 0f
        // A newly caught settling animation keeps its exact frame, including after the waist.
        val ceiling = max(HANDOFF, start.coerceIn(0f, 1f))
        val ratio = (if (up) -dy else dy).toDouble() / distance
        // MiniCardMorph.rubber's give=.8 curve, normalized to finish at the contact point.
        // ColorOS keeps its existing linear waist; HyperOS translates the complete shell.
        val pull = if (elastic) Math.copySign(1.8 * abs(ratio) / (1.0 + .8 * abs(ratio)), ratio)
            else ratio
        return (start + pull * HANDOFF).coerceIn(0.0, ceiling.toDouble()).toFloat()
    }

    fun commit(progress: Float, dy: Float, vy: Float, up: Boolean, density: Float,
               cancelled: Boolean): Boolean {
        if (cancelled || !listOf(progress, dy, vy, density).all { it.isFinite() } || density <= 0) return false
        val toward = if (up) -1f else 1f
        // A deliberate reversal wins over distance accumulated earlier in the gesture.
        if (vy * toward < -450f * density) return false
        return progress >= .32f || (progress > .04f && vy * toward > 1000f * density && dy * toward > 0f)
    }

    /** Pictures exchange native owners at the waist, so both must hand over at zero recoil. */
    fun recoilWeight(progress: Float): Float {
        if (!progress.isFinite()) return 0f
        return smooth((abs(progress - HANDOFF) / .08f).coerceIn(0f, 1f))
    }

    /** Pixels per second for the independent shell spring; its initial displacement stays zero. */
    fun releaseVelocity(progress: Float, vy: Float, up: Boolean, density: Float,
                        commit: Boolean, cancelled: Boolean = false): Float {
        if (cancelled || !listOf(progress, vy, density).all { it.isFinite() } || density <= 0f) return 0f
        val d = density.toDouble()
        val hand = vy.toDouble().coerceIn(-2400.0 * d, 2400.0 * d) * .35
        // Contact stores tension even after the finger pauses. Releasing an upward pull
        // initially pushes the shell downward; a downward pull has the symmetric response.
        // Reversing or cancelling does not invent another impulse.
        val contact = smooth((progress / HANDOFF).coerceIn(0f, 1f))
        val tension = if (commit) (if (up) 1.0 else -1.0) * 250.0 * d * contact else 0.0
        return (hand + tension).coerceIn(-1000.0 * d, 1000.0 * d)
            .coerceIn(-Float.MAX_VALUE.toDouble(), Float.MAX_VALUE.toDouble()).toFloat()
    }

    private fun smooth(value: Float): Float = value * value * (3f - 2f * value)

    fun settleDuration(from: Float, to: Float, hyperOs: Boolean = false): Long {
        val duration = if (hyperOs) RailFusionMotion.HYPEROS_DURATION_MS else RailFusionMotion.DURATION_MS
        return (duration * abs(to - from)).toLong().coerceIn(90L, duration)
    }
}
