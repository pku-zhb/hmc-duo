package com.zhuhuibin.hypermusiccover.duo

/** Stable vertical seats: ordinary notifications, individual focus cards, then music. */
internal object RailIslandStrip {
    fun order(keys: List<String>, previous: List<String>, stack: String, music: String): List<String> {
        val present = keys.toSet()
        val middle = (previous + keys).distinct().filter { it in present && it != stack && it != music }
        return listOfNotNull(stack.takeIf { it in present }) + middle + listOfNotNull(music.takeIf { it in present })
    }

    fun cycle(keys: List<String>, selected: String?, up: Boolean): String? {
        if (keys.isEmpty()) return null
        val index = keys.indexOf(selected).coerceAtLeast(0)
        return keys[(index + if (up) keys.size - 1 else 1) % keys.size]
    }

    /** HyperOS follows visible seat order; an edge or stale selection has no next target. */
    fun adjacent(keys: List<String>, selected: String?, up: Boolean): String? {
        val index = keys.indexOf(selected)
        if (index < 0) return null
        return keys.getOrNull(index + if (up) -1 else 1)
    }

    /** Open in native stack order: music, live cards in their stable order, then notifications. */
    fun expansionTarget(collapsed: List<String>, stack: String, music: String): String? =
        music.takeIf { it in collapsed } ?: collapsed.firstOrNull { it != stack }
            ?: stack.takeIf { it in collapsed }

    data class Slot(val key: String, val top: Float, val height: Float, val primary: Boolean)
    data class Plan(val slots: List<Slot>, val hidden: Int, val hiddenAbove: Int = 0, val hiddenBelow: Int = 0)

    /** Fit every seat first. Once even circles cannot fit, discard seats from the top. */
    fun layout(keys: List<String>, selected: String?, width: Float, height: Float,
               density: Float, oneOnly: Boolean): Plan? {
        if (keys.isEmpty() || selected !in keys || !listOf(width, height, density).all { it.isFinite() }
            || density <= 0 || width <= 0 || height < width) return null
        fun gap(count: Int) = (if (count >= 5) 6f else if (count == 4) 8f else 12f) * density
        var count = if (oneOnly) 1 else keys.size
        while (count > 1 && count * width + (count - 1) * gap(count) > height) count--
        val start = if (oneOnly) keys.indexOf(selected) else keys.size - count
        val shown = keys.subList(start, start + count)
        val primaryKey = selected.takeIf { it in shown } ?: shown.first()
        val hidden = if (oneOnly) 0 else start
        val gap = gap(count)
        val desired = maxOf(width, (221f - 27f * maxOf(0, count - 3)) * density)
        val pillHeight = if (hidden > 0) width else
            minOf(desired, height - (count - 1) * (width + gap)).coerceAtLeast(width)
        val total = pillHeight + (count - 1) * (width + gap)
        var y = height - total
        val slots = shown.map { key ->
            val primary = key == primaryKey
            val h = if (primary) pillHeight else width
            Slot(key, y, h, primary).also { y += h + gap }
        }
        return Plan(slots, hidden, hiddenAbove = hidden)
    }
}
