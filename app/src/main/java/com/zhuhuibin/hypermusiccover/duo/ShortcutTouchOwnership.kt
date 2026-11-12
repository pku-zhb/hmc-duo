package com.zhuhuibin.hypermusiccover.duo

/** The shortcut may only acquire a stream that started on its displayed icon. */
internal class ShortcutTouchOwnership {
    private var downTime = Long.MIN_VALUE
    private var shortcut = true

    fun start(time: Long, hit: Boolean) { downTime = time; shortcut = hit }
    fun allows(time: Long): Boolean = time != downTime || shortcut
}
