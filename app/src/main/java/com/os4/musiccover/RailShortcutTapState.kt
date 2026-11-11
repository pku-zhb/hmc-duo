package com.os4.musiccover

/** A single-finger tap may activate only the shortcut whose native gesture saw its DOWN. */
internal class RailShortcutTapState {
    private var side = -1
    private var x = 0f
    private var y = 0f
    private var start = 0L
    private var slop = 0f
    private var ready = -1
    private var released = 0L
    fun cancel() { side = -1; ready = -1 }
    fun begin(side: Int, x: Float, y: Float, time: Long, slop: Float) {
        cancel(); this.side = side; this.x = x; this.y = y; start = time; this.slop = slop
    }
    fun move(x: Float, y: Float) {
        if (kotlin.math.hypot(x - this.x, y - this.y) > slop) cancel()
    }
    fun end(x: Float, y: Float, time: Long) {
        move(x, y)
        ready = if (side in 0..1 && time - start in 0..350) side else -1
        released = time; side = -1
    }
    fun claim(side: Int, time: Long): Boolean {
        if (side !in 0..1 || side != ready || time - released !in 0..150) return false
        ready = -1
        return true
    }
}
