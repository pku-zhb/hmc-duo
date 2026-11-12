package com.zhuhuibin.hypermusiccover.duo

/** Restore only a value we actually wrote and still own, never a construction-time snapshot. */
internal class MediaPropertyLease<T>(private val same: (T, T) -> Boolean = { a, b -> a == b }) {
    private var original: T? = null
    private var written: T? = null
    var held = false
        private set

    @Suppress("UNCHECKED_CAST")
    fun owns(current: T): Boolean = held && same(current, written as T)

    @Suppress("UNCHECKED_CAST")
    fun nativeValue(current: T): T = if (owns(current)) original as T else current

    fun write(current: T, next: T, apply: (T) -> Unit): Boolean {
        if (same(current, next)) return false
        val baseline = nativeValue(current)
        apply(next)
        original = baseline
        written = next
        held = true
        return true
    }

    /** Forget the lease even if another writer won; returning true means a handoff occurred. */
    fun release(current: T, apply: (T) -> Unit): Boolean {
        if (!held) return false
        val restore = owns(current)
        val value = nativeValue(current)
        held = false
        original = null
        written = null
        if (restore && !same(current, value)) apply(value)
        return true
    }
}

/** A worker may finish after a settings change, detach, or a newer artwork request. */
internal class MediaRenderGeneration {
    @Volatile private var generation = 0L
    fun invalidate(): Long { generation++; return generation }
    fun accepts(token: Long): Boolean = token == generation
}

/** A detached child coalesces handoffs; attaching consumes one current-state recalculation. */
internal class MediaRebindRequest {
    var pending = false
        private set
    fun defer() { pending = true }
    fun take(attached: Boolean): Boolean {
        if (!attached || !pending) return false
        pending = false
        return true
    }
    fun clear() { pending = false }
}
