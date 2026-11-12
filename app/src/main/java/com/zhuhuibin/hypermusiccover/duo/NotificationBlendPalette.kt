package com.zhuhuibin.hypermusiccover.duo

import kotlin.math.roundToInt

/** Immutable copies of the OEM Point(color, blendMode) recipe, including native alpha. */
internal data class NotificationBlendColor(val color: Int, val mode: Int)

internal fun fadeNotificationPalette(colors: List<NotificationBlendColor>, amount: Float): List<NotificationBlendColor> {
    val factor = amount.coerceIn(0f, 1f)
    return colors.map {
        val alpha = ((it.color ushr 24) * factor).roundToInt()
        NotificationBlendColor((it.color and 0x00ffffff) or (alpha shl 24), it.mode)
    }
}
