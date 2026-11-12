package com.zhuhuibin.hypermusiccover.duo

/** One temporary notification property; a newer native write always wins at handback. */
internal class NotificationPropertyLease<T>(
    private val read: () -> T,
    private val write: (T) -> Unit,
    private val copy: (T) -> T = { it },
) {
    private var original = copy(read())
    private var last = copy(original)
    private var owned = false

    val baseline: T
        get() {
            observe(read())
            return copy(original)
        }

    private fun observe(current: T) {
        if (!owned || current != last) original = copy(current)
    }

    fun set(value: T) {
        val current = read()
        observe(current)
        if (current == value) return
        write(copy(value))
        // A throwing setter did not establish ownership. Copies keep mutable clips/matrices
        // independent of both the caller's next frame and the view's native implementation.
        last = copy(value)
        owned = true
    }

    fun restore() {
        if (!owned) return
        owned = false
        if (read() == last && original != last) write(copy(original))
    }
}
