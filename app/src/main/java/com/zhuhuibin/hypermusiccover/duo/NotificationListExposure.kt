package com.zhuhuibin.hypermusiccover.duo

/** Keep a rail representative until native rows are a usable list, not merely a scroll target. */
internal class NotificationListExposure {
    enum class Action { NONE, OPENED, RETARGET_LIST, RETRY_LIST, CAPTURE, RETAIN }
    private var requestedAt: Long? = null
    private var retargeted = false
    private var retries = 0
    private var pileSince: Long? = null
    private var visibleSince: Long? = null
    var captured = false
        private set
    val opening get() = requestedAt != null
    val holdsIsland get() = opening || captured

    /** Gesture routing must keep the island until exposure has settled, even on a visible frame. */
    fun listPresented(state: String?, wantsList: Boolean?, rowsVisible: Boolean): Boolean =
        !holdsIsland && isList(state, wantsList, rowsVisible)

    fun reset() {
        requestedAt = null; retargeted = false; retries = 0
        pileSince = null; visibleSince = null; captured = false
    }
    fun request(now: Long) { reset(); requestedAt = now }
    fun cancelOpen() { requestedAt = null; pileSince = null; visibleSince = null }
    fun failedOpen() { cancelOpen(); captured = true }

    fun observe(now: Long, active: Boolean, busy: Boolean, state: String?,
                wantsList: Boolean?, rowsVisible: Boolean, collapsed: Boolean): Action {
        if (!active) { reset(); return Action.NONE }
        val listVisible = isList(state, wantsList, rowsVisible)
        // The deadline applies even if native scrolling stays busy on an otherwise visible
        // LIST. Keep a retryable representative without forcing those visible rows away.
        requestedAt?.let { since ->
            if (now - since >= 2400L) {
                failedOpen()
                return if (listVisible) Action.RETAIN else Action.CAPTURE
            }
        }
        if (listVisible) {
            pileSince = null
            if (opening) {
                // Focus/media rows can join the stack after the first list frame. Do not retire
                // the bubble on that transient frame or during an unfinished native scroll.
                if (busy) visibleSince = null
                else {
                    val since = visibleSince ?: now.also { visibleSince = it }
                    if (now - since >= 128L) { reset(); return Action.OPENED }
                }
                return Action.NONE
            }
            if (!collapsed && !busy) captured = false
            return Action.NONE
        }
        visibleSince = null
        requestedAt?.let { since ->
            // SHOW_LIST may be NUMBER as well as STACK when focus rows consume the viewport.
            // Recompute the explicit opening target after those rows finish their layout.
            if (!busy && (state == "NUMBER" || state == "STACK") && !retargeted && now - since >= 160L) {
                retargeted = true
                return Action.RETARGET_LIST
            }
            if (!busy && retries < 2 && now - since >= 650L * (retries + 1)) {
                retries++
                return Action.RETRY_LIST
            }
            return Action.NONE
        }
        // NUMBER during waking is deliberately ignored: it is not a new user fold choice.
        // A settled partial pile, however, cannot stand for an expanded notification list.
        if (!busy && state == "STACK" && (wantsList != false || !rowsVisible)) {
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
