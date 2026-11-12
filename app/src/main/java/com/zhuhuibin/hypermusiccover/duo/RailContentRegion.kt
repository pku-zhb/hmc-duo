package com.zhuhuibin.hypermusiccover.duo

/** The clock/empty band ends at the first visible native card, not at the list's bottom. */
internal object RailContentRegion {
    fun clockSpace(y: Float, top: Float, bottom: Float, firstCardTop: Float?,
                   emptyBandBottom: Float? = null): Boolean {
        if (!listOf(y, top, bottom).all { it.isFinite() } || bottom <= top) return false
        // With every card folded, the absence of a boundary must not turn the entire
        // screen into an island gesture target. Use the measured clock band instead.
        val end = (firstCardTop?.takeIf { it.isFinite() }
            ?: emptyBandBottom?.takeIf { it.isFinite() }
            ?: (top + bottom) / 2f).coerceIn(top, bottom)
        return y >= top && y < end
    }
}
