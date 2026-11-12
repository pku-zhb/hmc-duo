package com.zhuhuibin.hypermusiccover.duo

import android.graphics.Matrix
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.ImageView
import java.lang.ref.WeakReference
import java.util.WeakHashMap

/** Own placement and animation coordinates; the OEM still performs secure shortcut launches. */
internal object RailShortcutRuntime {
    private var host: FrameLayout? = null
    private var root: View? = null
    private var left: ImageView? = null
    private var right: ImageView? = null
    private var observer: ViewTreeObserver? = null
    private var listener: ViewTreeObserver.OnPreDrawListener? = null
    private var attachment: View.OnAttachStateChangeListener? = null
    private var containerHeight: Int? = null
    private var writtenHeight = 0
    private val leases = WeakHashMap<ImageView, Lease>()
    private val clips = WeakHashMap<ViewGroup, Pair<Boolean, Boolean>>()
    private val installed = WeakHashMap<ClassLoader, Boolean>()
    private data class Origin(val icon: WeakReference<ImageView>, val controller: WeakReference<Any>,
                              var x: Float, var y: Float)
    private val origins = WeakHashMap<View, Origin>()
    private var error = "none"
    private var originUpdates = 0L
    private var active = false

    private class Lease(val icon: ImageView, val wrapper: View) {
        val wrapperParams = FrameLayout.LayoutParams(wrapper.layoutParams as FrameLayout.LayoutParams)
        val iconWidth = icon.layoutParams.width
        val iconHeight = icon.layoutParams.height
        val originalX = icon.translationX
        val originalY = icon.translationY
        var size = 0
        var wrapperLeft = 0
        var wrapperTop = 0
        var x = 0f
        var y = 0f

        fun place(slot: FoldMusicLayout.Box, parentX: Int, parentY: Int) {
            val nextSize = kotlin.math.round(slot.width()).toInt().coerceAtLeast(1)
            val nextLeft = kotlin.math.round(slot.left - parentX).toInt()
            val nextTop = kotlin.math.round(slot.top - parentY).toInt()
            val lp = wrapper.layoutParams as? FrameLayout.LayoutParams ?: return
            if (lp.width != nextSize || lp.height != nextSize || lp.leftMargin != nextLeft ||
                lp.topMargin != nextTop || lp.gravity != (Gravity.TOP or Gravity.LEFT)) {
                lp.width = nextSize; lp.height = nextSize
                lp.gravity = Gravity.TOP or Gravity.LEFT
                lp.leftMargin = nextLeft; lp.topMargin = nextTop
                lp.rightMargin = 0; lp.bottomMargin = 0
                lp.marginStart = nextLeft; lp.marginEnd = 0
                wrapper.layoutParams = lp
            }
            val ip = icon.layoutParams
            if (ip.width != nextSize || ip.height != nextSize) {
                ip.width = nextSize; ip.height = nextSize; icon.layoutParams = ip
            }
            // Fractional placement and the existing AOD inset are separate owners.
            val nextX = slot.left - parentX - nextLeft
            val nextY = slot.top - parentY - nextTop
            if (size == 0 || nextLeft != wrapperLeft || nextX != x) icon.translationX = nextX
            if (size == 0 || nextTop != wrapperTop || nextY != y) icon.translationY = nextY
            size = nextSize; wrapperLeft = nextLeft; wrapperTop = nextTop; x = nextX; y = nextY
        }

        fun restore() {
            val lp = wrapper.layoutParams as? FrameLayout.LayoutParams
            if (lp != null && lp.width == size && lp.height == size &&
                lp.leftMargin == wrapperLeft && lp.topMargin == wrapperTop)
                wrapper.layoutParams = FrameLayout.LayoutParams(wrapperParams)
            val ip = icon.layoutParams
            if (ip.width == size && ip.height == size) {
                ip.width = iconWidth; ip.height = iconHeight; icon.layoutParams = ip
            }
            if (icon.translationX == x) icon.translationX = originalX
            if (icon.translationY == y) icon.translationY = originalY
        }
    }

    @JvmStatic fun install(loader: ClassLoader) {
        runCatching {
            val cls = Xp.findClass("com.android.keyguard.shortcut.MiuiShortcutController", loader)
            Xp.hookAll(cls, "addShortcutViews") { chain ->
                val result = chain.proceed()
                runCatching {
                    val container = chain.args.firstOrNull() as? FrameLayout ?: return@runCatching
                    val controller = chain.thisObject
                    fun icon(isLeft: Boolean) = Xp.callMethod(controller, "onSystemUIAction\$1",
                        Bundle().apply { putBoolean("isLeftShortcutView", isLeft) }, "getShortcutView") as? ImageView
                    val torch = icon(true) ?: return@runCatching
                    val camera = icon(false) ?: return@runCatching
                    bind(container, torch, camera)
                    ShortcutGestureRuntime.install(controller)
                    val plugin = Xp.getObjectField(controller, "miuiShortcutPlugin") ?: return@runCatching
                    installAnimation(plugin.javaClass.classLoader ?: return@runCatching)
                }.onFailure { fail(it) }
                result
            }
        }.onFailure { fail(it) }
    }

    private fun bind(container: FrameLayout, torch: ImageView, camera: ImageView) {
        if (host === container && left === torch && right === camera) return
        detach()
        host = container; root = container.rootView; left = torch; right = camera
        val draw = ViewTreeObserver.OnPreDrawListener {
            runCatching { update() }.onFailure { fail(it); restore() }
            // This observer belongs to the whole shade window. A hidden shortcut child
            // may keep its pending-layout flag after unlock; it must never veto drawing
            // the notification shade or control center. Geometry settles on normal layout.
            true
        }
        listener = draw; observer = container.viewTreeObserver
        observer?.addOnPreDrawListener(draw)
        val attachListener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit
            override fun onViewDetachedFromWindow(v: View) {
                if (host === v) detach()
                v.removeOnAttachStateChangeListener(this)
            }
        }
        attachment = attachListener
        container.addOnAttachStateChangeListener(attachListener)
        update()
    }

    private fun update() {
        val container = host ?: return
        val window = container.rootView.also { root = it }
        if (!Main.sRightRail || !Main.keyguardLocked()) { restore(); return }
        if (MiniPlayerScene.keyguardGoingAway || window.width <= 0 || window.height <= 0) return
        val insets = RailWindowInsets.read(window)
        val geometry = RailShortcutLayout.plan(window.width.toFloat(), window.height.toFloat(),
            window.resources.displayMetrics.density, insets.right.toFloat(),
            insets.bottom.toFloat()) ?: run { restore(); return }
        val cp = container.layoutParams ?: return
        if (containerHeight == null) containerHeight = cp.height
        if (cp.height != window.height) { cp.height = window.height; container.layoutParams = cp }
        writtenHeight = window.height
        for ((icon, slot) in listOf(left to geometry.torch, right to geometry.camera)) {
            icon ?: continue
            val wrapper = icon.parent as? View ?: continue
            if (wrapper.layoutParams !is FrameLayout.LayoutParams) continue
            val parent = wrapper.parent as? View ?: continue
            var x = 0; var y = 0; var current: View? = parent
            var inContainer = false
            while (current != null && current !== window) {
                if (current === container) inContainer = true
                x += current.left; y += current.top
                current = current.parent as? View
            }
            if (current !== window || !inContainer) continue
            leases.getOrPut(icon) { Lease(icon, wrapper) }.place(slot, x, y)
            // Some plugin versions retain a fixed-height content row inside the container.
            // Only the shortcut branch may draw outside that row; never unclip native cards.
            var branch: View? = parent
            while (branch is ViewGroup) {
                clips.getOrPut(branch) { branch.clipChildren to branch.clipToPadding }
                branch.clipChildren = false; branch.clipToPadding = false
                if (branch === container) break
                branch = branch.parent as? View
            }
        }
        active = leases.size == 2
    }

    private fun restore() {
        active = false
        for (lease in leases.values.toList()) lease.restore()
        leases.clear(); origins.clear()
        for ((group, flags) in clips) {
            if (!group.clipChildren) group.clipChildren = flags.first
            if (!group.clipToPadding) group.clipToPadding = flags.second
        }
        clips.clear()
        val container = host
        val lp = container?.layoutParams
        if (lp != null && lp.height == writtenHeight && containerHeight != null) {
            lp.height = containerHeight!!; container.layoutParams = lp
        }
        containerHeight = null
    }

    private fun detach() {
        if (observer?.isAlive == true) listener?.let { observer?.removeOnPreDrawListener(it) }
        observer = null; listener = null
        attachment?.let { host?.removeOnAttachStateChangeListener(it) }
        attachment = null
        restore(); host = null; root = null; left = null; right = null
    }

    private fun owns(icon: ImageView) = active && Main.sRightRail && Main.keyguardLocked() &&
        (icon === left || icon === right)

    private fun installAnimation(loader: ClassLoader) {
        if (installed[loader] == true) return
        runCatching {
            val controller = Xp.findClass("com.miui.keyguard.shortcuts.controller.BaseShortcutAnimController", loader)
            val animation = Xp.findClass("com.miui.keyguard.shortcuts.view.BaseShortcutAnimView", loader)
            Xp.hookAll(controller, "updateShortcutView") { chain ->
                val result = chain.proceed()
                runCatching {
                    val icon = Xp.getObjectField(chain.thisObject, "bottomShortcutView") as? ImageView
                        ?: return@runCatching
                    if (!owns(icon)) return@runCatching
                    val view = Xp.callMethod(chain.thisObject, "getShortcutAnimView") as? View ?: return@runCatching
                    val origin = Origin(WeakReference(icon), WeakReference(chain.thisObject), 0f, 0f)
                    origins[view] = origin
                    captureOrigin(view, origin)
                    Xp.setObjectField(view, "iconCircleCenterX", origin.x)
                    Xp.setObjectField(view, "iconCircleCenterY", origin.y)
                    Xp.callMethod(view, "updateIconViewPositionAndScale", 1f)
                    view.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                        override fun onPreDraw(): Boolean {
                            if (view.viewTreeObserver.isAlive) view.viewTreeObserver.removeOnPreDrawListener(this)
                            runCatching {
                                if (origins[view] !== origin || !owns(icon)) return@runCatching
                                captureOrigin(view, origin)
                                val owner = origin.controller.get() ?: return@runCatching
                                Xp.callMethod(view, "updateViews", Xp.callMethod(owner, "getMovePer"),
                                    Xp.callMethod(owner, "getMoveYPer"), Xp.callMethod(owner, "getActiveAnimPer"))
                            }.onFailure { fail(it) }
                            return true
                        }
                    })
                }.onFailure { fail(it) }
                result
            }
            Xp.hookAll(animation, "updateViews") { chain ->
                runCatching {
                    val view = chain.thisObject as View
                    val origin = origins[view] ?: return@runCatching
                    if (origin.icon.get()?.let(::owns) == true) writeOrigin(view, origin)
                }.onFailure { fail(it) }
                chain.proceed()
            }
            installed[loader] = true
        }.onFailure { fail(it) }
    }

    private fun captureOrigin(view: View, origin: Origin) {
        val icon = origin.icon.get() ?: return
        val toScreen = Matrix(); icon.transformMatrixToGlobal(toScreen)
        val fromScreen = Matrix(); view.transformMatrixToGlobal(fromScreen)
        if (!fromScreen.invert(fromScreen)) return
        fromScreen.preConcat(toScreen)
        val center = floatArrayOf(icon.width / 2f, icon.height / 2f)
        fromScreen.mapPoints(center)
        origin.x = center[0]; origin.y = center[1]
        writeOrigin(view, origin); originUpdates++
    }

    private fun writeOrigin(view: View, origin: Origin) {
        Xp.setObjectField(view, "iconCircleInitCenterX", origin.x)
        Xp.setObjectField(view, "iconCircleInitCenterY", origin.y)
    }

    private fun fail(t: Throwable) {
        if (error == t.javaClass.simpleName) return
        error = t.javaClass.simpleName
        Xp.w("DuoShortcutLayout: $t")
    }

    @JvmStatic fun describe() = "active=$active owners=${leases.size} container=${host?.height ?: 0} " +
        "requestedHeight=${host?.layoutParams?.height ?: 0} pendingLayout=${host?.isLayoutRequested == true} " +
        "animation=${installed.isNotEmpty()} originUpdates=$originUpdates error=$error"
}
