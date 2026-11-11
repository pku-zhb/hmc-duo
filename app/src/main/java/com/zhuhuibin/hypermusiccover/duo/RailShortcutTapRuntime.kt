package com.zhuhuibin.hypermusiccover.duo

import android.os.SystemClock
import android.view.MotionEvent
import android.view.ViewConfiguration
import java.util.WeakHashMap

/** Observe the native controller's own stream, and retain its selected shortcut and launch policy. */
internal object RailShortcutTapRuntime {
    private val states = WeakHashMap<Any, RailShortcutTapState>()
    private val installed = WeakHashMap<ClassLoader, Boolean>()
    private var accepted = 0
    private var downs = 0
    private var hits = 0
    private var ups = 0
    private var cancels = 0
    private var nativeReleases = 0
    private var lastSide = -1
    private var failure = "none"
    private var interactive = false

    private fun side(owner: Any): Int = when {
        Xp.getObjectField(owner, "isLeftShortcutTouched") == true -> 0
        Xp.getObjectField(owner, "isRightShortcutTouched") == true -> 1
        else -> -1
    }

    fun install(controller: Any?) {
        val plugin = runCatching { Xp.getObjectField(controller ?: return, "miuiShortcutPlugin") }.getOrNull() ?: return
        val loader = plugin.javaClass.classLoader ?: return
        if (installed[loader] == true) return
        runCatching {
            val cls = Xp.findClass("com.miui.keyguard.shortcuts.controller.ShortcutMoveController", loader)
            val managerClass = Xp.findClass("com.miui.keyguard.shortcuts.manager.MiuiShortcutManager", loader)
            val manager = managerClass.getField("INSTANCE").get(null)
            Xp.hookAll(cls, "onTouchEvent") { chain ->
                val owner = chain.thisObject
                val event = chain.args.firstOrNull() as? MotionEvent
                val state = states.getOrPut(owner) { RailShortcutTapState() }
                if (event != null) {
                    if (!eligible()) state.cancel()
                    else when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> { downs++; state.cancel() }
                        MotionEvent.ACTION_MOVE -> state.move(event.rawX, event.rawY)
                        MotionEvent.ACTION_UP -> { ups++; state.end(event.rawX, event.rawY, event.eventTime) }
                        MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN -> { cancels++; state.cancel() }
                    }
                }
                // DOWN has to run first: initCommonTouchEvent selects the configured icon.
                // UP has to be observed first: it calls the native onTouchUp synchronously.
                val result = chain.proceed()
                if (event?.actionMasked == MotionEvent.ACTION_DOWN && eligible()) {
                    runCatching {
                        val selected = side(owner)
                        lastSide = selected
                        if (selected >= 0 && result == true) {
                            hits++
                            state.begin(selected, event.rawX, event.rawY, event.eventTime,
                                ViewConfiguration.get(Main.appContext()).scaledTouchSlop.toFloat())
                        }
                    }.onFailure { failure = it.javaClass.simpleName }
                }
                result
            }
            Xp.hookAll(cls, "onTouchUp") { chain ->
                val owner = chain.thisObject
                nativeReleases++
                runCatching {
                    val selected = side(owner)
                    val anim = Xp.getObjectField(owner, "shortcutOccludedAnimController")
                    val state = states[owner]
                    if (state != null && anim != null) {
                        val active = Xp.getObjectField(anim, "isActive") == true
                        interactive = Xp.callMethod(manager, "getInteractive") == true
                        val prepared = RailShortcutActivation.prepare(state, selected, SystemClock.uptimeMillis(),
                            eligible() && interactive, active, object : RailShortcutActivation.Target {
                                override fun initialize(left: Boolean) {
                                    val x = (Xp.getObjectField(owner, "initialTouchX") as Number).toFloat()
                                    val y = (Xp.getObjectField(owner, "initialTouchY") as Number).toFloat()
                                    // A tap never ran onTouchMove, so the OEM has not created its
                                    // launch view or recorded which shortcut is being opened.
                                    Xp.callMethod(anim, "onTouchDown", x, y, left)
                                }
                                override fun activate() { Xp.setBooleanField(anim, "isActive", true) }
                            })
                        if (prepared) {
                            accepted++
                            Xp.log("MCRailShortcut: initialized native tap side=$selected")
                        }
                    }
                }.onFailure { failure = it.javaClass.simpleName; Xp.w("MCRailShortcut: tap activation failed: $it") }
                chain.proceed()
            }
            installed[loader] = true
        }.onFailure { failure = it.javaClass.simpleName; Xp.w("MCRailShortcut: tap adapter unavailable: $it") }
    }

    private fun eligible() = MiniPlayerRuntime.detachedRail() && Main.screenOnCached() &&
        Main.keyguardLocked() && !Main.bouncerShown() && !MiniPlayerScene.keyguardGoingAway

    @JvmStatic fun describe() = "installed=${installed.isNotEmpty()} taps=$accepted downs=$downs hits=$hits " +
        "ups=$ups cancels=$cancels releases=$nativeReleases side=$lastSide interactive=$interactive error=$failure"
}
