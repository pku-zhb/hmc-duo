package com.zhuhuibin.hypermusiccover.duo

import android.graphics.Matrix
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroupOverlay
import kotlin.math.ceil
import kotlin.math.roundToInt

/** A display-only overlay inherits native keyguard transforms without entering its constraint solver. */
internal object RailStatusRuntime {
    private var view: RailStatusView? = null
    private var parent: ViewGroup? = null
    private var overlay: ViewGroupOverlay? = null
    private var error = "none"
    private val containerToWindow = Matrix()
    private val windowInverse = Matrix()
    private val point = FloatArray(2)
    private val values = FloatArray(9)
    private var holdingX = false
    private var nativeInkX = Float.NaN
    private var correctedInkX = Float.NaN

    @JvmStatic fun update(window: View, plan: RightRailLayout.Plan) {
        runCatching {
            val id = window.resources.getIdentifier("keyguard_root_view", "id", "com.android.systemui")
            val container = if (id == 0) null else window.findViewById<View>(id) as? ViewGroup
            if (container == null) { hide(); error = "keyguard parent unavailable"; return@runCatching }
            val density = window.resources.displayMetrics.density
            val bounds = RailStatusLayout.place((plan.pill.left+plan.pill.right)/2f,
                plan.content.top, density, Main.sRailStatusOffsetYDp) ?: run { hide(); return@runCatching }
            var offsetX = 0; var offsetY = 0; var node: View? = container
            while (node != null && node !== window) {
                val ancestor = node.parent as? View
                offsetX += node.left - (ancestor?.scrollX ?: 0)
                offsetY += node.top - (ancestor?.scrollY ?: 0)
                node = ancestor
            }
            if (node !== window) { hide(); return@runCatching }
            if (parent !== container || view == null) {
                detach()
                parent = container
                overlay = container.overlay
                view = RailStatusView(container.context).also { status ->
                    status.visibility = View.INVISIBLE
                    overlay?.add(status)
                }
            }
            val status = view ?: return@runCatching
            val left = bounds.left-offsetX; val top = bounds.top-offsetY
            val width = ceil(bounds.width()).toInt(); val height = ceil(bounds.height()).toInt()
            if (status.width != width || status.height != height || status.left != left.roundToInt() ||
                status.top != top.roundToInt()) {
                status.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
                status.layout(left.roundToInt(), top.roundToInt(), left.roundToInt()+width, top.roundToInt()+height)
            }
            var correctionX = 0f; var correctionY = 0f
            holdingX = LockScreenRailRuntime.holdRailX()
            nativeInkX = Float.NaN; correctedInkX = Float.NaN
            if (holdingX) {
                containerToWindow.reset(); container.transformMatrixToGlobal(containerToWindow)
                windowInverse.reset(); window.transformMatrixToGlobal(windowInverse)
                if (windowInverse.invert(windowInverse)) {
                    containerToWindow.postConcat(windowInverse)
                    val inkOffset = RailStatusLayout.CENTER_X * density / RailStatusLayout.REFERENCE_DENSITY
                    // Start from canonical layout, excluding our previous translation. The
                    // visible ring is centered at 98, not half the 205-pixel surface width.
                    point[0] = left + inkOffset
                    point[1] = top + bounds.height() / 2f
                    containerToWindow.mapPoints(point)
                    containerToWindow.getValues(values)
                    nativeInkX = point[0]
                    // Preserve the original physical-pixel rounding in place() at scale 1.
                    RailStatusLayout.pinX(bounds.left + inkOffset, point[0],
                        values[Matrix.MSCALE_X], values[Matrix.MSKEW_X],
                        values[Matrix.MSKEW_Y], values[Matrix.MSCALE_Y])?.let {
                        correctionX = it.x; correctionY = it.y
                        correctedInkX = nativeInkX + values[Matrix.MSCALE_X] * it.x + values[Matrix.MSKEW_X] * it.y
                    }
                }
            }
            status.translationX = left-status.left + correctionX
            status.translationY = top-status.top + correctionY
            status.visibility = View.VISIBLE
            status.setListening(true)
            refreshTime()
        }.onFailure {
            hide()
            if (error != it.javaClass.simpleName) {
                error = it.javaClass.simpleName; Xp.w("DuoStatusLayout: $it")
            }
        }
    }

    /** Reposition only the status overlay on the next native frame; retain all rail geometry. */
    @JvmStatic fun refreshPosition() { parent?.rootView?.invalidate() }

    @JvmStatic fun hide() {
        holdingX = false; nativeInkX = Float.NaN; correctedInkX = Float.NaN
        view?.apply { visibility = View.INVISIBLE; setTimeAlpha(0f); setListening(false) }
    }

    /** Called after the shared clock pose is written, independent of pre-draw listener order. */
    @JvmStatic fun refreshTime() {
        view?.setTimeAlpha(if (timeAvailable()) ClockCollapse.statusTimeAlpha() else 0f)
    }
    /** Do not hide the native clock until its destination exists, independent of fade progress. */
    @JvmStatic fun timeAvailable(): Boolean = LockScreenRailRuntime.active() &&
        // OverlayViewGroup is not parented to its host; use the retained real host for sight.
        view?.let { it.isAttachedToWindow && it.visibility == View.VISIBLE } == true &&
        parent?.let { it.isAttachedToWindow && it.isShown } == true &&
        Main.keyguardLocked() && !Main.bouncerShown() && !MiniPlayerScene.keyguardGoingAway

    @JvmStatic fun detach() {
        hide()
        view?.let { overlay?.remove(it) }
        view = null; parent = null; overlay = null
    }
    @JvmStatic fun describe() = "${view?.describe() ?: "not attached"} holdX=$holdingX " +
        "nativeInkX=$nativeInkX correctedInkX=$correctedInkX " +
        "offsetYDp=${RailStatusLayout.normalizeOffsetY(Main.sRailStatusOffsetYDp)} error=$error"
}
