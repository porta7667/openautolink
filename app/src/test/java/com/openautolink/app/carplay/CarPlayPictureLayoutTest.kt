package com.openautolink.app.carplay

import android.app.Application
import android.view.SurfaceView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import com.openautolink.app.ui.testing.createOalComposeRule
import com.openautolink.carplaylab.media.CarPlayPictureInsets
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [32], qualifiers = "w1200dp-h900dp-mdpi")
@ConscryptMode(ConscryptMode.Mode.OFF)
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
class CarPlayPictureLayoutTest {
    @get:Rule val compose = createOalComposeRule()
    @Test fun pictureAndTouchMoveTogetherWhileParentStaysFullSize() {
        val insets = mutableStateOf(CarPlayPictureInsets(top=1000, right=2000))
        var normalized: Pair<Double, Double>? = null
        var surface: SurfaceView? = null
        val mapper = CarPlayTouchMapper()
        compose.setContent {
            Box(Modifier.size(1000.dp, 500.dp).testTag("parent")) {
                Box(Modifier.fillMaxSize().carPlayPictureBounds(insets.value)) {
                    BoxWithConstraints(Modifier.align(Alignment.Center).aspectRatio(16f/9)) {
                        val width = constraints.maxWidth
                        val height = constraints.maxHeight
                        AndroidView(factory = { context -> SurfaceView(context).also { surface = it } },
                            modifier = Modifier.fillMaxSize().testTag("video"))
                        Box(Modifier.fillMaxSize().testTag("touch").pointerInteropFilter { event ->
                            val contacts = mapper.map(event.actionMasked, event.actionIndex,
                                listOf(CarPlayPointer(event.getPointerId(0), event.x, event.y)), width, height)
                            contacts.firstOrNull { it.down }?.let { normalized = it.x to it.y }
                            true
                        })
                    }
                }
            }
        }
        val parent = compose.onNodeWithTag("parent").fetchSemanticsNode().boundsInRoot
        val video = compose.onNodeWithTag("video").fetchSemanticsNode().boundsInRoot
        val touch = compose.onNodeWithTag("touch").fetchSemanticsNode().boundsInRoot
        assertEquals(video, touch)
        compose.runOnIdle {
            assertEquals(800, surface!!.width)
            assertEquals(450, surface!!.height)
        }
        assertEquals(800f, video.width, 1f)
        assertEquals(450f, video.height, 1f)
        assertEquals(50f, video.top - parent.top, 1f)
        assertEquals(200f, parent.right - video.right, 1f)
        compose.onNodeWithTag("touch").performTouchInput { click(center) }
        compose.runOnIdle { assertEquals(0.5, normalized!!.first, .01); assertEquals(0.5, normalized!!.second, .01) }
        normalized = null
        compose.onNodeWithTag("parent").performTouchInput { click(Offset(900f, 20f)) }
        compose.runOnIdle { assertNull(normalized); insets.value = CarPlayPictureInsets() }
        assertEquals(parent, compose.onNodeWithTag("parent").fetchSemanticsNode().boundsInRoot)
        val restored = compose.onNodeWithTag("video").fetchSemanticsNode().boundsInRoot
        assertEquals(500f, restored.height, 1f)
        assertEquals(16f/9, restored.width/restored.height, .01f)
    }
}
