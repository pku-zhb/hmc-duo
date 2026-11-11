package com.zhuhuibin.hypermusiccover.duo

/** Two independent OEM filters; neither replaces the lock-screen privacy provider. */
internal data class LockscreenNotificationPolicy(val showAll: Boolean = false, val retain: Boolean = false) {
    fun canShowOnKeyguard(original: Boolean): Boolean = showAll || original
    fun shownAfterUnlock(original: Boolean): Boolean = original && !retain
}
