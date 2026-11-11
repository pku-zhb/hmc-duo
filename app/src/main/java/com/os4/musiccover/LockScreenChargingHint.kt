package com.os4.musiccover

import java.lang.ref.WeakReference

/** Hide the native charging indication through its controller, including later battery updates. */
internal object LockScreenChargingHint {
    private var controller: WeakReference<Any>? = null
    private var installed = false
    private var suppressed = 0

    fun install(loader: ClassLoader) {
        runCatching {
            val owner = Xp.findClass("com.android.systemui.statusbar.KeyguardIndicationController", loader)
            for (name in listOf("setIndicationArea", "updateDeviceEntryIndication")) {
                Xp.hookAll(owner, name) { chain ->
                    controller = WeakReference(chain.thisObject)
                    chain.proceed()
                }
            }
            val rotate = Xp.findClass("com.android.systemui.keyguard.KeyguardIndicationRotateTextViewController", loader)
            Xp.hookAll(rotate, "updateIndication") { chain ->
                val type = chain.args.firstOrNull() as? Int ?: -1
                if (shouldHide(type, chain.args.getOrNull(1) != null)) {
                    Xp.callMethod(chain.thisObject, "hideIndication", type)
                    suppressed++
                    null
                } else chain.proceed()
            }
            installed = true
        }.onFailure { Xp.w("MCChargingHint: indication hook unavailable: $it") }
    }

    private fun shouldHide(type: Int, hasContent: Boolean): Boolean {
        val owner = controller?.get() ?: return false
        val protection = runCatching {
            Xp.getObjectField(owner, "mBatteryDefender") == true
        }.getOrDefault(true)
        return ChargingHintPolicy.hide(Main.sHideChargingHint, Main.keyguardLocked(), type, hasContent, protection)
    }

    fun refresh() {
        Main.main().post {
            controller?.get()?.let { runCatching { Xp.callMethod(it, "updateDeviceEntryIndication", false) } }
        }
    }

    fun describe(): String = "installed=$installed enabled=${Main.sHideChargingHint} suppressed=$suppressed"
}
