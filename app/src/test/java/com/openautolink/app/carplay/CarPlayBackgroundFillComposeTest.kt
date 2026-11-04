package com.openautolink.app.carplay

import android.app.Application
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.openautolink.app.ui.testing.createOalComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class)
@Config(application=Application::class,sdk=[32],qualifiers="w800dp-h600dp-mdpi")
@ConscryptMode(ConscryptMode.Mode.OFF)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
class CarPlayBackgroundFillComposeTest {
    @get:Rule val compose=createOalComposeRule()
    @Test fun overlayColorsOnlyMarginsPreservesSurfaceSizeAndForwardsLocalTouch() {
        val area=CarPlayBackgroundArea(.1f,.1f,.8f,.9f)
        val palette=CarPlayBackgroundPalette(List(16){0xff887766.toInt()},List(16){0xff887766.toInt()},
            List(16){0xff887766.toInt()},List(16){0xff887766.toInt()})
        var matching by mutableStateOf(true)
        var pressed: Pair<Float,Float>? = null
        var surface: SurfaceView? = null
        var composeView: android.view.View? = null
        compose.setContent {
            composeView = LocalView.current
            Box(Modifier.size(600.dp,300.dp).testTag("videoParent")) {
                AndroidView(factory={context->SurfaceView(context).also{surface=it}},modifier=Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Color.Magenta).pointerInteropFilter {
                    pressed=it.x to it.y; true
                })
                if(matching) CarPlayBackgroundFill(area,palette)
            }
        }
        val before=compose.onNodeWithTag("videoParent").fetchSemanticsNode().boundsInRoot
        compose.runOnIdle { assertEquals(600,surface!!.width); assertEquals(300,surface!!.height) }
        // Robolectric has no real window redraw for captureToImage/PixelCopy. Draw
        // the actual Compose view into a native Canvas, as the repository's UI gates do.
        fun capture(): android.graphics.Bitmap {
            var result: android.graphics.Bitmap? = null
            compose.runOnIdle {
                result=android.graphics.Bitmap.createBitmap(600,300,android.graphics.Bitmap.Config.ARGB_8888)
                val canvas=android.graphics.Canvas(result!!)
                canvas.translate(-before.left,-before.top)
                composeView!!.draw(canvas)
            }
            return result!!
        }
        val matched=capture()
        val preview=java.io.File("build/reports/tests/background-match-preview.png")
        preview.parentFile.mkdirs()
        preview.outputStream().use { matched.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
        assertEquals(android.graphics.Color.MAGENTA,matched.getPixel(300,150)) // safe center is untouched
        assertEquals(0xff887766.toInt(),matched.getPixel(590,150))
        assertEquals(0xff887766.toInt(),matched.getPixel(300,5))
        compose.onNodeWithTag("videoParent").performTouchInput { click(Offset(590f,150f)) }
        compose.runOnIdle { assertEquals(590f,pressed!!.first,1f); assertEquals(150f,pressed!!.second,1f); matching=false }
        assertEquals(before,compose.onNodeWithTag("videoParent").fetchSemanticsNode().boundsInRoot)
        val original=capture()
        java.io.File(preview.parentFile,"background-match-off.png").outputStream().use {
            original.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)
        }
        assertEquals(android.graphics.Color.MAGENTA,original.getPixel(590,150))
        matched.recycle(); original.recycle()
    }
}
