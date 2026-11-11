package com.zhuhuibin.hypermusiccover.duo

import android.graphics.Canvas
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.widget.TextView
import kotlin.math.ceil
import kotlin.math.min

/** Cache measured runs; preserve Android shaping for sideways Latin/RTL words. */
internal class VerticalTextRenderer {
    private data class Key(val text: String, val w: Int, val h: Int, val size: Float,
        val typeface: Typeface?, val spacing: Float)
    private data class DrawRun(val run: VerticalPillText.Run, val horizontal: StaticLayout?)
    private var key: Key? = null
    private var runs = emptyList<DrawRun>()
    private val paint = TextPaint()
    private var cell = 0f

    fun draw(canvas: Canvas, view: TextView) {
        val w = view.width - view.paddingLeft - view.paddingRight
        val h = view.height - view.paddingTop - view.paddingBottom
        if (w <= 0 || h <= 0) return
        val next = Key(view.text.toString(), w, h, view.textSize, view.typeface, view.letterSpacing)
        if (key != next) {
            key = next
            paint.set(view.paint)
            // A narrow capsule must not clip the user's scaled glyphs into its adjacent column.
            paint.textSize = min(view.textSize, w * .86f)
            val metrics = paint.fontMetrics
            cell = ceil(metrics.descent - metrics.ascent)
            runs = VerticalPillText.layout(next.text, cell, h.toFloat()) { paint.measureText(it).toDouble() }
                .map { run ->
                    val horizontal = if (run.orientation == VerticalPillText.Orientation.SIDEWAYS)
                        StaticLayout.Builder.obtain(run.text, 0, run.text.length, paint,
                            ceil(run.advance).toInt().coerceAtLeast(1))
                            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR)
                            .setIncludePad(false).setMaxLines(1).build()
                    else null
                    DrawRun(run, horizontal)
                }
        }
        paint.color = view.currentTextColor
        canvas.save()
        canvas.clipRect(view.paddingLeft, view.paddingTop, view.width-view.paddingRight, view.height-view.paddingBottom)
        canvas.translate(view.paddingLeft.toFloat(), view.paddingTop.toFloat())
        var y = 0f
        for ((run, horizontal) in runs) {
            canvas.save()
            if (horizontal != null) {
                val crossScale = min(1f, w / horizontal.height.toFloat().coerceAtLeast(1f))
                canvas.translate((w + horizontal.height * crossScale) / 2f, y)
                canvas.rotate(90f)
                canvas.scale(1f, crossScale)
                horizontal.draw(canvas)
            } else {
                val scale = min(1f, w / paint.measureText(run.text).coerceAtLeast(1f))
                canvas.translate(w / 2f, y + cell / 2f)
                canvas.scale(scale, 1f)
                val metrics = paint.fontMetrics
                canvas.drawText(run.text, -paint.measureText(run.text) / 2f,
                    -(metrics.ascent + metrics.descent) / 2f, paint)
            }
            canvas.restore()
            y += run.advance
        }
        canvas.restore()
    }
}
