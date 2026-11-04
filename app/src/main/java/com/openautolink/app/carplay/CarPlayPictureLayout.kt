package com.openautolink.app.carplay

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import com.openautolink.carplaylab.media.CarPlayPictureInsets

/** Child coordinates are local to the inset rectangle; ancestors retain the full measured parent. */
internal fun Modifier.carPlayPictureBounds(insets: CarPlayPictureInsets): Modifier = layout { measurable, constraints ->
    val bounds = insets.bounds(constraints.maxWidth, constraints.maxHeight)
    val child = measurable.measure(Constraints.fixed(bounds.width, bounds.height))
    layout(constraints.maxWidth, constraints.maxHeight) { child.place(bounds.left, bounds.top) }
}
