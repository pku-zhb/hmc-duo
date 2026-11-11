package com.zhuhuibin.hypermusiccover.duo

/** Restore an off-screen card only after native visibility has been handed to suppression. */
internal object RailExitHandoff {
    interface Target {
        fun hideNative()
        fun selectMini()
        fun restorePose()
    }
    fun complete(returnToMini: Boolean, target: Target) {
        if (returnToMini) {
            target.hideNative()
            target.selectMini()
        }
        target.restorePose()
    }
}
