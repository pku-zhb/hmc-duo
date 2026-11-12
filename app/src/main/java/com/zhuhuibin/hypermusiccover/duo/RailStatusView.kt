package com.zhuhuibin.hypermusiccover.duo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Matrix
import android.view.MotionEvent
import android.view.View
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin

/** Display-only status cluster with a clock that shares the artwork scene's fade progress. */
internal class RailStatusView(context: Context) : View(context) {
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        // The reference applies this shadow to glyph paths, not the ring or signal marks.
        setShadowLayer(6f, 0f, 1f, 0x26000000)
    }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
    }
    private val shape = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL_AND_STROKE; strokeJoin = Paint.Join.ROUND; strokeWidth = 0f
    }
    private val glyph = Path()
    private val glyphMatrix = Matrix()
    private val plane = Path().apply {
        val direction = Path.Direction.CW
        addRoundRect(-15.95f, -2.95f, 18.95f, 2.95f, 2.95f, 2.95f, direction)
        val part = Path()
        part.addRect(-15.95f, -2.95f, -9.95f, 2.95f, direction)
        op(part, Path.Op.UNION)
        for (sign in intArrayOf(-1, 1)) {
            val side = sign.toFloat()
            val attachY = -2.5f * side
            part.rewind(); part.moveTo(-15.95f, attachY)
            part.lineTo(-15.95f, -7.25f * side)
            part.quadTo(-15.95f, -7.75f * side, -15.4f, -7.75f * side)
            part.lineTo(-13.4f, -7.75f * side)
            part.quadTo(-12.9f, -7.75f * side, -12.65f, -7.3f * side)
            part.lineTo(-10.2f, attachY); part.close(); op(part, Path.Op.UNION)
            part.rewind(); part.moveTo(-3.7f, attachY)
            part.lineTo(-7.45f, -15.3f * side)
            part.quadTo(-6.3f, -16.5f * side, -5.15f, -15.3f * side)
            part.lineTo(5.85f, -4.35f * side)
            part.quadTo(7.6f, -3.05f * side, 14.5f, -2.95f * side)
            part.lineTo(14.5f, attachY); part.close(); op(part, Path.Op.UNION)
        }
        transform(Matrix().apply { setScale(1.0167364f, 1.0167364f) })
    }
    private val wifiDot = Path().apply {
        moveTo(-7f, 10.3f); quadTo(0f, 5.1f, 7f, 10.3f)
        quadTo(8.7f, 11.9f, 7f, 13.9f); lineTo(2.6f, 18.1f)
        quadTo(0f, 20.5f, -2.6f, 18.1f); lineTo(-7f, 13.9f)
        quadTo(-8.7f, 11.9f, -7f, 10.3f); close()
    }
    private var status = RailStatusSnapshot()
    private var time = ""
    private var timeAlpha = 0f
    private val clock = RailStatusClockSource(context) { text ->
        time = text
        refreshDescription()
        invalidate()
    }
    private val source = RailStatusSource(context) { next ->
        val changed = status != next
        status = next
        refreshDescription()
        if (changed) invalidate()
    }
    private var listening = false

    init {
        isClickable = false; isLongClickable = false; isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        accessibilityLiveRegion = ACCESSIBILITY_LIVE_REGION_NONE
    }
    override fun onTouchEvent(event: MotionEvent): Boolean = false
    override fun onDetachedFromWindow() {
        source.stop(); clock.stop(); listening = false
        super.onDetachedFromWindow()
    }

    fun setTimeAlpha(value: Float) {
        val next = if (value.isFinite()) value.coerceIn(0f, 1f) else 0f
        if (next == timeAlpha) { if (next > 0f) clock.refresh(); return }
        timeAlpha = next
        syncClockListening()
        refreshDescription()
        invalidate()
    }

    private fun syncClockListening() {
        if (listening && timeAlpha > 0f) clock.start() else clock.stop()
    }

    fun setListening(value: Boolean) {
        if (listening == value) return
        listening = value
        if (value) { source.start(); refreshDescription() } else source.stop()
        syncClockListening()
    }
    private fun refreshDescription() {
        val zh = resources.configuration.locales[0].language == "zh"
        val power = if (status.battery < 0) "—" else "${status.battery}%"
        val clockText = if (timeAlpha > 0f && time.isNotEmpty()) time + (if (zh) "，" else ", ") else ""
        contentDescription = clockText + if (zh) "电量 $power" else "Battery $power"
    }
    fun describe() = "visible=${visibility == VISIBLE} timeAlpha=$timeAlpha ${clock.describe()} ${source.describe()}"

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val checkpoint = canvas.save()
        val scale = resources.displayMetrics.density / RailStatusLayout.REFERENCE_DENSITY
        canvas.scale(scale, scale)
        ink.color = Color.WHITE
        val clockAlpha = (255f * timeAlpha).toInt().coerceIn(0, 255)
        if (clockAlpha > 0) {
            // A translucent shadow color owns its alpha independently of Paint.alpha.
            // Fade the completed time drawing, including its shadow, so the spring's
            // near-zero tail cannot leave a dark time outline beside the restored clock.
            val clockLayer = if (clockAlpha < 255) canvas.saveLayerAlpha(
                0f, 0f, RailStatusLayout.WIDTH, RailStatusLayout.HEIGHT, clockAlpha) else canvas.save()
            // Restore the original three-in-one renderer's exact reference typography.
            drawGlyphs(canvas, time, 29.986198f, 98f, 64.400604f)
            canvas.restoreToCount(clockLayer)
        }
        drawBattery(canvas)
        drawNetwork(canvas)
        canvas.restoreToCount(checkpoint)
    }

    private fun drawGlyphs(canvas: Canvas, text: String, size: Float, x: Float, baseline: Float) {
        if (RailStatusGlyphs.path(text, size, false, x, baseline, glyph, glyphMatrix))
            canvas.drawPath(glyph, ink)
    }

    private fun mobileBars() = !status.airplane && status.wifi < 0 && status.network.isNotEmpty()

    private fun drawBattery(canvas: Canvas) {
        val text = if (status.battery < 0) "" else status.battery.toString()
        val textWidth = if (text.isEmpty()) 13f else RailStatusGlyphs.measure(text, 21.869999f, false)
        stroke.strokeWidth = 8.067599f
        val gap = Math.toDegrees(asin(((textWidth/2f + 5f + stroke.strokeWidth/2f) / 48.6f)
            .coerceIn(0f, 1f)).toDouble()).toFloat()
        val narrowOpening = status.airplane || mobileBars() || status.dualSim
        val start = if (narrowOpening) 152f else 149f
        val first = 270f-gap-start
        val second = (if (narrowOpening) 28f else 31f)+90f-gap
        fun arc(startAngle: Float, sweep: Float) {
            if (sweep > 0f) canvas.drawArc(49.4f, 97.4f, 146.6f, 194.6f, startAngle, sweep, false, stroke)
        }
        stroke.color = DIM
        arc(start, first); arc(270f+gap, second)
        stroke.color = when {
            status.charging -> 0xff34c759.toInt()
            status.battery in 0..20 -> 0xffff3b30.toInt()
            else -> Color.WHITE
        }
        val ratio = if (status.battery < 0) 0f else (status.battery/100f).coerceIn(.03f, 1f)
        val filled = (first+second) * ratio
        arc(start, minOf(first, filled))
        if (filled > first) arc(270f+gap, filled-first)
    }

    private fun drawWifi(canvas: Canvas, centerY: Float) {
        val checkpoint = canvas.save()
        canvas.translate(98f, centerY)
        canvas.scale(.87f, .87f)
        stroke.strokeWidth = 7.8f
        stroke.color = if (status.wifi >= 2) Color.WHITE else DIM
        canvas.drawArc(-32.8f, -15.1f, 32.8f, 50.5f, 227.9f, 84.2f, false, stroke)
        stroke.color = if (status.wifi >= 1) Color.WHITE else DIM
        canvas.drawArc(-21f, -1.6f, 21f, 40.4f, 232.8f, 74.4f, false, stroke)
        shape.color = if (status.wifi >= 0) Color.WHITE else DIM
        shape.style = Paint.Style.FILL
        canvas.drawPath(wifiDot, shape)
        shape.style = Paint.Style.FILL_AND_STROKE
        canvas.restoreToCount(checkpoint)
    }

    private fun drawDualSignal(canvas: Canvas, centerY: Float) {
        for (row in 0..1) for (dot in 0..3) {
            val level = if (row == 0) status.signal else status.secondSignal
            shape.color = if (dot < level) Color.WHITE else DIM
            canvas.drawCircle(98f+(dot-1.5f)*13.122f, centerY+(row-.5f)*13.122f, 4.131f, shape)
        }
    }

    private fun drawNetwork(canvas: Canvas) {
        if (status.battery >= 0) drawGlyphs(canvas, status.battery.toString(), 21.869999f, 98f, 111.251f)
        else {
            // Unknown is a local data state, not the reference's misleading default 100%.
            ink.style = Paint.Style.STROKE; ink.strokeWidth = 2f
            canvas.drawLine(91.5f, 103f, 104.5f, 103f, ink)
            ink.style = Paint.Style.FILL
        }
        when {
            status.airplane -> {
                if (status.wifi >= 0) drawWifi(canvas, 154.748f-15.6f)
                val checkpoint = canvas.save()
                canvas.translate(98f, 181.235f)
                shape.color = Color.WHITE; canvas.drawPath(plane, shape)
                canvas.restoreToCount(checkpoint)
            }
            mobileBars() -> {
                if (status.dualSim) drawDualSignal(canvas, 142.355f)
                else {
                    val width = 7.5937495f
                    val left = 98f-(33.887108f+width)/2f
                    for (bar in 0..3) {
                        val height = (bar*2f+7.8f)*1.8984374f
                        val x = left+bar*11.295702f
                        shape.color = if (bar < status.signal) Color.WHITE else DIM
                        canvas.drawRoundRect(x, 154.73282f-height, x+width, 154.73282f,
                            .3f*width, .3f*width, shape)
                    }
                }
                drawGlyphs(canvas, status.network, 21.869999f, 98f, 192.17f)
            }
            else -> {
                drawWifi(canvas, 164.468f-(if (status.dualSim) 9.72f else 0f)-15.6f)
                if (status.dualSim) drawDualSignal(canvas, 179.534f)
                else for ((dot, degrees) in SIGNAL_ANGLES.withIndex()) {
                    val angle = Math.toRadians(degrees)
                    shape.color = if (dot < status.signal) Color.WHITE else DIM
                    canvas.drawCircle(98f+48.6f*cos(angle).toFloat(), 146f+48.6f*sin(angle).toFloat(), 5.1029997f, shape)
                }
            }
        }
    }
    companion object {
        private const val DIM = 0x4dffffff
        private val SIGNAL_ANGLES = doubleArrayOf(121.0, 101.5, 80.0, 59.0)
    }
}
