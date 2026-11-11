package com.os4.musiccover

/** Each presentation applies its initial choice once; waking AOD never resets live choices. */
internal class LockscreenEntryState {
    private val notifications = mutableSetOf<Boolean>()
    private val music = mutableSetOf<Boolean>()
    fun arm() { notifications.clear(); music.clear() }
    fun takeNotifications(ready: Boolean, landscape: Boolean = false): Boolean = ready && notifications.add(landscape)
    fun takeMusic(ready: Boolean, landscape: Boolean = false): Boolean = ready && music.add(landscape)
}
