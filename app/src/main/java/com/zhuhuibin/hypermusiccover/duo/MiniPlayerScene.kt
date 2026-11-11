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
    private val exitEvents = java.util.ArrayDeque<String>()

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
            setKeyguardGoingAway(true)
        }
        hook("com.android.systemui.statusbar.policy.KeyguardStateControllerImpl", "notifyKeyguardState") {
            setKeyguardGoingAway(it.firstOrNull() != true)
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
        val keyguardExitReset = AodWakePolicy.resetExit(aodActive, active, keyguardGoingAway)
        if (keyguardExitReset) keyguardGoingAway = false
        // Asked once per change of the doze, not per frame: it reflects into the AOD manager.
        val fullScreen = active && (aodActive && fullScreenAod || Main.fullAodOn())
        if (aodActive == active && fullScreenAod == fullScreen && !keyguardExitReset) return
        fullScreenAod = fullScreen
        aodActive = active
        WallpaperHandoffDiagnostics.note("doze", "active=$active away=$keyguardGoingAway")
        StatusBarArtwork.refresh()
        if (!active) MiniPlayerRuntime.aodEnded()
        MiniPlayerRuntime.refresh()
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
                CoverBackdrop.refreshVisibility()
                CoverMorphLayer.cancel()
                CoverCardLayer.hideNow()
                MiniPlayerRuntime.keyguardExiting()
            }
            if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) release.run()
            else Main.main().post(release)
        } else {
            Main.main().post { CoverBackdrop.sync(false) }
        }
        MiniPlayerRuntime.refresh()
    }
}
