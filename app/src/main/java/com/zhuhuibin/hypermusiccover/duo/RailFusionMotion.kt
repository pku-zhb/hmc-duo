package com.zhuhuibin.hypermusiccover.duo

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Two native glass surfaces exchange roles; their icons never fade during a two-seat switch. */
internal object RailFusionMotion {
    const val DURATION_MS = 420L
    data class Box(val x: Float, val y: Float, val w: Float, val h: Float) {
        val cx get() = x + w / 2f
        val cy get() = y + h / 2f
        fun valid() = listOf(x, y, w, h).all { it.isFinite() } && w > 0f && h >= w
    }
    data class Frame(val incoming: Boolean, val primary: Box, val seats: Map<String, Box>,
        val companionMaterial: Float, val detail: Float, val icon: Box, val iconAlpha: Float)

    class Plan internal constructor(val from: String, val to: String,
        val before: Map<String, Box>, val after: Map<String, Box>, val joined: Boolean,
        val single: Boolean) {
        /** Art seats have the actual (inset) artwork size, rather than the outer glass diameter. */
        fun frame(progress: Float, oldArt: Box, newArt: Box, iconShare: Float): Frame {
            val t = if (progress.isFinite()) progress.coerceIn(0f, 1f) else 1f
            val incoming = t >= .5f
            val leg = smooth(if (incoming) 2f * t - 1f else 2f * t)
            val old = before.getValue(from)
            val next = after.getValue(to)
            val seats = LinkedHashMap<String, Box>()
            val primary: Box
            val icon: Box
            var material = 1f
            var iconAlpha = 1f
            if (single) {
                val circle = Box(old.x, old.cy - old.w / 2, old.w, old.w)
                primary = if (incoming) mix(circle, next, leg) else mix(old, circle, leg)
                val artCircle = inset(circle, iconShare)
                icon = if (incoming) mix(artCircle, newArt, leg) else mix(oldArt, artCircle, leg)
                // No second visible seat in ColorOS: change the picture only at the round waist.
                iconAlpha = if (incoming) smooth((t - .5f) / .12f) else 1f - smooth((t - .38f) / .12f)
                seats[if (incoming) to else from] = primary
            } else if (joined) {
                val targetCircle = before.getValue(to)
                val oldCircle = after.getValue(from)
                val union = Box(old.x, min(old.y, targetCircle.y), old.w,
                    max(old.y + old.h, targetCircle.y + targetCircle.h) - min(old.y, targetCircle.y))
                primary = if (incoming) mix(union, next, leg) else mix(old, union, leg)
                for (key in before.keys) seats[key] = mix(before.getValue(key), after.getValue(key), smooth(t))
                seats[from] = if (incoming) oldCircle else primary
                seats[to] = if (incoming) primary else targetCircle
                val companion = if (incoming) oldCircle else targetCircle
                // Only the glass disappears under the merged shell; its live icon stays visible.
                val covered = (min(primary.y + primary.h, companion.y + companion.h) -
                    max(primary.y, companion.y)).coerceIn(0f, companion.h) / companion.h
                material = 1f - smooth(covered)
                icon = if (incoming) mix(inset(targetCircle, iconShare), newArt, leg)
                    else mix(oldArt, inset(oldCircle, iconShare), leg)
            } else {
                // A wrap must not grow a long capsule through unrelated bubbles. Exchange size
                // in the two original seats instead; interpolation preserves every intervening gap.
                for (key in before.keys) seats[key] = mix(before.getValue(key), after.getValue(key), smooth(t))
                primary = seats.getValue(if (incoming) to else from)
                val circleArt = inset(primary, iconShare)
                icon = if (incoming) mix(circleArt, newArt, leg) else mix(oldArt, circleArt, leg)
            }
            val detail = if (incoming) smooth((t - .57f) / .43f) else 1f - smooth(t / .38f)
            return Frame(incoming, primary, seats, material, detail, icon, iconAlpha)
        }
    }

    fun plan(from: String, to: String, before: Map<String, Box>, after: Map<String, Box>): Plan? {
        if (from == to || before[from] == null || after[to] == null ||
            (before.values + after.values).any { !it.valid() }) return null
        val single = before.size == 1 && after.size == 1
        if (single) {
            // A geometry/configuration change is settled, not animated across a stale viewport.
            if (before.getValue(from) != after.getValue(to)) return null
            return Plan(from, to, before, after, false, true)
        }
        if (before.keys != after.keys || before[to] == null || after[from] == null) return null
        val keys = before.keys.toList()
        val adjacent = abs(keys.indexOf(from) - keys.indexOf(to)) == 1
        return Plan(from, to, before, after, adjacent, false)
    }

    private fun inset(box: Box, share: Float): Box {
        val side = min(box.w, box.h) * share
        return Box(box.cx - side / 2, box.cy - side / 2, side, side)
    }
    private fun smooth(t: Float): Float = t.coerceIn(0f, 1f).let { it * it * (3f - 2f * it) }
    private fun mix(a: Box, b: Box, t: Float) = Box(a.x + (b.x - a.x) * t,
        a.y + (b.y - a.y) * t, a.w + (b.w - a.w) * t, a.h + (b.h - a.h) * t)
}
