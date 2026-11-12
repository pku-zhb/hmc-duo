package com.zhuhuibin.hypermusiccover.duo

import android.view.MotionEvent
import android.view.View
import java.util.WeakHashMap

/** Share native shortcut hit regions with rail routing; mirror only the relocated left axis. */
internal object ShortcutGestureRuntime {
    private val installed = WeakHashMap<ClassLoader, Boolean>()
    private var downs = 0
    private var hits = 0
    private var ups = 0
    private var cancels = 0
    private var side = -1
    private var startX = 0f
    private var startY = 0f
    private var dx = 0f
    private var dy = 0f
    private var failure = "none"
    private data class Gesture(val x: Float, val downTime: Long)
    private val mirrored = WeakHashMap<Any, Gesture>()
    private val ownership = WeakHashMap<Any, ShortcutTouchOwnership>()
    private val nativeEvents = WeakHashMap<Any, MotionEvent>()
    private var rejectedStreams = 0
    private var protectedBottomMoves = 0
    private val contentStreams = WeakHashMap<Any, Long>()

    /** Stock HyperOS treats everything below the indication text as the bottom area.
     * Reflowed cards occupy that band, so clear only that legacy intercept flag for their
     * streams. All other panel interception and the actual shortcut plugin remain native. */
    @JvmStatic fun installContentBoundary(loader: ClassLoader) {
        runCatching {
            val count = Xp.findClass("com.miui.systemui.notification.view.NotificationNumStateAreaTouchHandler", loader)
            val streams = WeakHashMap<Any, Long>()
            Xp.hookAll(count, "onInterceptTouchEvent") { chain ->
                val event = chain.args.firstOrNull() as? MotionEvent
                val owner = chain.thisObject
                if (event?.actionMasked == MotionEvent.ACTION_DOWN) {
                    streams.remove(owner)
                    if (eligible() && MiniPlayerRuntime.nativeContentAt(event.rawX, event.rawY))
                        streams[owner] = event.downTime
                }
                if (event != null && streams[owner] == event.downTime && eligible()) {
                    // The hidden native count footer must not own the card drawn in its band.
                    Xp.setObjectField(owner, "isDownInNumStateArea", false)
                    Xp.setObjectField(owner, "pendingClick", false)
                    Xp.setObjectField(owner, "handledByNumState", null)
                    null
                } else chain.proceed()
            }
        }.onFailure { Xp.w("DuoShortcut: count area boundary unavailable: $it") }
        runCatching {
            val cls = Xp.findClass("com.android.systemui.shade.NotificationPanelViewController\$TouchHandler", loader)
            Xp.hookAll(cls, "onInterceptTouchEvent") { chain ->
                val event = chain.args.firstOrNull() as? MotionEvent
                val handler = chain.thisObject
                if (event?.actionMasked == MotionEvent.ACTION_DOWN) {
                    contentStreams.remove(handler)
                    if (eligible() && MiniPlayerRuntime.nativeContentAt(event.rawX, event.rawY))
                        contentStreams[handler] = event.downTime
                }
                fun clearLegacyArea() {
                    if (event == null || contentStreams[handler] != event.downTime || !eligible()) return
                    runCatching {
                        val panel = Xp.getObjectField(handler, "this\$0")
                        val touch = Xp.getObjectField(panel, "mTouchHandlerInjector")
                        val keyguard = Xp.getObjectField(touch, "keyguardPanelViewTouchHandlerInjector")
                        val injector = Xp.getObjectField(keyguard, "keyguardPanelViewInjector")
                        val bottom = Xp.callMethod(Xp.getObjectField(injector, "keyguardBottomAreaInjector\$delegate"), "getValue")
                        if (Xp.getObjectField(bottom, "mTouchAtKeyguardBottomArea") == true) {
                            Xp.setObjectField(bottom, "mTouchAtKeyguardBottomArea", false)
                            protectedBottomMoves++
                        }
                    }.onFailure { failure = it.javaClass.simpleName }
                }
                clearLegacyArea()
                val result = chain.proceed()
                // DOWN computes the native flag inside the inlined interceptor.
                if (event?.actionMasked == MotionEvent.ACTION_DOWN) clearLegacyArea()
                if (event?.actionMasked == MotionEvent.ACTION_UP || event?.actionMasked == MotionEvent.ACTION_CANCEL)
                    contentStreams.remove(handler)
                result
            }
        }.onFailure { failure = it.javaClass.simpleName; Xp.w("DuoShortcut: content boundary unavailable: $it") }
    }
    private var projected = 0
    private var assistedHits = 0
    private var peakDistance = 0f
    private var lastPeak = 0f
    private var lastActive = false

    private fun eligible() = MiniPlayerRuntime.detachedRail() && Main.screenOn() &&
        Main.keyguardLocked() && !Main.bouncerShown() && !MiniPlayerScene.aodActive && !MiniPlayerScene.keyguardGoingAway

    fun hitsShortcut(icon: View, other: View?, x: Float, y: Float): Boolean {
        // Native hit testing waits for alpha == 1. Accept the visible wake ramp while
        // retaining native activation and rejecting doze, hidden icons and unlock.
        if (!eligible() || !icon.isShown || !ShortcutGesturePolicy.shortcutVisible(icon.alpha) ||
            icon.width <= 0 || icon.height <= 0) return false
        val at = IntArray(2).also(icon::getLocationOnScreen)
        val dx = x - at[0] - icon.width / 2f
        val dy = y - at[1] - icon.height / 2f
        val neighbour = other?.takeIf { it.isShown && it.width > 0 }?.let {
            val xy = IntArray(2).also(it::getLocationOnScreen)
            kotlin.math.hypot(x - xy[0] - it.width / 2f, y - xy[1] - it.height / 2f)
        } ?: Float.POSITIVE_INFINITY
        return ShortcutGesturePolicy.shortcutHit(dx, dy, minOf(icon.width, icon.height) / 2f,
            icon.resources.displayMetrics.density, neighbour)
    }

    fun install(controller: Any?) {
        val plugin = runCatching { Xp.getObjectField(controller ?: return, "miuiShortcutPlugin") }.getOrNull() ?: return
        val loader = plugin.javaClass.classLoader ?: return
        if (installed[loader] == true) return
        runCatching {
            val cls = Xp.findClass("com.miui.keyguard.shortcuts.controller.ShortcutMoveController", loader)
            Xp.hookAll(cls, "isTouchOnIcon") { chain ->
                val observed = runCatching {
                    val icon = chain.args.getOrNull(0) as? View
                    val owner = chain.thisObject
                    val event = nativeEvents[owner]
                    val left = Xp.getObjectField(owner, "leftIcon") as? View
                    val right = Xp.getObjectField(owner, "rightIcon") as? View
                    val other = if (icon === left) right else left
                    if (!eligible() || event == null || icon == null ||
                        (icon !== left && icon !== right)) null
                    else hitsShortcut(icon, other, event.rawX, event.rawY)
                }.getOrNull()
                // The OEM compares local event coordinates with screen icon coordinates.
                // Keep one coordinate space and never union the old hit area with the new one.
                if (observed == true) assistedHits++
                observed ?: chain.proceed()
            }
            Xp.hookAll(cls, "initCommonTouchEvent") { chain ->
                val result = chain.proceed()
                runCatching {
                    val owner = chain.thisObject
                    val event = chain.args.firstOrNull() as? MotionEvent
                    if (event != null) {
                        if (!eligible() || event.pointerCount != 1 ||
                            event.actionMasked == MotionEvent.ACTION_CANCEL) mirrored.remove(owner)
                        else if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                            mirrored.remove(owner)
                            // Selection still comes from the OEM's actual icon hit test.
                            if (Xp.getObjectField(owner, "isLeftShortcutTouched") == true)
                                mirrored[owner] = Gesture(event.x, event.downTime)
                        } else {
                            val gesture = mirrored[owner]
                            if (gesture != null && gesture.downTime == event.downTime &&
                                Xp.getObjectField(owner, "isLeftShortcutTouched") == true) {
                                Xp.setObjectField(owner, "touchX",
                                    ShortcutGesturePolicy.nativeX(true, true, gesture.x, event.x))
                                projected++
                            }
                            if (event.actionMasked == MotionEvent.ACTION_UP) mirrored.remove(owner)
                        }
                    }
                }.onFailure { failure = it.javaClass.simpleName }
                result
            }
            Xp.hookAll(cls, "onTouchEvent") { chain ->
                val event = chain.args.firstOrNull() as? MotionEvent
                val owner = chain.thisObject
                if (event != null && eligible()) runCatching {
                    if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                        val left = Xp.getObjectField(owner, "leftIcon") as? View
                        val right = Xp.getObjectField(owner, "rightIcon") as? View
                        if (left != null && right != null) {
                            val hit = hitsShortcut(left, right, event.rawX, event.rawY) ||
                                hitsShortcut(right, left, event.rawX, event.rawY)
                            ownership.getOrPut(owner) { ShortcutTouchOwnership() }.start(event.downTime, hit)
                            if (!hit) rejectedStreams++
                        }
                    }
                }.onFailure { failure = it.javaClass.simpleName }
                if (event != null && ownership[owner]?.allows(event.downTime) == false)
                    return@hookAll false
                runCatching {
                    if (event?.actionMasked == MotionEvent.ACTION_DOWN) peakDistance = 0f
                    if (event?.actionMasked == MotionEvent.ACTION_UP) {
                        lastPeak = peakDistance
                        val anim = Xp.getObjectField(chain.thisObject, "shortcutOccludedAnimController")
                        lastActive = Xp.getObjectField(anim, "isActive") == true
                    }
                }
                // A diagnostic failure must never prevent native dispatch or cause a second dispatch.
                if (event != null) nativeEvents[owner] = event
                val result = try { chain.proceed() } finally { nativeEvents.remove(owner) }
                runCatching {
                    if (event != null && MiniPlayerRuntime.detachedRail()) {
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN -> {
                                downs++
                                startX = event.rawX; startY = event.rawY; dx = 0f; dy = 0f
                                side = when {
                                    Xp.getObjectField(chain.thisObject, "isLeftShortcutTouched") == true -> 0
                                    Xp.getObjectField(chain.thisObject, "isRightShortcutTouched") == true -> 1
                                    else -> -1
                                }
                                if (side >= 0 && result == true) hits++
                            }
                            MotionEvent.ACTION_MOVE -> {
                                dx = event.rawX - startX; dy = event.rawY - startY
                                val distance = (Xp.getObjectField(chain.thisObject, "moveDistance") as Number).toFloat()
                                peakDistance = maxOf(peakDistance, distance)
                            }
                            MotionEvent.ACTION_UP -> { ups++; dx = event.rawX - startX; dy = event.rawY - startY }
                            MotionEvent.ACTION_CANCEL -> cancels++
                        }
                    }
                }.onFailure { failure = it.javaClass.simpleName }
                result
            }
            installed[loader] = true
        }.onFailure { failure = it.javaClass.simpleName; Xp.w("DuoShortcut: diagnostics unavailable: $it") }
    }

    @JvmStatic fun describe() = "installed=${installed.isNotEmpty()} launch=native projected=$projected downs=$downs hits=$hits " +
        "assisted=$assistedHits rejected=$rejectedStreams bottomProtected=$protectedBottomMoves ups=$ups cancels=$cancels side=$side dx=${dx.toInt()} dy=${dy.toInt()} " +
        "lastPeak=${lastPeak.toInt()} lastActive=$lastActive error=$failure"
}
