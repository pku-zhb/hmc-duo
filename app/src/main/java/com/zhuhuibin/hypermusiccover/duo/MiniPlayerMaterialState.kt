package com.zhuhuibin.hypermusiccover.duo

/**
 * Values to compare one card dressing against the next by. Arrays are copied into lists: the
 * OEM hands the same array to the next dressing, changed, and the previous recipe held by
 * reference would change with it and always read as equal. From TakeKazeX's PR #15.
 */
internal object MiniPlayerMaterialState {
    /** Native glass needs both the element and its attached blur container to have bounds. */
    class LayoutGate {
        var appliedGeneration: Int? = null
            private set
        var attemptedGeneration: Int? = null
            private set
        var attempts = 0
            private set
        private var retryAt = 0L

        fun needsApply(generation: Int, attached: Boolean, frameWidth: Int, frameHeight: Int,
                       elementWidth: Int, elementHeight: Int, nowMs: Long = 0L): Boolean =
            attached && frameWidth > 1 && frameHeight > 1 &&
                elementWidth > 1 && elementHeight > 1 && appliedGeneration != generation &&
                (attemptedGeneration != generation || attempts < MAX_ATTEMPTS && nowMs >= retryAt)

        fun completed(generation: Int, success: Boolean, nowMs: Long) {
            if (attemptedGeneration != generation) attempts = 0
            attemptedGeneration = generation
            attempts++
            appliedGeneration = if (success) generation else null
            retryAt = if (success) 0L else nowMs + RETRY_DELAY_MS
        }

        fun retryDelay(generation: Int, nowMs: Long): Long? =
            if (attemptedGeneration == generation && appliedGeneration != generation && attempts < MAX_ATTEMPTS)
                (retryAt - nowMs).coerceAtLeast(0L) else null

        fun status(generation: Int): String = when {
            appliedGeneration == generation -> "native"
            attemptedGeneration != generation -> "pending"
            attempts < MAX_ATTEMPTS -> "fallback-retry"
            else -> "fallback-final"
        }

        /** Reparenting creates another native rendering attachment, even for the same recipe. */
        fun detached() {
            appliedGeneration = null; attemptedGeneration = null; attempts = 0; retryAt = 0L
        }

        companion object {
            private const val MAX_ATTEMPTS = 3
            private const val RETRY_DELAY_MS = 150L
        }
    }

    fun snapshot(value: Any?): Any? = when (value) {
        is IntArray -> value.toList()
        is FloatArray -> value.toList()
        is LongArray -> value.toList()
        is DoubleArray -> value.toList()
        is BooleanArray -> value.toList()
        is ByteArray -> value.toList()
        is ShortArray -> value.toList()
        is CharArray -> value.toList()
        is Array<*> -> value.map(::snapshot)
        is List<*> -> value.map(::snapshot)
        else -> value
    }

    /** Custom blur owns its layer; changing the OEM glass style only triggers a redress. */
    fun styleKey(effect: String, customBlur: Boolean, generation: Int): String =
        "${if (customBlur) "customBlur" else "$effect:native"}#$generation"

    /** Another effect, not only new values for the same one: keys are "effect#generation". */
    fun replacesLayer(previous: String?, next: String): Boolean =
        previous != null && previous.substringBefore('#') != next.substringBefore('#')
}
