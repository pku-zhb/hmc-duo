package com.zhuhuibin.hypermusiccover.duo

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

    @JvmStatic fun update(window: View, plan: RightRailLayout.Plan) {
        runCatching {
            val id = window.resources.getIdentifier("keyguard_root_view", "id", "com.android.systemui")
            val container = if (id == 0) null else window.findViewById<View>(id) as? ViewGroup
            if (container == null) { hide(); error = "keyguard parent unavailable"; return@runCatching }
            val density = window.resources.displayMetrics.density
            val bounds = RailStatusLayout.place((plan.pill.left+plan.pill.right)/2f,
                plan.content.top, density) ?: run { hide(); return@runCatching }
            var offsetX = 0; var offsetY = 0; var node: View? = container
            while (node != null && node !== window) {
                offsetX += node.left; offsetY += node.top; node = node.parent as? View
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
            status.translationX = left-status.left; status.translationY = top-status.top
            status.visibility = View.VISIBLE
            status.setListening(true)
        }.onFailure {
            hide()
            if (error != it.javaClass.simpleName) {
                error = it.javaClass.simpleName; Xp.w("DuoStatusLayout: $it")
            }
        }
    }

    @JvmStatic fun hide() {
        view?.apply { visibility = View.INVISIBLE; setListening(false) }
    }
    @JvmStatic fun detach() {
        hide()
        view?.let { overlay?.remove(it) }
        view = null; parent = null; overlay = null
    }
    @JvmStatic fun describe() = "${view?.describe() ?: "not attached"} error=$error"
}
