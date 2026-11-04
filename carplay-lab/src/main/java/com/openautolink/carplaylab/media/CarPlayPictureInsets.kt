// SPDX-License-Identifier: GPL-3.0-only
package com.openautolink.carplaylab.media

/** Fractions of the full chrome-free parent, shared by canvas qualification and rendering. */
data class CarPlayPictureInsets(val top: Int = 0, val bottom: Int = 0,
    val left: Int = 0, val right: Int = 0) {
    init { require(listOf(top, bottom, left, right).all { it in 0..4999 }) }
    data class Bounds(val left: Int, val top: Int, val width: Int, val height: Int)
    fun bounds(width: Int, height: Int): Bounds {
        require(width in 1..16384 && height in 1..16384)
        val x = width * left / 10000
        val y = height * top / 10000
        return Bounds(x, y, width - x - width * right / 10000,
            height - y - height * bottom / 10000)
    }
}
