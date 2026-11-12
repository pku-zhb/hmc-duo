package com.zhuhuibin.hypermusiccover.duo

/** Native folding may finish in one traversal; the new bubble still needs a visible birth. */
internal class NotificationReturnProgress(private val initialFade: Float?, private val minimumMs: Long = 240L) {
    var progress = 0f
        private set
    var completed = false
        private set

    fun update(elapsedMs: Long, fade: Float?, scroll: Float?, busy: Boolean, folded: Boolean): Float {
        val nativeDone = !busy && folded && elapsedMs >= 32L
        val normalizedFade = fade?.takeIf { it.isFinite() }?.let {
            val baseline = initialFade?.takeIf { value -> value.isFinite() } ?: 0f
            if (baseline >= .999f) null else ((it - baseline) / (1f - baseline)).coerceIn(0f, 1f)
        }
        val nativeProgress = if (nativeDone) 1f else
            maxOf(normalizedFade ?: 0f, scroll?.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f)
        // Native motion remains the source. Only an instantaneous/very fast completion is
        // spread over this short visible interval; normal longer folds are not retimed.
        val visibleLimit = RailTransitionTiming.birthLimit(elapsedMs, minimumMs)
        progress = maxOf(progress, minOf(nativeProgress, visibleLimit))
        completed = nativeDone && progress >= .999f
        return progress
    }
}
