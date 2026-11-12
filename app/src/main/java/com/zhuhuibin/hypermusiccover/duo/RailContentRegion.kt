package com.zhuhuibin.hypermusiccover.duo

/** The clock/empty band ends at the first visible native card, not at the list's bottom. */
internal object RailContentRegion {
    fun clockSpace(y: Float, top: Float, bottom: Float, firstCardTop: Float?): Boolean {
        if (!listOf(y, top, bottom).all { it.isFinite() } || bottom <= top) return false
        val end = firstCardTop?.takeIf { it.isFinite() }?.coerceIn(top, bottom) ?: bottom
        return y >= top && y < end
    }
}
