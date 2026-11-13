package com.zhuhuibin.hypermusiccover.duo

/** Temporary navigation choices never become last-lockscreen preferences. */
internal class NavigationPresentationSession {
    val entry = LockscreenEntryState()
    private val saved = mutableMapOf<Boolean, LockscreenSnapshot>()
    private var restoring = false
    private var locked = false
    private var asleep = false
    var active = false
        private set

    /** Returns true once at the end so the normal entry gates can replay the saved choices. */
    fun observe(navigation: Boolean, keyguard: Boolean, aod: Boolean): Boolean {
        val ended = active && !navigation
        if (!active && navigation) {
            saved.clear()
            restoring = false
            entry.arm()
        }
        if (ended) restoring = saved.isNotEmpty()
        if (navigation && keyguard && (!locked || aod && !asleep)) entry.arm()
        active = navigation
        locked = keyguard
        asleep = aod
        return ended
    }

    fun remember(landscape: Boolean, capture: () -> LockscreenSnapshot) {
        if (active && !saved.containsKey(landscape)) saved[landscape] = capture()
    }

    fun restore(landscape: Boolean): LockscreenSnapshot? =
        if (restoring) saved.remove(landscape) else null

    fun sleepEntry() { if (active) entry.arm() }
}
