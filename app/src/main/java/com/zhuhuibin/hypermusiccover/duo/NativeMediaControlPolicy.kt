package com.zhuhuibin.hypermusiccover.duo

/** Native media gestures keep their DOWN hit; the whole media card is not a control. */
internal object NativeMediaControlPolicy {
    private val buttons = setOf("actionPlayPause", "actionPrev", "actionNext", "media_play_pause",
        "media_output_switcher", "media_seamless", "cover_source")
    private val action = Regex("action[0-4]")
    @JvmStatic fun effectiveAlpha(parentAlpha: Float, alpha: Float, transitionAlpha: Float): Float =
        if (!parentAlpha.isFinite() || !alpha.isFinite() || !transitionAlpha.isFinite()) 0f
        else parentAlpha.coerceIn(0f, 1f) * alpha.coerceIn(0f, 1f) * transitionAlpha.coerceIn(0f, 1f)
    fun interactive(shown: Boolean, enabled: Boolean, alpha: Float, width: Int, height: Int): Boolean =
        shown && enabled && alpha.isFinite() && alpha > .01f && width > 0 && height > 0
    fun owns(name: String?, seekBar: Boolean, clickable: Boolean, boundControl: Boolean = false): Boolean = seekBar ||
        name == "media_progress_bar" || clickable && (boundControl || name in buttons || name?.matches(action) == true)
}
