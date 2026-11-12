package com.zhuhuibin.hypermusiccover.duo

/** A small pressure release at the old seat; opening the native card never waits for it. */
internal object RailExpansionPopMotion {
    const val DURATION_MS = 260L

    data class Frame(val scale: Float, val alpha: Float)

    fun frame(progress: Float): Frame {
        val p = if (progress.isFinite()) progress.coerceIn(0f, 1f) else 1f
        val swell = smooth((p / .55f).coerceIn(0f, 1f))
        val fade = smooth(((p - .10f) / .90f).coerceIn(0f, 1f))
        return Frame(1f + .075f * swell, 1f - fade)
    }

    private fun smooth(value: Float) = value * value * (3f - 2f * value)
}
