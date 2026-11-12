package com.zhuhuibin.hypermusiccover.duo

import kotlin.math.*

/** A closing card buds from its nearest visible neighbor into its fixed category seat. */
internal object RailReturnMotion {
    class Plan internal constructor(val key: String, val selected: String,
        val before: Map<String, RailFusionMotion.Box>, val after: Map<String, RailFusionMotion.Box>,
        val source: String, val single: Boolean = false) {
        val surfaceOwnsPrimary get() = single || source == selected

        /** The contour replaces only the two merging surfaces, never unrelated glass. */
        fun ownsBubbleMaterial(item: String): Boolean = item == key || item == source

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
            val old = before.getValue(source)
            val end = after.getValue(key)
            val primary = mix(before.getValue(selected), after.getValue(selected), move)
            val sourceBox = mix(old, after.getValue(source), move)
            val towardBottom = end.cy > old.cy
            val startY = if (towardBottom) old.y + old.h - old.w / 2f else old.y + old.w / 2f
            val travel = smooth((p - .12f) / .88f)
            val size = 1f + (end.w - 1f) * smooth(p / .45f)
            val cx = old.cx + (end.cx - old.cx) * travel
            val cy = startY + (end.cy - startY) * travel
            val bubble = RailFusionMotion.Box(cx - size / 2, cy - size / 2, size, size)
            val seats = LinkedHashMap<String, RailFusionMotion.Box>()
            val alphas = LinkedHashMap<String, Float>()
            for (item in before.keys + after.keys) seats[item] = when (item) {
                key -> bubble
                selected -> primary
                else -> {
                    alphas[item] = when {
                        item !in after -> 1f - smooth(p / .35f)
                        item !in before -> smooth((p - .65f) / .35f)
                        else -> 1f
                    }
                    mix(before[item] ?: after.getValue(item), after[item] ?: before.getValue(item), move)
                }
            }
            val tension = sin(Math.PI * p).toFloat().coerceAtLeast(0f)
            return RailFusionMotion.Frame(false, primary, seats, 0f, 1f,
                mix(oldArt, newArt, move), 1f, RailSurfaceContour.shape(sourceBox, bubble, tension), alphas,
                primaryMaterial = if (surfaceOwnsPrimary) 0f else 1f)
        }
        fun iconAlpha(progress: Float): Float = smooth((progress - .16f) / .34f)
    }

    fun plan(key: String, selected: String, before: Map<String, RailFusionMotion.Box>,
             after: Map<String, RailFusionMotion.Box>): Plan? {
        if (before.isEmpty() && selected == key && after.keys == setOf(key) && after.getValue(key).valid())
            return Plan(key, selected, before, after, key, single = true)
        if (key in before || before[selected] == null || after[selected] == null || after[key] == null ||
            (before.values + after.values).any { !it.valid() }) return null
        // Overflow can remove the uppermost seat. Only a neighbor visible in both layouts
        // can donate a surface; using the selected capsule crossed intervening bubbles.
        val destination = after.getValue(key)
        val source = before.keys.filter { it in after }.minByOrNull {
            abs(after.getValue(it).cy - destination.cy)
        } ?: return null
        return Plan(key, selected, before, after, source)
    }
    private fun smooth(p: Float) = p.coerceIn(0f, 1f).let { it * it * (3f - 2f * it) }
    private fun mix(a: RailFusionMotion.Box, b: RailFusionMotion.Box, p: Float) = RailFusionMotion.Box(
        a.x + (b.x - a.x) * p, a.y + (b.y - a.y) * p,
        a.w + (b.w - a.w) * p, a.h + (b.h - a.h) * p)
}
