// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package com.zhuhuibin.hypermusiccover.duo

/**
 * The editor reuses the keyguard window, and the control centre is drawn over it. Their explicit
 * SystemUI states decide whether the lock screen the mini player belongs to is actually what is
 * showing.
 *
 * Not the full-screen charging animation: it is a view added on top of the lock screen, which
 * stays as it is underneath. Counted as a block, the pill went for the animation's few seconds
 * and the media card stood in for it, then the pill came back as the animation ended.
 */
internal object MiniPlayerScene {
    @Volatile private var editorActive = false
    @Volatile private var controlCenterActive = false
    @Volatile var keyguardGoingAway = false
        private set
    @Volatile var aodActive = false
        private set
    @Volatile private var fullScreenAod = false
    private var aodWindow = java.lang.ref.WeakReference<android.view.View>(null)
    private var aodGeometryRevision = -1L
    private val exitEvents = java.util.ArrayDeque<String>()
    private val exitState = KeyguardExitState()

    fun exitTrace(): String = synchronized(exitEvents) { exitEvents.joinToString(" ; ") }


    /**
     * The doze is the full-screen AOD, the one that keeps the row (holdButtonsThroughDoze).
     * Any other AOD - the plugin's or the linkage style - is the OEM's alone: the row goes
     * out with the lock screen's own fade and stays out until the wake.
     */
    val fullScreenAodActive: Boolean
        get() = aodActive && fullScreenAod

    val customAodActive: Boolean
        get() = aodActive && !fullScreenAod

    val blocksMiniPlayer: Boolean
        get() = editorActive

    val controlCenterIsActive: Boolean
        get() = controlCenterActive

    /** Each hook alone: a renamed class costs the one signal it carried, nothing else. */
    fun install(cl: ClassLoader) {
        fun hook(className: String, methodName: String, before: (List<Any?>) -> Unit) {
            runCatching {
                Xp.hookAll(Xp.findClass(className, cl), methodName) { chain ->
                    before(chain.args)
                    chain.proceed()
                }
            }.onFailure { Xp.w("MCMini: scene hook $methodName unavailable: $it") }
        }
        hook("com.android.keyguard.injector.KeyguardViewMediatorInjector", "keyguardGoingAway") {
            keyguardLeaving()
        }
        hook("com.android.systemui.statusbar.policy.KeyguardStateControllerImpl", "notifyKeyguardState") {
            transitionExit { exitState.showing(it.firstOrNull() == true) }
        }
        // This explicit state also carries a cancelled exit; showing=true alone does not.
        hook("com.android.systemui.statusbar.policy.KeyguardStateControllerImpl", "notifyKeyguardGoingAway") {
            (it.firstOrNull() as? Boolean)?.let { away -> transitionExit { exitState.exit(away) } }
        }
        hook("com.android.systemui.statusbar.StatusBarStateControllerImpl", "setIsDozing") {
            setAodActive(it.firstOrNull() == true)
        }
        hook("com.android.keyguard.editor.KeyguardEditorHelper", "setEditorState") {
            setEditorActive(it.firstOrNull()?.toString() != "IDEL")
        }
        runCatching {
            val listener = Xp.findClass(
                "com.miui.systemui.controlcenter.container.ControlCenterContainerController\$onExpandChangeListener\$1",
                cl,
            )
            Xp.hookAll(listener, "onExpandStateChanged") { chain ->
                val state = chain.args.firstOrNull()?.toString()
                if (state != null && state != "COLLAPSED") setControlCenterActive(true)
                val result = chain.proceed()
                if (state == "COLLAPSED") setControlCenterActive(false)
                result
            }
        }.onFailure { Xp.w("MCMini: control centre scene hook unavailable: $it") }
    }

    private fun setEditorActive(active: Boolean) {
        if (editorActive == active) return
        editorActive = active
        StatusBarArtwork.refresh()
        MiniPlayerRuntime.refresh()
    }

    private fun setControlCenterActive(active: Boolean) {
        if (controlCenterActive == active) return
        controlCenterActive = active
        StatusBarArtwork.refresh()
        MiniPlayerRuntime.refresh()
    }

    private fun setAodActive(active: Boolean) {
        // Asked once per change of the doze, not per frame: it reflects into the AOD manager.
        val window = Main.sContainer?.rootView
        val revision = Main.screenGeometry().revision
        val sameDisplay = aodWindow.get() === window && aodGeometryRevision == revision
        val fullScreen = active && Main.fullAodOn()
        if (aodActive == active && fullScreenAod == fullScreen && sameDisplay) return
        aodWindow = java.lang.ref.WeakReference(window)
        aodGeometryRevision = revision
        fullScreenAod = fullScreen
        aodActive = active
        AodTransitionRuntime.scene(fullScreen)
        WallpaperHandoffDiagnostics.note("doze", "active=$active away=$keyguardGoingAway")
        StatusBarArtwork.refresh()
        if (!active) MiniPlayerRuntime.aodEnded()
        MiniPlayerRuntime.refresh()
    }

    /** Explicit geometry publication invalidates the display-scoped full-AOD capability. */
    fun geometryChanged() {
        val window = Main.sContainer?.rootView
        val revision = Main.screenGeometry().revision
        if (aodWindow.get() === window && aodGeometryRevision == revision) return
        aodWindow = java.lang.ref.WeakReference(window)
        aodGeometryRevision = revision
        if (!aodActive) return
        val fullScreen = Main.fullAodOn()
        val changed = fullScreenAod != fullScreen
        fullScreenAod = fullScreen
        AodTransitionRuntime.scene(fullScreen)
        if (changed) {
            StatusBarArtwork.refresh()
            MiniPlayerRuntime.refresh()
        }
    }

    /** Shared by all verified exit hooks, including the binder-side exit animation route. */
    fun keyguardLeaving() = transitionExit { exitState.exit(true) }

    /** A new sleep is explicit; a late setIsDozing callback is not a new lock session. */
    fun startedGoingToSleep() = transitionExit { exitState.sleep() }

    /** Publish in the same critical section as the event: binder/main callbacks cannot
     * return an older state and write it after a newer sleep or cancellation. */
    private fun transitionExit(change: () -> Boolean) = synchronized(exitState) {
        setKeyguardGoingAway(change())
    }

    private fun setKeyguardGoingAway(goingAway: Boolean) {
        if (keyguardGoingAway == goingAway) return
        if (goingAway) MiniPlayerRuntime.rememberLockState()
        keyguardGoingAway = goingAway
        WallpaperHandoffDiagnostics.note("unlock", "away=$goingAway aod=$aodActive screen=${Main.screenOnCached()}")
        synchronized(exitEvents) {
            exitEvents.addLast("${android.os.SystemClock.uptimeMillis()} away=$goingAway " +
                "aod=$aodActive screen=${Main.screenOnCached()}")
            while (exitEvents.size > 16) exitEvents.removeFirst()
        }
        StatusBarArtwork.refresh()
        if (goingAway) {
            MiniPlayerRuntime.forgetRestoreScene()
            val release = Runnable {
                if (!keyguardGoingAway) return@Runnable
                CoverBackdrop.refreshVisibility()
                CoverMorphLayer.cancel()
                CoverCardLayer.hideNow()
                MiniPlayerRuntime.keyguardExiting()
            }
            if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) release.run()
            else Main.main().post(release)
        } else {
            Main.main().post { if (!keyguardGoingAway) CoverBackdrop.sync(false) }
        }
        MiniPlayerRuntime.refresh()
    }
}
