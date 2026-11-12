package com.zhuhuibin.hypermusiccover.duo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.text.format.DateFormat
import android.view.MotionEvent
import android.view.View
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin

/** Display-only clock, battery arc and network cluster, drawn in the native keyguard overlay. */
internal class RailStatusView(context: Context) : View(context) {
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.create("sans-serif", Typeface.NORMAL), 600, false)
        setShadowLayer(1.8f, 0f, 0f, 0x66000000)
    }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
        setShadowLayer(1.8f, 0f, 0f, 0x44000000)
    }
    private val shape = Paint(Paint.ANTI_ALIAS_FLAG)
    private val plane = Path().apply {
        moveTo(19f, 0f); lineTo(4f, -3f); lineTo(-6f, -16f); lineTo(-9f, -16f)
        lineTo(-4f, -3f); lineTo(-12f, -3f); lineTo(-16f, -8f); lineTo(-18f, -8f)
        lineTo(-16f, 0f); lineTo(-18f, 8f); lineTo(-16f, 8f); lineTo(-12f, 3f)
        lineTo(-4f, 3f); lineTo(-9f, 16f); lineTo(-6f, 16f); lineTo(4f, 3f); close()
    }
    private val wifiDot = Path().apply {
        moveTo(-7f, 10.3f); quadTo(0f, 5.1f, 7f, 10.3f)
        quadTo(8.7f, 11.9f, 7f, 13.9f); lineTo(2.6f, 18.1f)
        quadTo(0f, 20.5f, -2.6f, 18.1f); lineTo(-7f, 13.9f)
        quadTo(-8.7f, 11.9f, -7f, 10.3f); close()
    }
    private var status = RailStatusSnapshot()
    private var time = ""
    private var timeBucket = Long.MIN_VALUE
    private val source = RailStatusSource(context) { next ->
        val changed = status != next
        status = next
        refreshTime(force = true)
        if (changed) invalidate()
    }
    private var listening = false

    init {
        isClickable = false; isLongClickable = false; isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        accessibilityLiveRegion = ACCESSIBILITY_LIVE_REGION_NONE
    }
    override fun onTouchEvent(event: MotionEvent): Boolean = false
    override fun onDetachedFromWindow() { source.stop(); listening = false; super.onDetachedFromWindow() }

    fun setListening(value: Boolean) {
        if (listening == value) return
        listening = value
        if (value) { source.start(); refreshTime(force = true) } else source.stop()
    }
    fun refreshTime(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && timeBucket == now / 60_000L) return
        timeBucket = now / 60_000L
        time = DateFormat.format(if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm", now).toString()
        val zh = resources.configuration.locales[0].language == "zh"
        val power = if (status.battery < 0) "—" else "${status.battery}%"
        contentDescription = if (zh) "$time，电量 $power" else "$time, battery $power"
        invalidate()
    }
    fun describe() = "visible=${visibility == VISIBLE} ${source.describe()}"

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val checkpoint = canvas.save()
        val scale = resources.displayMetrics.density / RailStatusLayout.REFERENCE_DENSITY
        canvas.scale(scale, scale)
        ink.color = Color.WHITE; ink.textSize = 29.9862f
        canvas.drawText(time, 98f, 64.4006f, ink)
        drawBattery(canvas)
        drawNetwork(canvas)
        canvas.restoreToCount(checkpoint)
    }

    private fun drawBattery(canvas: Canvas) {
        ink.textSize = 21.87f
        val text = if (status.battery < 0) "—" else status.battery.toString()
        canvas.drawText(text, 98f, 111.251f, ink)
        stroke.strokeWidth = 8.0676f
        val gap = Math.toDegrees(asin(((ink.measureText(text)/2f + 5f + stroke.strokeWidth/2f) / 48.6f)
            .coerceIn(0f, 1f)).toDouble()).toFloat()
        val first = 270f-gap-149f
        val second = 121f-gap
        fun arc(start: Float, sweep: Float) {
            if (sweep > .01f) canvas.drawArc(49.4f, 97.4f, 146.6f, 194.6f, start, sweep, false, stroke)
        }
        stroke.color = DIM
        arc(149f, first); arc(270f+gap, second)
        stroke.color = when {
            status.charging -> 0xff34c759.toInt()
            status.battery in 0..20 -> 0xffff3b30.toInt()
            else -> Color.WHITE
        }
        val filled = (first+second) * status.battery.coerceAtLeast(0)/100f
        arc(149f, minOf(first, filled))
        arc(270f+gap, (filled-first).coerceAtLeast(0f))
    }

    private fun drawNetwork(canvas: Canvas) {
        val raised = status.dualSim || status.airplane
        if (status.wifi >= 0 || status.network.isEmpty() && !status.airplane) {
            val checkpoint = canvas.save()
            canvas.translate(98f, 148.868f - if (raised) 9.72f else 0f)
            canvas.scale(.87f, .87f)
            stroke.strokeWidth = 7.8f
            stroke.color = if (status.wifi >= 2) Color.WHITE else DIM
            canvas.drawArc(-32.8f, -15.1f, 32.8f, 50.5f, 227.9f, 84.2f, false, stroke)
            stroke.color = if (status.wifi >= 1) Color.WHITE else DIM
            canvas.drawArc(-21f, -1.6f, 21f, 40.4f, 232.8f, 74.4f, false, stroke)
            shape.color = if (status.wifi >= 0) Color.WHITE else DIM
            canvas.drawPath(wifiDot, shape)
            canvas.restoreToCount(checkpoint)
        } else if (!status.airplane) {
            ink.textSize = 28f
            val baseline = 150f - (ink.ascent()+ink.descent())/2f - if (raised) 9.72f else 0f
            canvas.drawText(status.network, 98f, baseline, ink)
        }
        if (status.airplane) {
            val checkpoint = canvas.save()
            canvas.translate(98f, 181.235f)
            shape.color = Color.WHITE; canvas.drawPath(plane, shape)
            canvas.restoreToCount(checkpoint)
        } else if (status.dualSim) {
            for (row in 0..1) for (dot in 0..3) {
                shape.color = if (dot < if (row == 0) status.signal else status.secondSignal) Color.WHITE else DIM
                canvas.drawCircle(98f+(dot-1.5f)*13.122f, 179.534f+(row-.5f)*13.122f, 4.131f, shape)
            }
        } else {
            for ((dot, degrees) in SIGNAL_ANGLES.withIndex()) {
                val angle = Math.toRadians(degrees)
                shape.color = if (dot < status.signal) Color.WHITE else DIM
                canvas.drawCircle(98f+48.6f*cos(angle).toFloat(), 146f+48.6f*sin(angle).toFloat(), 5.103f, shape)
            }
        }
    }
    companion object {
        private const val DIM = 0x4cffffff
        private val SIGNAL_ANGLES = doubleArrayOf(121.0, 101.5, 80.0, 59.0)
    }
}
