package com.zhuhuibin.hypermusiccover.duo

import kotlin.math.*

/** A closing card buds from the existing capsule, then separates into its fixed category seat. */
internal object RailReturnMotion {
    class Plan internal constructor(val key: String, val selected: String,
        val before: Map<String, RailFusionMotion.Box>, val after: Map<String, RailFusionMotion.Box>,
        val detour: Boolean, val single: Boolean = false) {
        fun frame(progress: Float, oldArt: RailFusionMotion.Box, newArt: RailFusionMotion.Box): RailFusionMotion.Frame {
            val p = if (progress.isFinite()) progress.coerceIn(0f, 1f) else 1f
            val move = smooth(p)
            if (single) {
                val end = after.getValue(key)
                val width = end.w * (.01f + .99f * smooth(p / .3f))
                val height = width + (end.h - end.w) * smooth((p - .2f) / .8f)
                val primary = RailFusionMotion.Box(end.cx - width / 2, end.cy - height / 2, width, height)
                val iconSize = newArt.w * smooth(p / .42f)
                val icon = RailFusionMotion.Box(end.cx + (newArt.cx - end.cx) * move - iconSize / 2,
                    end.cy + (newArt.cy - end.cy) * move - iconSize / 2, iconSize.coerceAtLeast(.01f), iconSize.coerceAtLeast(.01f))
                return RailFusionMotion.Frame(false, primary, mapOf(key to primary), 0f,
                    smooth((p - .35f) / .65f), icon, smooth((p - .1f) / .35f),
                    RailSurfaceContour.shape(primary, null, 0f))
            }
            val old = before.getValue(selected)
            val end = after.getValue(key)
            val primary = mix(old, after.getValue(selected), move)
            val towardBottom = end.cy > old.cy
            val startY = if (towardBottom) old.y + old.h - old.w / 2f else old.y + old.w / 2f
            val travel = smooth((p - .12f) / .88f)
            val size = 1f + (end.w - 1f) * smooth(p / .45f)
            val cx = old.cx + (end.cx - old.cx) * travel -
                if (detour) sin(Math.PI * travel).toFloat() * (old.w * 1.65f) else 0f
            val cy = startY + (end.cy - startY) * travel
            val bubble = RailFusionMotion.Box(cx - size / 2, cy - size / 2, size, size)
            val seats = LinkedHashMap<String, RailFusionMotion.Box>()
            for (item in after.keys) seats[item] = when (item) {
                key -> bubble
                selected -> primary
                else -> mix(before.getValue(item), after.getValue(item), move)
            }
            val tension = sin(Math.PI * p).toFloat().coerceAtLeast(0f)
            return RailFusionMotion.Frame(false, primary, seats, 0f, 1f,
                mix(oldArt, newArt, move), 1f, RailSurfaceContour.shape(primary, bubble, tension))
        }
        fun iconAlpha(progress: Float): Float = smooth((progress - .16f) / .34f)
    }

    fun plan(key: String, selected: String, before: Map<String, RailFusionMotion.Box>,
             after: Map<String, RailFusionMotion.Box>): Plan? {
        if (before.isEmpty() && selected == key && after.keys == setOf(key) && after.getValue(key).valid())
            return Plan(key, selected, before, after, false, single = true)
        if (key in before || before[selected] == null || after[selected] == null || after[key] == null ||
            before.keys + key != after.keys || (before.values + after.values).any { !it.valid() }) return null
        val order = after.keys.toList()
        return Plan(key, selected, before, after, abs(order.indexOf(key) - order.indexOf(selected)) > 1)
    }
    private fun smooth(p: Float) = p.coerceIn(0f, 1f).let { it * it * (3f - 2f * it) }
    private fun mix(a: RailFusionMotion.Box, b: RailFusionMotion.Box, p: Float) = RailFusionMotion.Box(
        a.x + (b.x - a.x) * p, a.y + (b.y - a.y) * p,
        a.w + (b.w - a.w) * p, a.h + (b.h - a.h) * p)
}
