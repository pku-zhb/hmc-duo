package com.zhuhuibin.hypermusiccover.duo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.format.DateFormat
import java.util.TimeZone

/** Observe time only while the status clock is drawn; handler deadlines never wake the CPU. */
internal class RailStatusClockSource(private val context: Context, private val changed: (String) -> Unit) {
    private val main = Handler(Looper.getMainLooper())
    private var running = false
    private var receiverRegistered = false
    private var settingsRegistered = false
    private var minute = Long.MIN_VALUE
    private var error: String? = null
    private val tick = Runnable { refresh(force = true) }
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) { refresh(force = true) }
    }
    private val settings = object : ContentObserver(main) {
        override fun onChange(selfChange: Boolean) { refresh(force = true) }
    }

    fun start() {
        if (running) return
        running = true
        attempt("time receiver") {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_TIME_TICK)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
                addAction(Intent.ACTION_LOCALE_CHANGED)
            }
            context.registerReceiver(receiver, filter, null, main, Context.RECEIVER_EXPORTED)
            receiverRegistered = true
        }
        attempt("clock format") {
            context.contentResolver.registerContentObserver(Settings.System.getUriFor(Settings.System.TIME_12_24),
                false, settings)
            settingsRegistered = true
        }
        refresh(force = true)
    }

    fun stop() {
        running = false
        main.removeCallbacks(tick)
        if (receiverRegistered) runCatching { context.unregisterReceiver(receiver) }
        if (settingsRegistered) runCatching { context.contentResolver.unregisterContentObserver(settings) }
        receiverRegistered = false; settingsRegistered = false
        minute = Long.MIN_VALUE
    }

    /** Also called on a resumed frame so a sleeping CPU cannot leave the previous minute. */
    fun refresh(force: Boolean = false) {
        if (!running) return
        val now = System.currentTimeMillis()
        val bucket = Math.floorDiv(now, 60_000L)
        if (!force && minute == bucket) return
        minute = bucket
        attempt("time format") {
            changed(RailStatusTime.format(now, TimeZone.getDefault(), DateFormat.is24HourFormat(context)))
        }
        main.removeCallbacks(tick)
        main.postDelayed(tick, RailStatusTime.nextMinuteDelay(now))
    }

    private inline fun attempt(operation: String, block: () -> Unit) {
        runCatching(block).onFailure {
            val next = "$operation:${it.javaClass.simpleName}"
            if (error != next) { error = next; Xp.w("DuoStatusClock: $next") }
        }
    }

    fun describe() = "clockListening=$running clockError=${error ?: "none"}"
}
