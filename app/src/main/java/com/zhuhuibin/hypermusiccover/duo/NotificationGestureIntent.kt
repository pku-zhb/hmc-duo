package com.zhuhuibin.hypermusiccover.duo

import kotlin.math.abs

/** A vertical gesture needs native movement or exposure changes, not a window MOVE alone. */
internal class NotificationGestureIntent {
    private var downX = 0f
    private var downY = 0f
    private var scroll: Int? = null
    private var nativeState: String? = null
    private var nativeFade: Float? = null
    private var slop = 0f
    private var tracking = false
    private var releasedDirection = 0

    fun begin(x: Float, y: Float, position: Int?, touchSlop: Float,
              state: String? = null, fade: Float? = null) {
        downX = x; downY = y; scroll = position
        nativeState = state
        nativeFade = fade?.takeIf { it.isFinite() }
        slop = touchSlop.coerceAtLeast(1f)
        tracking = x.isFinite() && y.isFinite()
        releasedDirection = 0
    }

    private fun direction(x: Float, y: Float): Int {
        if (!tracking || !x.isFinite() || !y.isFinite()) return 0
        val dx = abs(x - downX)
        val dy = abs(y - downY)
        return if (dy <= slop || dy <= dx) 0 else if (y < downY) -1 else 1
    }

    fun needsNativeEvidence(x: Float, y: Float): Boolean = direction(x, y) != 0

    fun moved(x: Float, y: Float, position: Int?, state: String? = null, fade: Float? = null): Boolean {
        if (direction(x, y) == 0) return false
        val scrolled = scroll?.let { from -> position?.let { abs(it.toLong() - from) > 2L } } == true
        // A single normal row can expose LIST/STACK at the same scroll coordinate as NUMBER.
        val stateChanged = nativeState != null && state != null && nativeState != state
        val faded = nativeFade?.let { from -> fade?.let { it.isFinite() && abs(it - from) > .04f } } == true
        return scrolled || stateChanged || faded
    }

    /** The window observer runs before native dispatch; keep this until HANDS_UP resolves. */
    fun finish(x: Float, y: Float) {
        releasedDirection = direction(x, y)
        tracking = false
    }

    /** Equal physical targets cannot distinguish a visible upward-opened row from NUMBER. */
    fun collapsedAtRelease(target: Float, number: Float, wantsList: Boolean?, rowsVisible: Boolean): Boolean {
        val upward = releasedDirection < 0
        releasedDirection = 0
        return abs(target - number) < 1f && !(upward && wantsList == false && rowsVisible)
    }

    fun clear() {
        tracking = false; scroll = null; nativeState = null; nativeFade = null
        releasedDirection = 0
    }
}
