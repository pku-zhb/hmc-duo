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
    /** An explicit action wins over initial categories that have not finished binding yet. */
    fun finishPresentation(landscape: Boolean, knownFocus: Collection<String>) {
        music.add(landscape)
        notifications.add(landscape)
        selections.add(landscape)
        knownFocus.forEach { focus.add(landscape to it) }
    }
    fun snapshot(landscape: Boolean, load: () -> LockscreenSnapshot?): LockscreenSnapshot? {
        if (!snapshots.containsKey(landscape)) snapshots[landscape] = load()
        return snapshots[landscape]
    }
}
