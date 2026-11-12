package com.zhuhuibin.hypermusiccover.duo

import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver
import java.lang.ref.WeakReference
import java.util.WeakHashMap

/** Clips only the native lock-screen status strip; its alpha animation remains entirely native. */
internal object LockscreenStatusBar {
    private val tracked = WeakHashMap<View, Strip>()
    private var installed = false
    private var error = "none"

    private class Strip(view: View) {
        val reference = WeakReference(view)
        val observer: ViewTreeObserver = view.viewTreeObserver
        var held = false
        var naturalClip: Rect? = view.clipBounds
        // A distinct empty rectangle cannot be confused with the OEM's ordinary empty clip.
        val hiddenClip = Rect(-1, -1, -1, -1)
        val beforeDraw = ViewTreeObserver.OnPreDrawListener {
            runCatching { update() }.onFailure { error = it.javaClass.simpleName; restore() }
            true
        }
        fun update() {
            val view = reference.get() ?: return
            val scene = MiniPlayerScene
            val hide = LockscreenStatusBarPolicy.hide(Main.sHideLockStatusBar, Main.keyguardLocked(),
                scene.keyguardGoingAway, Main.bouncerShown(), scene.blocksMiniPlayer,
                scene.controlCenterIsActive || Main.miniPlayerControlCenterUp())
            if (!hide) { restore(); return }
            val current = view.clipBounds
            if (!held || current != hiddenClip) naturalClip = current
            held = true
            if (current != hiddenClip) view.clipBounds = hiddenClip
        }
        fun restore() {
            if (!held) return
            reference.get()?.let { if (it.clipBounds == hiddenClip) it.clipBounds = naturalClip }
            held = false
        }
        fun detach() {
            restore()
            if (observer.isAlive) observer.removeOnPreDrawListener(beforeDraw)
        }
    }

    @JvmStatic fun install(loader: ClassLoader) {
        if (installed) return
        runCatching {
            val owner = Xp.findClass("com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView", loader)
            Xp.hookAll(owner, "onAttachedToWindow") { chain ->
                val result = chain.proceed()
                (chain.thisObject as? View)?.let { view ->
                    runCatching {
                        tracked.remove(view)?.detach()
                        val strip = Strip(view)
                        tracked[view] = strip
                        strip.observer.addOnPreDrawListener(strip.beforeDraw)
                        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                            override fun onViewAttachedToWindow(view: View) = Unit
                            override fun onViewDetachedFromWindow(view: View) {
                                tracked.remove(view)?.detach()
                                view.removeOnAttachStateChangeListener(this)
                            }
                        })
                        strip.update()
                    }.onFailure { error = it.javaClass.simpleName }
                }
                result
            }
            installed = true
        }.onFailure { error = it.javaClass.simpleName }
    }

    @JvmStatic fun refresh() {
        Main.main().post {
            tracked.values.toList().forEach { strip ->
                runCatching { strip.update() }.onFailure {
                    error = it.javaClass.simpleName
                    strip.restore()
                }
            }
        }
    }
    @JvmStatic fun describe(): String = "installed=$installed enabled=${Main.sHideLockStatusBar} " +
        "strips=${tracked.size} hidden=${tracked.values.count { it.held }} error=$error"
}
