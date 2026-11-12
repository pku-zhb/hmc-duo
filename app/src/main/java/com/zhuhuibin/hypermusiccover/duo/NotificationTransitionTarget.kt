package com.zhuhuibin.hypermusiccover.duo

import android.view.View
import java.lang.reflect.Method

/** Ordinary lock-screen rows only; an unknown OEM role is not permission to take the row. */
internal object NotificationTransitionTarget {
    private class Access(type: Class<*>) {
        val headsUp = method(type, "isHeadsUpState", "isHeadsUp")
        val pinned = method(type, "isPinned")
        val leaving = method(type, "isHeadsUpAnimatingAway\$1", "isHeadsUpAnimatingAway")
    }
    private val access = HashMap<Class<*>, Access>()

    private fun method(type: Class<*>, vararg names: String): Method? = names.firstNotNullOfOrNull {
        runCatching { Xp.findMethodExact(type, it) }.getOrNull()
    }

    fun ordinary(row: View): Boolean {
        val a = access.getOrPut(row.javaClass) { Access(row.javaClass) }
        return NotificationTransitionPolicy.ordinary(
            runCatching { a.headsUp?.invoke(row) as? Boolean }.getOrNull(),
            runCatching { a.pinned?.invoke(row) as? Boolean }.getOrNull(),
            runCatching { a.leaving?.invoke(row) as? Boolean }.getOrNull())
    }

    fun sceneEligible(): Boolean = NotificationTransitionPolicy.scene(
        Main.keyguardLocked(), MiniPlayerScene.keyguardGoingAway,
        MiniPlayerScene.aodActive, Main.screenOnCached()) && !Main.bouncerShown()

    fun eligible(row: View): Boolean = row.isAttachedToWindow && sceneEligible() && ordinary(row)
}

internal object NotificationTransitionPolicy {
    fun ordinary(headsUp: Boolean?, pinned: Boolean?, leaving: Boolean?): Boolean =
        headsUp == false && pinned == false && leaving == false

    fun scene(locked: Boolean, goingAway: Boolean, aod: Boolean, screenOn: Boolean): Boolean =
        locked && !goingAway && !aod && screenOn
}
