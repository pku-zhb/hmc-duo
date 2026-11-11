package com.zhuhuibin.hypermusiccover.duo

/** Each presentation applies its initial choice once; waking AOD never resets live choices. */
internal class LockscreenEntryState {
    private val notifications = mutableSetOf<Boolean>()
    private val music = mutableSetOf<Boolean>()
    private val focus = mutableSetOf<Pair<Boolean, String>>()
    private val selections = mutableSetOf<Boolean>()
    private val snapshots = mutableMapOf<Boolean, LockscreenSnapshot?>()
    fun arm() { notifications.clear(); music.clear(); focus.clear(); selections.clear(); snapshots.clear() }
    fun takeNotifications(ready: Boolean, landscape: Boolean = false): Boolean = ready && notifications.add(landscape)
    fun takeMusic(ready: Boolean, landscape: Boolean = false): Boolean = ready && music.add(landscape)
    fun takeFocus(key: String, landscape: Boolean = false): Boolean = focus.add(landscape to key)
    fun takeSelection(ready: Boolean, landscape: Boolean): Boolean = ready && selections.add(landscape)
    fun snapshot(landscape: Boolean, load: () -> LockscreenSnapshot?): LockscreenSnapshot? {
        if (!snapshots.containsKey(landscape)) snapshots[landscape] = load()
        return snapshots[landscape]
    }
}
