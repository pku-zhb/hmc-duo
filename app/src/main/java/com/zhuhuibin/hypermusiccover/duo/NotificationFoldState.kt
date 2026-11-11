package com.zhuhuibin.hypermusiccover.duo

/** User presentation survives the native stack's temporary AOD NUMBER state. */
internal class NotificationFoldState {
    private val choices = mutableMapOf<Boolean, Boolean>()
    private var landscape = false
    var collapsed = true
        private set

    /** Keep the current wake target until the next lock entry has supplied its own choice. */
    fun beginSession() { choices.clear() }

    fun seed(innerLandscape: Boolean, entryCollapsed: Boolean): Boolean {
        landscape = innerLandscape
        val next = choices.getOrPut(innerLandscape) { entryCollapsed }
        val changed = next != collapsed
        collapsed = next
        return changed
    }

    fun choose(folded: Boolean): Boolean {
        choices[landscape] = folded
        val changed = folded != collapsed
        collapsed = folded
        return changed
    }

    fun wakeState(): String = if (collapsed) "NUMBER" else "LIST"
    fun autoFold(enabled: Boolean): Boolean = enabled && collapsed
}
