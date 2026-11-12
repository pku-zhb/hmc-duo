package com.zhuhuibin.hypermusiccover.duo

import kotlin.math.abs

/** DOWN owns the stream: a native control, an expansion, or one content collapse. */
internal class RailContentGesture {
    enum class Action { NONE, EXPAND, COLLAPSE }
    private val expansion = RailExpandGesture()
    private val collapse = MiniPlayerCollapseGesture()
    private var action = Action.NONE
    private var scrollableCard = false
    var nativeControl = false
        private set
    val claimed: Boolean get() = action != Action.NONE

    fun start(canExpand: Boolean, canCollapse: Boolean, onNativeControl: Boolean,
              startsOnScrollableCard: Boolean = false, atListTop: Boolean = true) {
        reset()
        nativeControl = onNativeControl || startsOnScrollableCard
        scrollableCard = startsOnScrollableCard
        expansion.start(canExpand && !nativeControl)
        collapse.start(canCollapse && !nativeControl && (!scrollableCard || atListTop))
    }

    /** True exactly once, when the original native recipient needs a CANCEL. */
    fun move(dx: Float, dy: Float, slop: Float, atListTop: Boolean): Boolean {
        if (nativeControl || claimed) return false
        if (expansion.move(dx, dy, slop)) {
            collapse.reset()
            action = Action.EXPAND
        } else if (collapse.move(dx, dy, slop, !scrollableCard || atListTop)) {
            expansion.reset()
            action = Action.COLLAPSE
        }
        return claimed
    }

    fun commit(dx: Float, dy: Float, velocityY: Float, density: Float, cancelled: Boolean): Action {
        if (nativeControl || cancelled) return Action.NONE
        return when (action) {
            Action.EXPAND -> if (expansion.commit(dx, dy, velocityY, density, false)) action else Action.NONE
            Action.COLLAPSE -> if (density > 0f && listOf(dx, dy, velocityY, density).all { it.isFinite() }
                && dy > abs(dx) && (dy >= 48f * density || dy >= 16f * density && velocityY > 1000f * density))
                action else Action.NONE
            Action.NONE -> Action.NONE
        }
    }

    fun reset() {
        expansion.reset()
        collapse.reset()
        action = Action.NONE
        nativeControl = false
        scrollableCard = false
    }
}
