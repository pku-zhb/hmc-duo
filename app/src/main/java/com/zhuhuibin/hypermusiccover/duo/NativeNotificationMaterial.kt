// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Lewewe (HyperMusicCover-Enhanced, https://github.com/Lewewe/HyperMusicCover-Enhanced)
// Developed with the assistance of OpenAI Codex.
package com.zhuhuibin.hypermusiccover.duo

import android.view.View
import android.graphics.Point
import java.lang.reflect.Method
import java.util.IdentityHashMap
import java.util.WeakHashMap

/** Ask the OEM binder to select today's material, not replay a previous shade/AOD effect. */
internal object NativeNotificationMaterial {
    private class Binding(val method: Method, val args: Array<Any?>, val kind: NotificationMaterialBinding) {
        var refreshes = 0
    }
    private val bindings = WeakHashMap<View, Binding>()
    // Active transitions only; end() removes these closures and their view references.
    private val fades = IdentityHashMap<View, NotificationPropertyLease<List<NotificationBlendColor>>>()
    private val dirty = WeakHashMap<View, Boolean>()
    private var readColors: Method? = null
    private var writeColors: Method? = null
    private var refreshing = false
    private var installed = false

    fun install(loader: ClassLoader) {
        if (installed) return
        installed = true
        runCatching {
            readColors = View::class.java.getMethod("getMiBackgroundBlendColors")
            writeColors = View::class.java.getMethod("setMiBackgroundBlendColors", ArrayList::class.java)
        }.onFailure { Xp.w("MCNativeMaterial: blend readback unavailable: $it") }
        runCatching {
            val binder = Xp.findClass(
                "com.android.systemui.statusbar.notification.style.view.ExpandableNotificationRowViewBinder", loader)
            val name = "bindInternal\$updateEffect"
            val methods = binder.declaredMethods.mapNotNull { method ->
                if (method.name != name) null else NotificationMaterialBinding.from(
                    method.parameterTypes.map { it.name })?.let { method to it }
            }
            check(methods.isNotEmpty()) { "no supported notification material binder signature" }
            for ((method, kind) in methods) {
                method.isAccessible = true
                Xp.hook(method) { chain ->
                    val result = chain.proceed()
                    val row = chain.args.getOrNull(1) as? View
                    if (!refreshing && row != null) {
                        val args = chain.args.toTypedArray()
                        // Do not retain the row itself through a weak map's value. These native
                        // interactor and key/effect refs contain state rather than the row.
                        args[1] = null
                        bindings[row] = Binding(method, args, kind)
                    }
                    result
                }
            }
        }.onFailure { Xp.w("MCNativeMaterial: native binder unavailable: $it") }
    }

    private fun palette(view: View): List<NotificationBlendColor> =
        (readColors!!.invoke(view) as List<*>).map { point ->
            point as Point
            NotificationBlendColor(point.x, point.y)
        }

    /** Fade only this transition's current native recipe, including updates made mid-flight. */
    fun fadeBlend(view: View, amount: Float) {
        val lease = fades[view] ?: return
        if (!amount.isFinite()) return
        runCatching {
            lease.set(fadeNotificationPalette(lease.baseline, amount))
        }.onFailure { Xp.w("MCNativeMaterial: blend fade failed: $it") }
    }

    fun begin(row: View, background: View) {
        dirty[row] = true
        if (readColors == null || writeColors == null || fades.containsKey(background)) return
        runCatching {
            fades[background] = NotificationPropertyLease(
                { palette(background) },
                { colors ->
                    writeColors!!.invoke(background, ArrayList(colors.map { Point(it.color, it.mode) }))
                    Unit
                },
                { it.toList() })
        }.onFailure { Xp.w("MCNativeMaterial: blend lease unavailable: $it") }
    }

    /** Remove only our last fade before handing control back, even if the AOD binder defers. */
    fun end(background: View) {
        val lease = fades.remove(background) ?: return
        runCatching { lease.restore() }.onFailure { Xp.w("MCNativeMaterial: blend handback failed: $it") }
    }

    /** Re-evaluate native keyguard/night/glass state only at ownership boundaries. */
    fun restore(row: View): Boolean {
        // Consume before invoking native code: an OEM callback or a second cleanup must not
        // refresh this boundary twice. Unsupported/detached/AOD rows have no queued retry.
        if (dirty.remove(row) != true) return false
        if (refreshing || !row.isAttachedToWindow) return false
        val binding = bindings[row] ?: return false
        // The direct binder reads shouldApplyFullAodEffect itself and defers while the OEM
        // AOD animator owns the row. Preserve the keyed variant's existing AOD boundary.
        if (MiniPlayerScene.aodActive && binding.kind == NotificationMaterialBinding.KEYED) return false
        val key = if (binding.kind == NotificationMaterialBinding.KEYED) binding.args[2] ?: return false else null
        val oldKey = key?.let { runCatching { Xp.getObjectField(it, "element") }.getOrNull() }
        refreshing = true
        return try {
            // Clear only the binder's EffectKey cache. Its normal update then computes
            // the appropriate material from the current platform flows and theme context.
            if (key != null) Xp.setObjectField(key, "element", null)
            val args = binding.args.copyOf().also { it[1] = row }
            binding.method.invoke(null, *args)
            binding.refreshes++
            true
        } catch (error: Throwable) {
            // A partially completed binder may already have installed a newer cache entry.
            runCatching {
                if (key != null && Xp.getObjectField(key, "element") == null) Xp.setObjectField(key, "element", oldKey)
            }
            Xp.w("MCNativeMaterial: native row rebind failed: $error")
            false
        } finally { refreshing = false }
    }

    fun describe(row: View): String {
        val binding = bindings[row]
        val keyguard = runCatching {
            val interactor = binding?.args?.get(0) ?: return@runCatching null
            val lazy = Xp.getObjectField(interactor, "materialViewState\$delegate")
            val flow = Xp.callMethod(lazy, "getValue")
            val state = Xp.callMethod(flow, "getValue")
            Xp.getObjectField(state, "isShowingKeyguardNotification")
        }.getOrNull()
        val bg = runCatching { Xp.getObjectField(row, "mBackgroundNormal") as? View }.getOrNull()
        return "refresh=${binding?.refreshes}/kind=${binding?.kind}/keyguard=$keyguard" +
            "/blend=${bg?.let { runCatching { palette(it).size }.getOrNull() }}/held=${bg?.let { fades.containsKey(it) }}"
    }
}
