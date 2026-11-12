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
        val companionMaterial: Float, val detail: Float, val icon: Box, val iconAlpha: Float,
        val surface: RailSurfaceContour.Shape, val seatAlpha: Map<String, Float> = emptyMap())

    class Plan internal constructor(val from: String, val to: String,
        val before: Map<String, Box>, val after: Map<String, Box>, val joined: Boolean,
        val single: Boolean, val windowed: Boolean = false) {
        /** Art seats have the actual (inset) artwork size, rather than the outer glass diameter. */
        fun frame(progress: Float, oldArt: Box, newArt: Box, iconShare: Float,
                  recoilY: Float = 0f): Frame {
            val t = if (progress.isFinite()) progress.coerceIn(0f, 1f) else 1f
            if (windowed) return windowFrame(t, oldArt, newArt, iconShare)
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
                val upwards = targetCircle.cy < old.cy
                // Keep the shell's length: bring its leading round end onto the target bubble.
                // At the held handoff both pictures are inside the merged shell. The outgoing
                // picture reaches its final, separate bubble seat only after release.
                val merged = Box(old.x,
                    if (upwards) targetCircle.y else targetCircle.y + targetCircle.h - old.h,
                    old.w, old.h)
                val exitCap = Box(merged.x,
                    if (upwards) merged.y + merged.h - merged.w else merged.y,
                    merged.w, merged.w)
                val drag = 2f * t
                primary = if (incoming) mix(merged, next, leg) else mix(old, merged, drag)
                for (key in before.keys) seats[key] = before.getValue(key)
                seats[from] = if (incoming) mix(exitCap, oldCircle, leg) else primary
                seats[to] = if (incoming) primary else targetCircle
                val companion = seats.getValue(if (incoming) from else to)
                // Only the glass disappears under the merged shell; its live icon stays visible.
                val covered = (min(primary.y + primary.h, companion.y + companion.h) -
                    max(primary.y, companion.y)).coerceIn(0f, companion.h) / companion.h
                material = 1f - smooth(covered)
                icon = if (incoming) mix(inset(targetCircle, iconShare), newArt, leg)
                    else mix(oldArt, inset(exitCap, iconShare), drag)
            } else {
                // A wrap must not grow a long capsule through unrelated bubbles. Exchange size
                // in the two original seats instead; interpolation preserves every intervening gap.
                for (key in before.keys) seats[key] = mix(before.getValue(key), after.getValue(key), smooth(t))
                primary = seats.getValue(if (incoming) to else from)
                val circleArt = inset(primary, iconShare)
                icon = if (incoming) mix(circleArt, newArt, leg) else mix(oldArt, circleArt, leg)
            }
            val detail = if (incoming) smooth((t - .57f) / .43f) else 1f - smooth(t / .38f)
            // Recoil belongs to the active shell, not the whole rail. Keep semantic progress
            // monotone while the runtime's physical spring carries this independent offset.
            val recoil = if (joined && recoilY.isFinite()) recoilY else 0f
            val drawnPrimary = primary.copy(y = primary.y + recoil)
            val drawnIcon = icon.copy(y = icon.y + recoil)
            seats[if (incoming) to else from] = drawnPrimary
            val companion = if (single) null else seats.getValue(if (incoming) from else to)
            val tension = if (joined) kotlin.math.sin(t * Math.PI).toFloat().coerceAtLeast(0f) else 0f
            return Frame(incoming, drawnPrimary, seats, material, detail, drawnIcon, iconAlpha,
                RailSurfaceContour.shape(drawnPrimary, companion, tension))
        }

        /** Overflow moves a bounded viewport, not a neck across an invisible item. The old
         * edge leaves before the new edge enters, keeping the configured visible-item count. */
        private fun windowFrame(t: Float, oldArt: Box, newArt: Box, share: Float): Frame {
            val incoming = t >= .5f
            val old = before.getValue(from)
            val next = after.getValue(to)
            val primary = mix(old, next, smooth(t))
            val seats = LinkedHashMap<String, Box>()
            val alphas = LinkedHashMap<String, Float>()
            val top = min(before.values.minOf { it.y }, after.values.minOf { it.y })
            val bottom = max(before.values.maxOf { it.y + it.h }, after.values.maxOf { it.y + it.h })
            fun circle(b: Box) = Box(b.x, b.y, b.w, b.w)
            fun edge(b: Box) = circle(b).copy(y = if (b.cy < old.cy) top else bottom - b.w)
            for (key in before.keys + after.keys) {
                val a = before[key]?.let(::circle) ?: edge(after.getValue(key))
                val b = after[key]?.let(::circle) ?: edge(before.getValue(key))
                seats[key] = mix(a, b, smooth(t))
                alphas[key] = when {
                    key == from && incoming -> if (key in after) smooth((t - .5f) * 2f) else 0f
                    key == to && !incoming -> if (key in before) 1f - smooth(t * 2f) else 0f
                    key !in after -> 1f - smooth(t * 2f)
                    key !in before -> smooth((t - .5f) * 2f)
                    else -> 1f
                }
            }
            seats[if (incoming) to else from] = primary
            val anchor = inset(Box(primary.x, primary.y, primary.w, primary.w), share)
            val leg = smooth(if (incoming) (t - .5f) * 2f else t * 2f)
            val icon = if (incoming) mix(anchor, newArt, leg) else mix(oldArt, anchor, leg)
            val alpha = if (incoming) smooth((t - .5f) / .16f) else 1f - smooth((t - .34f) / .16f)
            val detail = if (incoming) smooth((t - .57f) / .43f) else 1f - smooth(t / .38f)
            return Frame(incoming, primary, seats, 1f, detail, icon, alpha,
                RailSurfaceContour.shape(primary, null, 0f), alphas)
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
        if (before.keys != after.keys) {
            if (before.size != after.size || before.keys.intersect(after.keys).isEmpty() ||
                before.getValue(from).w != after.getValue(to).w ||
                before.getValue(from).h != after.getValue(to).h) return null
            return Plan(from, to, before, after, false, false, windowed = true)
        }
        if (before[to] == null || after[from] == null) return null
        val keys = before.keys.toList()
        val adjacent = abs(keys.indexOf(from) - keys.indexOf(to)) == 1
        if (adjacent && (before.getValue(from).w != after.getValue(to).w ||
                before.getValue(from).h != after.getValue(to).h)) return null
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
