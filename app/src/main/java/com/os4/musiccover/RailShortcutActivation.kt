package com.os4.musiccover

/** The OEM's release branch requires a fully initialized native shortcut before activation. */
internal object RailShortcutActivation {
    interface Target {
        fun initialize(left: Boolean)
        fun activate()
    }
    fun prepare(state: RailShortcutTapState, side: Int, now: Long, eligible: Boolean,
                alreadyActive: Boolean, target: Target): Boolean {
        if (!eligible || alreadyActive || !state.claim(side, now)) return false
        target.initialize(side == 0)
        target.activate()
        return true
    }
}
