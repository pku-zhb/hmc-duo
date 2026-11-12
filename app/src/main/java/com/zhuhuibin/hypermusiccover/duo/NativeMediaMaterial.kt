package com.zhuhuibin.hypermusiccover.duo

import android.view.View

/** One current OEM calculation at handoff; no cached effect, extra collector, or frame loop. */
internal object NativeMediaMaterial {
    enum class Result { REQUESTED, DEFERRED, UNAVAILABLE }
    private const val HEADER = "com.android.systemui.statusbar.notification.mediacontrol.MiuiMediaHeaderView"
    // Verified against the current ROM's MediaViewBinder: this transform performs one selection
    // from its input StateFlows and the interactor's current AOD/material/context state.
    private const val TRANSFORM = "com.android.systemui.statusbar.notification.style.view." +
        "MediaViewBinder\$bind\$1\$1\$invokeSuspend\$\$inlined\$combine\$1\$3"
    private var warned = false

    fun rebind(controller: Any, child: View): Result {
        if (!child.isAttachedToWindow) return Result.DEFERRED
        var header: View? = child
        while (header != null && header.javaClass.name != HEADER) header = header.parent as? View
        val target = header ?: return Result.UNAVAILABLE
        return runCatching {
            val interactor = Xp.getObjectField(controller, "notificationMaterialStateInteractor")
                ?: return Result.UNAVAILABLE
            // The OEM transform deliberately does nothing inside this transition. Retry on a
            // subsequent native binding/wake event, never report that a skipped call applied.
            val aod = Xp.getObjectField(interactor, "notifiFullAodController")
            if (Xp.getObjectField(aod, "mWillDoAodAnim") == true) return Result.DEFERRED
            val loader = controller.javaClass.classLoader ?: return Result.UNAVAILABLE
            fun current(name: String): Any? = Xp.callMethod(Xp.getObjectField(interactor, name), "getValue")
            val inputs = arrayOf(current("notifStyle"), current("isShowingKeyguardNotification"),
                current("isLightWallPaper"), null)
            check(inputs.take(3).none { it == null }) { "current material state unavailable" }
            val transform = loader.loadClass(TRANSFORM)
            val constructor = transform.declaredConstructors.first { it.parameterCount == 3 &&
                it.parameterTypes[0].isInstance(target) && it.parameterTypes[1].isInstance(interactor) &&
                it.parameterTypes[2].name == "kotlin.coroutines.Continuation" }.apply { isAccessible = true }
            val oneShot = constructor.newInstance(target, interactor, null)
            val collector = loader.loadClass("kotlinx.coroutines.flow.internal.NopCollector")
                .getDeclaredField("INSTANCE").apply { isAccessible = true }.get(null)
            transform.getDeclaredMethod("invoke", Any::class.java, Any::class.java, Any::class.java)
                .apply { isAccessible = true }.invoke(oneShot, collector, inputs, null)
            Result.REQUESTED
        }.getOrElse {
            if (!warned) {
                warned = true
                Xp.w("MCMediaMaterial: current OEM rebind unavailable; awaiting native update: $it")
            }
            Result.UNAVAILABLE
        }
    }
}
