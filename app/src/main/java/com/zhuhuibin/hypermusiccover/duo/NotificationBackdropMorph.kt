// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Lewewe (HyperMusicCover-Enhanced, https://github.com/Lewewe/HyperMusicCover-Enhanced)
// Developed with the assistance of OpenAI Codex.
package com.zhuhuibin.hypermusiccover.duo

import android.graphics.Outline
import android.graphics.Rect
import android.view.View
import android.view.ViewOutlineProvider
import kotlin.math.roundToInt

/** Own the material element's window too: vendor blur does not follow every ancestor clip. */
internal class NotificationBackdropMorph {
    private inner class State(val row: View, val view: View) : View.OnAttachStateChangeListener {
        val alpha = NotificationPropertyLease({ view.alpha }, { view.alpha = it })
        val overlap = NotificationPropertyLease({ view.getHasOverlappingRendering() },
            { view.forceHasOverlappingRendering(it) })
        val clip = NotificationPropertyLease({ view.clipToOutline }, { view.clipToOutline = it })
        val outline = NotificationPropertyLease({ view.outlineProvider }, { view.outlineProvider = it })
        val bounds = NotificationPropertyLease({ view.clipBounds }, { view.clipBounds = it },
            { it?.let(::Rect) })
        // An unknown getter is not a baseline of 255. Do not borrow a property we cannot restore.
        val drawableAlpha = runCatching {
            val setter = Xp.findMethodExact(view.javaClass, "setDrawableAlpha", Int::class.javaPrimitiveType!!)
            NotificationPropertyLease(
                { (Xp.getObjectField(view, "mDrawableAlpha") as Number).toInt() },
                { setter.invoke(view, it); Unit })
        }.getOrNull()
        val window = Rect()
        var radius = -1f
        val provider = object : ViewOutlineProvider() {
            override fun getOutline(v: View, out: Outline) { out.setRoundRect(window, radius.coerceAtLeast(0f)) }
        }
        init {
            row.addOnAttachStateChangeListener(this)
            view.addOnAttachStateChangeListener(this)
            NativeNotificationMaterial.begin(row, view)
        }

        fun restore() {
            row.removeOnAttachStateChangeListener(this)
            view.removeOnAttachStateChangeListener(this)
            NativeNotificationMaterial.end(view)
            // Each property has independent ownership: replacing the outline does not grant
            // permission to overwrite a new clip, alpha, or drawable alpha.
            listOf(overlap, alpha, drawableAlpha, outline, clip, bounds).forEach { property ->
                runCatching { property?.restore() }
                    .onFailure { Xp.w("MCNotif: backdrop handback failed: $it") }
            }
        }

        override fun onViewAttachedToWindow(v: View) = Unit
        override fun onViewDetachedFromWindow(v: View) { release(row) }
    }
    private val states = HashMap<View, State>()

    fun apply(row: View, left: Float, top: Float, width: Float, height: Float, radius: Float, alpha: Float) {
        if (!NotificationTransitionTarget.eligible(row)) { release(row); return }
        val current = runCatching { Xp.getObjectField(row, "mBackgroundNormal") as? View }.getOrNull()
        if (states[row]?.view !== current) release(row)
        val bg = current?.takeIf { it.isAttachedToWindow } ?: return
        val state = states.getOrPut(row) { State(row, bg) }
        state.overlap.set(false)
        val l = (left - bg.left).roundToInt()
        val t = (top - bg.top).roundToInt()
        val r = l + width.roundToInt().coerceAtLeast(1)
        val b = t + height.roundToInt().coerceAtLeast(1)
        val corner = radius.coerceAtLeast(0f)
        val changed = state.window.left != l || state.window.top != t ||
            state.window.right != r || state.window.bottom != b || state.radius != corner
        state.window.set(l, t, r, b)
        state.radius = corner
        state.outline.set(state.provider)
        state.clip.set(true)
        state.bounds.set(state.window)
        if (changed) bg.invalidateOutline()
        val a = alpha.coerceIn(0f, 1f)
        // NUMBER can have the background at alpha zero already. That is a native state to
        // restore, not the opacity of the fully opened material we are morphing toward.
        state.alpha.set(a)
        NativeNotificationMaterial.fadeBlend(bg, a)
        val drawableAlpha = (255 * a).roundToInt()
        runCatching { state.drawableAlpha?.set(drawableAlpha) }
    }

    private fun release(row: View, rebind: Boolean = true) {
        val state = states.remove(row) ?: return
        state.restore()
        if (rebind) NativeNotificationMaterial.restore(row)
    }

    /** Let the caller restore its row geometry before the one current-material recomputation. */
    fun clear(rebind: Boolean = true): Set<View> {
        val rows = states.keys.toSet()
        rows.forEach { release(it, rebind) }
        states.clear()
        return rows
    }
}
