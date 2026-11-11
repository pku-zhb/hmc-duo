package com.zhuhuibin.hypermusiccover.duo

import android.animation.AnimatorListenerAdapter
import android.view.View
import android.view.ViewTreeObserver
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.IdentityHashMap

/** Observes the OEM's row removal; never starts, cancels or retimes its animator. */
internal object NativeNotificationReturn {
    private val tracked = IdentityHashMap<View, Observation>()
    private var installed = false
    private var alphaProperty: Any? = null
    private var alphaGetter: Method? = null
    private var appearFraction: Field? = null

    fun install(loader: ClassLoader) {
        if (installed) return
        runCatching {
            val owner = Xp.findClass("com.android.systemui.statusbar.notification.row.ExpandableNotificationRow", loader)
            val method = owner.declaredMethods.single { m ->
                val p = m.parameterTypes
                m.name == "performRemoveAnimation" && m.returnType == Long::class.javaPrimitiveType &&
                    p.size == 8 && p[0] == Long::class.javaPrimitiveType && p[1] == Float::class.javaPrimitiveType &&
                    p[2] == Boolean::class.javaPrimitiveType && p[3] == Boolean::class.javaPrimitiveType &&
                    p[4] == Runnable::class.java && p[5] == Runnable::class.java &&
                    p[6] == AnimatorListenerAdapter::class.java
            }
            // MIUI's normal remove animator writes its own add/remove transition-alpha, not
            // View.alpha. Heads-up removal uses AOSP's content/outline appear fraction instead.
            val injector = Xp.findClass("com.android.systemui.statusbar.notification.row.ExpandableViewInjector", loader)
            alphaProperty = injector.getDeclaredField("FOLME_NOTIF_ADD_REMOVE_TRANSITION_ALPHA")
                .apply { isAccessible = true }.get(null)
            alphaGetter = Xp.findMethodExact(alphaProperty!!.javaClass, "getValue", Any::class.java)
            val appearOwner = Xp.findClass("com.android.systemui.statusbar.notification.row.ActivatableNotificationView", loader)
            appearFraction = appearOwner.getDeclaredField("mAppearAnimationFraction")
                .apply { isAccessible = true }
            check(appearFraction!!.type == Float::class.javaPrimitiveType)
            Xp.hook(method) { chain ->
                val observation = (chain.thisObject as? View)?.let(tracked::get)
                    ?: return@hook chain.proceed()
                val args = chain.args.toTypedArray()
                observation.startRemoval(args[2] == true)
                val done = args[5] as? Runnable
                val listener = args[6] as? AnimatorListenerAdapter
                args[5] = Runnable {
                    try { done?.run() }
                    finally { observation.finish(!observation.cancelled) }
                }
                args[6] = NativeNotificationAnimatorListener(listener,
                    { observation.cancelled = true },
                    { observation.finish(!observation.cancelled) })
                // The original start/finish callbacks and listener still run in their native order.
                try { chain.proceed(args).also { observation.captureStart() } }
                catch (error: Throwable) { observation.finish(false); throw error }
            }
            installed = true
        }.onFailure { Xp.w("MCNotif: native return observer unavailable: $it") }
    }

    fun begin(key: String, row: View, onProgress: (Float) -> Unit, onFinished: (Boolean) -> Unit,
              onUnavailable: () -> Unit = { onFinished(false) }): Boolean {
        if (!installed || !row.isAttachedToWindow || !row.isShown || visibility(row) <= .01f) return false
        // Validate both carriers before recapture changes the native pipeline. An unsupported
        // firmware/row uses the caller's shared card/bubble animation from the outset.
        if (readCarrier(row, false) == null || readCarrier(row, true) == null) return false
        tracked.values.filter { it.key == key || it.row === row }.toList().forEach { it.dispose() }
        val observation = Observation(key, row, onProgress, onFinished, onUnavailable)
        tracked[row] = observation
        row.addOnAttachStateChangeListener(observation)
        observation.observeTree()
        observation.report(0f)
        return true
    }

    /** Only drops our observation; the native animation and its callbacks remain untouched. */
    fun cancel(row: View) { tracked[row]?.dispose() }

    private fun visibility(row: View): Float = (row.alpha * row.transitionAlpha).coerceIn(0f, 1f)

    private fun readCarrier(row: View, headsUp: Boolean): Float? = runCatching {
        val value = if (headsUp) appearFraction!!.getFloat(row)
            else (alphaGetter!!.invoke(alphaProperty, row) as Number).toFloat()
        value.takeIf { it.isFinite() }
    }.getOrNull()

    private class Observation(val key: String, val row: View,
        val onProgress: (Float) -> Unit, val onFinished: (Boolean) -> Unit,
        val onUnavailable: () -> Unit) : ViewTreeObserver.OnPreDrawListener, View.OnAttachStateChangeListener {
        var removeStarted = false
        var cancelled = false
        private var headsUp = false
        private var startCarrier: Float? = null
        private var observer: ViewTreeObserver? = null
        private var traversals = 0
        private var progress = -1f
        private fun active() = tracked[row] === this

        fun startRemoval(headsUp: Boolean) {
            removeStarted = true
            this.headsUp = headsUp
            captureStart()
        }

        fun captureStart() {
            if (!active() || startCarrier != null) return
            // -1 means the AOSP appear animator has not initialized yet. Its delayed/menu
            // removal can initialize after performRemoveAnimation returns; wait for a value.
            startCarrier = readCarrier(row, headsUp)?.takeIf { it > .001f }
        }

        fun observeTree() {
            val next = row.viewTreeObserver
            if (observer === next) return
            observer?.takeIf { it.isAlive }?.removeOnPreDrawListener(this)
            observer = next
            next.addOnPreDrawListener(this)
        }

        override fun onPreDraw(): Boolean {
            if (!active()) return true
            if (!row.isAttachedToWindow) {
                confirmDetached()
                return true
            }
            if (!row.isShown) {
                finish(true)
                return true
            }
            if (!removeStarted) {
                if (++traversals >= 3) {
                    // A native pipeline may remove without animating; hidden rows above are complete.
                    // If it kept the row, let the caller use its own shared card/bubble exit path.
                    dispose()
                    runCatching(onUnavailable).onFailure { Xp.w("MCNotif: return fallback callback failed: $it") }
                } else {
                    // Ensure the bounded pipeline check completes even when the row stays idle.
                    row.postInvalidateOnAnimation()
                }
                return true
            }
            captureStart()
            val initial = startCarrier ?: return true
            val current = readCarrier(row, headsUp) ?: run { finish(false); return true }
            if (current >= 0f) report((1f - current / initial).coerceIn(0f, 1f))
            return true
        }

        fun report(value: Float) {
            if (!active() || !value.isFinite() || kotlin.math.abs(value - progress) < .001f) return
            progress = value
            runCatching { onProgress(value) }.onFailure { Xp.w("MCNotif: return progress callback failed: $it") }
        }

        fun finish(commit: Boolean) {
            if (!active()) return
            if (commit) report(1f)
            if (!active()) return
            dispose()
            runCatching { onFinished(commit) }.onFailure { Xp.w("MCNotif: return completion callback failed: $it") }
        }

        fun dispose() {
            if (active()) tracked.remove(row)
            observer?.takeIf { it.isAlive }?.removeOnPreDrawListener(this)
            observer = null
            row.removeOnAttachStateChangeListener(this)
        }

        override fun onViewAttachedToWindow(v: View) { if (active()) observeTree() }
        private fun confirmDetached() {
            // NotificationStackScrollLayout can briefly reparent a row into its transient layer.
            // Only the next main-loop turn confirms removal; do not depend on an end callback
            // from a row whose animator was abandoned during detach.
            Main.main().post { if (active() && !row.isAttachedToWindow) finish(!cancelled) }
        }

        override fun onViewDetachedFromWindow(v: View) { confirmDetached() }
    }
}
