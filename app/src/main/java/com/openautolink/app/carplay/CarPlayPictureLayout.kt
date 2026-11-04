package com.openautolink.app.carplay

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import com.openautolink.carplaylab.media.CarPlayPictureInsets

/** Child coordinates are local to the inset rectangle; ancestors retain the full measured parent. */
internal fun Modifier.carPlayPictureBounds(insets: CarPlayPictureInsets): Modifier = layout { measurable, constraints ->
    // A transient zero-size parent during Activity resize cannot produce a valid canvas.
    // Lay out no content until a positive parent is measured; do not fabricate geometry.
    if (constraints.maxWidth == 0 || constraints.maxHeight == 0) {
        val child = measurable.measure(Constraints.fixed(0, 0))
        return@layout layout(constraints.maxWidth, constraints.maxHeight) { child.place(0, 0) }
    }
    val bounds = insets.bounds(constraints.maxWidth, constraints.maxHeight)
    val child = measurable.measure(Constraints.fixed(bounds.width, bounds.height))
    layout(constraints.maxWidth, constraints.maxHeight) { child.place(bounds.left, bounds.top) }
}
