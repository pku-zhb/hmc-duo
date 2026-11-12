package com.zhuhuibin.hypermusiccover.duo

/** Actual card pixels stay native; all remaining content space belongs to the rail. */
internal object RailContentRegion {
    fun emptySpace(y: Float, top: Float, bottom: Float, onCard: Boolean): Boolean =
        !onCard && listOf(y, top, bottom).all { it.isFinite() } && bottom > top &&
            y >= top && y < bottom
}
