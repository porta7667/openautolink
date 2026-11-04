package com.openautolink.app.data

import com.openautolink.carplaylab.media.CarPlayAutoScalePolicy
import com.shilapi.xcertplay.media.VideoDecoderProbe
import org.junit.Assert.*
import org.junit.Test

class CarPlayPictureSettingsTest {
    private val viewport = CarPlayAutoScalePolicy.Input(2000, 800, 2000, 800, 200.0, 200.0, 200)
    private val probe = VideoDecoderProbe { "qualified-${it.codec}" }
    private val margins = CarPlaySettings(insetEntirePicture = true, safeTop = 500, safeBottom = 250,
        safeLeft = 100, safeRight = 1500)

    @Test fun migrationIsOffAndSavedSwitchAndEdgesRoundTrip() {
        assertFalse(CarPlaySettings.decode("""{"resolution":"4k"}""").insetEntirePicture)
        assertEquals(margins, CarPlaySettings.decode(CarPlaySettings.encode(margins)))
    }
    @Test fun windowNegotiationUsesTheSameInsetBoundsAsTheRendererWithoutDoublePadding() {
        val request = margins.qualifiedDisplay(viewport, probe)
        val bounds = request.pictureInsets.bounds(2000, 800)
        assertEquals(1680, bounds.width)
        assertEquals(740, bounds.height)
        assertEquals(bounds.width, request.autoScaleInput!!.viewportWidthPx)
        assertEquals(bounds.height, request.autoScaleInput!!.viewportHeightPx)
        assertEquals(bounds.width.toDouble() / bounds.height, request.width.toDouble() / request.height, 0.02)
        assertNull(request.encodedSafeInsets)
        assertEquals(2000, viewport.viewportWidthPx) // full parent receipt not mutated
    }
    @Test fun exactPresetStaysExactAndAspectFitsWithinInsetRectangle() {
        val request = margins.copy(autoResolution = false, fitToWindow = false, resolution = "4k")
            .qualifiedDisplay(viewport, probe)
        assertEquals(3840, request.width)
        assertEquals(2160, request.height)
        assertEquals(300, request.pictureInsets.bounds(2000,800).let { 2000 - it.left - it.width })
    }
    @Test fun offKeepsExistingPhoneSafeAreaBehavior() {
        val request = margins.copy(insetEntirePicture = false).qualifiedDisplay(viewport, probe)
        assertNotNull(request.encodedSafeInsets)
        assertEquals(2000, request.pictureInsets.bounds(2000,800).width)
    }
    @Test(expected = IllegalArgumentException::class) fun absentParentCannotSilentlyIgnorePictureMargins() {
        margins.copy(autoResolution = false, autoScale = false, fitToWindow = false).qualifiedDisplay(null, probe)
    }
}
