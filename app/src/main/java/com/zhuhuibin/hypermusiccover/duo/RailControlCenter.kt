package com.zhuhuibin.hypermusiccover.duo

import java.lang.ref.WeakReference
import java.util.WeakHashMap

/** Use the OEM panel's own lock-screen/security gate and animated expansion. */
internal object RailControlCenter {
    private val installed = WeakHashMap<ClassLoader, Boolean>()
    private var controller = WeakReference<Any>(null)
    private var error = "none"

    fun install(loader: ClassLoader) {
        if (installed[loader] == true) return
        runCatching {
            val cls = Xp.findClass("com.miui.systemui.controlcenter.ControlCenterImpl", loader)
            Xp.hookAllConstructors(cls) { chain ->
                val result = chain.proceed()
                controller = WeakReference(chain.thisObject)
                result
            }
            // Retain the real process-owned instance, including runtimes that start it later.
            Xp.hookAll(cls, "start") { chain ->
                val result = chain.proceed()
                controller = WeakReference(chain.thisObject)
                result
            }
            installed[loader] = true
        }.onFailure { error = it.javaClass.simpleName; Xp.w("DuoStatus: native panel entry unavailable: $it") }
    }

    fun open(): Boolean {
        val native = controller.get() ?: return false
        if (!Main.screenOnCached() || !Main.keyguardLocked() || !Main.onKeyguardNow() ||
            Main.bouncerShown() || Main.controlCenterShown() || MiniPlayerScene.aodActive ||
            MiniPlayerScene.blocksMiniPlayer || MiniPlayerScene.keyguardGoingAway) return false
        return runCatching {
            // This delegates to ControlCenterControllerImpl.isPanelEnabled(), including the
            // user's expandableInKeyguard setting, device interactivity and native disable flags.
            if (Xp.callMethod(native, "getPanelEnabled") != true) return@runCatching false
            Xp.callMethod(native, "expand", true)
            true
        }.onFailure { error = it.javaClass.simpleName; Xp.w("DuoStatus: native panel tap failed: $it") }
            .getOrDefault(false)
    }

    fun describe() = "nativePanel=${controller.get() != null} panelError=$error"
}
