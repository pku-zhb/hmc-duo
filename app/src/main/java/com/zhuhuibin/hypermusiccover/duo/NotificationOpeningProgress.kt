package com.zhuhuibin.hypermusiccover.duo

/** Keep the visual handoff on native progress while exposure retains a retryable owner. */
internal class NotificationOpeningProgress {
    private var initialFade: Float? = null
    private var from: Int? = null
    private var to: Int? = null
    private var progress = 0f

    fun begin(fade: Float?, position: Int?, target: Int?) {
        initialFade = fade?.takeIf { it.isFinite() }?.coerceIn(0f, 1f)
        from = position
        to = target
        progress = 0f
    }

    fun retarget(target: Int?) { if (target != null) to = target }

    fun alpha(fade: Float?, position: Int?): Float {
        val startFade = initialFade
        val fadeProgress = if (startFade != null && startFade > .001f && fade?.isFinite() == true)
            ((startFade - fade) / startFade).coerceIn(0f, 1f) else 0f
        val start = from
        val target = to
        val scrollProgress = if (start != null && target != null && start != target && position != null)
            ((position.toFloat() - start) / (target.toFloat() - start)).coerceIn(0f, 1f) else 0f
        progress = maxOf(progress, fadeProgress, scrollProgress)
        return 1f - progress
    }
}
