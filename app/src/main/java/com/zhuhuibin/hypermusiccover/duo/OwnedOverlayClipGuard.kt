package com.zhuhuibin.hypermusiccover.duo

import android.graphics.Rect
import android.view.View
import java.util.WeakHashMap

/** Hide owned overlay pixels without changing animation, layout or presentation state. */
internal class OwnedOverlayClipGuard {
    private val leases = OwnedOverlayClipLeases<View, Rect>(
        hidden = Rect(-1, -1, -1, -1),
        read = { it.clipBounds },
        write = { view, clip -> view.clipBounds = clip },
        copy = { Rect(it) },
    )

    val hasOverrides: Boolean get() = leases.hasOverrides
    fun suppress(view: View) = leases.suppress(view)
    fun restoreAll() = leases.restoreAll()
}

/** Clip-only ports keep the lease testable and cannot write a view's alpha or visibility. */
internal class OwnedOverlayClipLeases<T : Any, C : Any>(
    private val hidden: C,
    private val read: (T) -> C?,
    private val write: (T, C?) -> Unit,
    private val copy: (C) -> C,
) {
    private class Lease<C>(var original: C?)
    // Values contain only copied clips: neither they nor the ports retain target views.
    private val held = WeakHashMap<T, Lease<C>>()
    val hasOverrides: Boolean get() = held.isNotEmpty()

    fun suppress(target: T) {
        val current = read(target)
        val lease = held[target]
        if (lease == null) held[target] = Lease(current?.let(copy))
        else if (current != hidden) lease.original = current?.let(copy)
        if (current != hidden) write(target, copy(hidden))
    }

    fun restoreAll() {
        val snapshot = held.entries.map { it.key to it.value.original }
        held.clear()
        for ((target, original) in snapshot) {
            // A newer owner may already have replaced our clip; never overwrite that value.
            if (read(target) == hidden) write(target, original?.let(copy))
        }
    }
}
