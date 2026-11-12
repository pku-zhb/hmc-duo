package com.zhuhuibin.hypermusiccover.duo

/** A pile's visible edge is not a usable notification row. All inputs are screen pixels. */
internal object NotificationRowExposure {
    const val MIN_VISIBLE_FRACTION = .85f
    const val MIN_NATIVE_SCALE = .995f
    const val MIN_NATIVE_ALPHA = .85f

    fun requiredHeight(actualHeight: Float, viewportHeight: Float): Float =
        minOf(actualHeight, viewportHeight) * MIN_VISIBLE_FRACTION

    fun usable(actualHeight: Float, visibleHeight: Float, viewportHeight: Float,
               nativeScale: Float, nativeAlpha: Float = 1f): Boolean =
        listOf(actualHeight, visibleHeight, viewportHeight, nativeScale, nativeAlpha).all { it.isFinite() } &&
            actualHeight > 0f && viewportHeight > 0f && nativeScale >= MIN_NATIVE_SCALE &&
            nativeAlpha >= MIN_NATIVE_ALPHA &&
            visibleHeight >= requiredHeight(actualHeight, viewportHeight)

    /** Use one continuous readable segment, not the sum of slivers between overlaid cards. */
    fun uncoveredHeight(top: Float, bottom: Float, blockers: List<Pair<Float, Float>>): Float {
        if (!top.isFinite() || !bottom.isFinite() || bottom <= top) return 0f
        var edge = top
        var largest = 0f
        for ((start, end) in blockers.sortedBy { it.first }) {
            if (!start.isFinite() || !end.isFinite() || end <= edge || start >= bottom) continue
            largest = maxOf(largest, (minOf(start, bottom) - edge).coerceAtLeast(0f))
            edge = maxOf(edge, minOf(end, bottom))
        }
        return maxOf(largest, bottom - edge).coerceAtLeast(0f)
    }
}
