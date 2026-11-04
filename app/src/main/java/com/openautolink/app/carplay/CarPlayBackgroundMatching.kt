package com.openautolink.app.carplay

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle

/** PixelCopy reads the decoder Surface only, so the overlay never samples itself. */
internal fun copyCarPlayBackground(view: SurfaceView, area: CarPlayBackgroundArea,
    handler: Handler, done: (CarPlayBackgroundPalette?) -> Unit) {
    if (view.width <= 0 || view.height <= 0 || !view.holder.surface.isValid) { done(null); return }
    val width = 160
    val height = (width.toDouble() * view.height / view.width).toInt().coerceIn(16, 96)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    try {
        PixelCopy.request(view, bitmap, { result ->
            val palette = try {
                if (result != PixelCopy.SUCCESS) null else {
                    val pixels = IntArray(width * height)
                    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                    CarPlayBackgroundPalette.sample(pixels, width, height, area)
                }
            } finally { bitmap.recycle() }
            done(palette)
        }, handler)
    } catch (_: IllegalArgumentException) { bitmap.recycle(); done(null) }
}

@Composable
internal fun CarPlayBackgroundMatching(view: SurfaceView?, area: CarPlayBackgroundArea?, active: Boolean,
    surfaceWidth: Int, surfaceHeight: Int, surfaceGeneration: Long) {
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    var colors by remember(view, area, surfaceWidth, surfaceHeight, surfaceGeneration) { mutableStateOf<CarPlayBackgroundPalette?>(null) }
    DisposableEffect(view, area, active, surfaceWidth, surfaceHeight, surfaceGeneration, lifecycle) {
        val loop = if (view != null && area != null && active && lifecycle.isAtLeast(Lifecycle.State.STARTED)) {
            val handler = Handler(Looper.getMainLooper())
            CarPlayBackgroundLoop(
                capture = { done -> copyCarPlayBackground(view, area, handler, done) },
                post = { task, delay -> handler.postDelayed(task, delay); Unit },
                remove = { task -> handler.removeCallbacks(task) },
                publish = { colors = it },
            ).also { it.start() }
        } else { colors = null; null }
        onDispose { loop?.close() }
    }
    if (area != null) colors?.let { CarPlayBackgroundFill(area, it) }
}

/** Draw-only sibling. Every rectangle is outside the safe area; no pointer modifier or video resize. */
@Composable
internal fun CarPlayBackgroundFill(area: CarPlayBackgroundArea, palette: CarPlayBackgroundPalette) {
    val top = remember(palette) { palette.top.map { Color(it) } }
    val bottom = remember(palette) { palette.bottom.map { Color(it) } }
    val left = remember(palette) { palette.left.map { Color(it) } }
    val right = remember(palette) { palette.right.map { Color(it) } }
    Canvas(Modifier.fillMaxSize().testTag("carPlayBackgroundFill")) {
        val l = area.left * size.width; val r = area.right * size.width
        val t = area.top * size.height; val b = area.bottom * size.height
        if (t > 0) drawRect(Brush.horizontalGradient(top, l, r), size = Size(size.width, t))
        if (b < size.height) drawRect(Brush.horizontalGradient(bottom, l, r), Offset(0f, b), Size(size.width, size.height - b))
        if (l > 0) drawRect(Brush.verticalGradient(left, t, b), size = Size(l, size.height))
        if (r < size.width) drawRect(Brush.verticalGradient(right, t, b), Offset(r, 0f), Size(size.width - r, size.height))
    }
}
