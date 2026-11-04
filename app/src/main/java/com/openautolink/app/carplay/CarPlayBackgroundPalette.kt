package com.openautolink.app.carplay

import com.openautolink.carplaylab.media.CarPlayDisplayPreferences

/** Normalized coordinates of the *encoded* safe area, already mapped once by qualification. */
internal data class CarPlayBackgroundArea(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    init { require(left >= 0 && top >= 0 && right <= 1 && bottom <= 1 && right > left && bottom > top) }
    companion object {
        fun from(display: CarPlayDisplayPreferences): CarPlayBackgroundArea? {
            if (!display.matchBackground || display.pictureInsets != com.openautolink.carplaylab.media.CarPlayPictureInsets()) return null
            val safe = display.encodedSafeInsets ?: return null
            if (safe.left + safe.right + safe.top + safe.bottom == 0) return null
            return CarPlayBackgroundArea(safe.left.toFloat() / display.width, safe.top.toFloat() / display.height,
                1f - safe.right.toFloat() / display.width, 1f - safe.bottom.toFloat() / display.height)
        }
    }
}

/** Only small color arrays survive a copy. No retained video frame, disk or network output. */
internal data class CarPlayBackgroundPalette(
    val top: List<Int>, val bottom: List<Int>, val left: List<Int>, val right: List<Int>,
) {
    companion object {
        fun sample(pixels: IntArray, width: Int, height: Int, area: CarPlayBackgroundArea): CarPlayBackgroundPalette? {
            require(width in 1..160 && height in 1..96 && pixels.size == width * height)
            val l = kotlin.math.ceil(area.left * width).toInt()
            val t = kotlin.math.ceil(area.top * height).toInt()
            val r = kotlin.math.floor(area.right * width).toInt() - 1
            val b = kotlin.math.floor(area.bottom * height).toInt() - 1
            if (r - l < 8 || b - t < 8) return null
            // Move a few sample pixels inside the safe rectangle, away from rounded card outlines.
            val dx = minOf(3, (r - l) / 4)
            val dy = minOf(3, (b - t) / 4)
            fun color(xs: IntRange, ys: IntRange): Int = median(ys.flatMap { y -> xs.map { x -> pixels[y * width + x] } })
            fun smooth(colors: List<Int>) = colors.indices.map { i -> median(colors.subList(maxOf(0, i - 3), minOf(colors.size, i + 4))) }
            val top = smooth((l..r).map { x -> color(x..x, t + dy - 1..t + dy + 1) }).toMutableList()
            val bottom = smooth((l..r).map { x -> color(x..x, b - dy - 1..b - dy + 1) }).toMutableList()
            val left = smooth((t..b).map { y -> color(l + dx - 1..l + dx + 1, y..y) }).toMutableList()
            val right = smooth((t..b).map { y -> color(r - dx - 1..r - dx + 1, y..y) }).toMutableList()
            fun corner(a: MutableList<Int>, ai: Int, c: MutableList<Int>, ci: Int) {
                val color = median(listOf(a[ai], c[ci])); a[ai] = color; c[ci] = color
            }
            corner(top, 0, left, 0); corner(top, top.lastIndex, right, 0)
            corner(bottom, 0, left, left.lastIndex); corner(bottom, bottom.lastIndex, right, right.lastIndex)
            return CarPlayBackgroundPalette(top, bottom, left, right)
        }
        private fun median(colors: List<Int>): Int {
            fun channel(shift: Int) = colors.map { (it ushr shift) and 255 }.sorted()[colors.size / 2]
            return (255 shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
        }
    }
}

/** Main-thread loop: one outstanding capture, bounded sampling, stale callback rejection. */
internal class CarPlayBackgroundCopyGate {
    private var pending = false
    fun acquire(): Boolean = if (pending) false else { pending = true; true }
    fun release() { pending = false }
}

internal class CarPlayBackgroundLoop(
    private val capture: ((CarPlayBackgroundPalette?) -> Unit) -> Unit,
    private val post: (Runnable, Long) -> Unit,
    private val remove: (Runnable) -> Unit,
    private val publish: (CarPlayBackgroundPalette?) -> Unit,
) : AutoCloseable {
    private var active = false
    private var started = false
    private var pending = false
    private var sequence = 0L
    private val tick = Runnable { request() }
    fun start() { if (!started) { started = true; active = true; request() } }
    private fun request() {
        if (!active || pending) return
        pending = true
        val ticket = ++sequence
        capture { colors ->
            if (ticket == sequence && pending) {
                pending = false
                if (active) { publish(colors); post(tick, 400L) }
            }
        }
    }
    override fun close() { active = false; sequence++; pending = false; remove(tick); publish(null) }
}
