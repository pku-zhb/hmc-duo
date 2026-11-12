package com.zhuhuibin.hypermusiccover.duo

/** Keep a rail representative until native rows are a usable list, not merely a scroll target. */
internal class NotificationListExposure {
    enum class Action { NONE, OPENED, CLICK_LIST, RETRY_LIST, CAPTURE }
    private var requestedAt: Long? = null
    private var clicked = false
    private var retries = 0
    private var pileSince: Long? = null
    var captured = false
        private set
    val opening get() = requestedAt != null
    val holdsIsland get() = opening || captured

    fun reset() {
        requestedAt = null; clicked = false; retries = 0; pileSince = null; captured = false
    }
    fun request(now: Long) { reset(); requestedAt = now }
    fun cancelOpen() { requestedAt = null; pileSince = null }

    fun observe(now: Long, active: Boolean, busy: Boolean, state: String?,
                wantsList: Boolean?, rowsVisible: Boolean, collapsed: Boolean): Action {
        if (!active) { reset(); return Action.NONE }
        if (isList(state, wantsList, rowsVisible)) {
            pileSince = null
            if (opening) { reset(); return Action.OPENED }
            if (!collapsed && !busy) captured = false
            return Action.NONE
        }
        requestedAt?.let { since ->
            if (now - since >= 2400L) {
                cancelOpen(); captured = true
                return Action.CAPTURE
            }
            if (!busy && state == "STACK" && wantsList != false && !clicked) {
                clicked = true
                return Action.CLICK_LIST
            }
            if (!busy && retries < 2 && now - since >= 650L * (retries + 1)) {
                retries++
                return Action.RETRY_LIST
            }
            return Action.NONE
        }
        // NUMBER during waking is deliberately ignored: it is not a new user fold choice.
        // A settled partial pile, however, cannot stand for an expanded notification list.
        if (!busy && state == "STACK" && (wantsList == true || !rowsVisible)) {
            val since = pileSince ?: now.also { pileSince = it }
            if (!captured && now - since >= 400L) {
                captured = true
                return Action.CAPTURE
            }
        } else pileSince = null
        return Action.NONE
    }

    companion object {
        fun isList(state: String?, wantsList: Boolean?, rowsVisible: Boolean): Boolean =
            rowsVisible && (state == "LIST" || state == "STACK" && wantsList == false)
    }
}
