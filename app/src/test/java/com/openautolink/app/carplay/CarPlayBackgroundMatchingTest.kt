package com.openautolink.app.carplay

import com.openautolink.app.data.CarPlaySettings
import com.openautolink.carplaylab.media.CarPlayAutoScalePolicy
import com.shilapi.xcertplay.media.VideoDecoderProbe
import org.junit.Assert.*
import org.junit.Test

class CarPlayBackgroundMatchingTest {
    private val area = CarPlayBackgroundArea(.1f, .1f, .8f, .9f)
    private val gray = 0xff887766.toInt()
    private val purple = 0xff5500aa.toInt()
    private fun frame() = IntArray(160 * 80) { i ->
        val x = i % 160; val y = i / 160
        if (x in 16..127 && y in 8..71) gray else purple
    }
    @Test fun samplesInsideSafeRectangleInsteadOfPurpleWallpaperAndIgnoresSinglePixelHighlights() {
        val pixels = frame()
        for (y in 8..71) pixels[y * 160 + 124] = -1
        val palette = CarPlayBackgroundPalette.sample(pixels, 160, 80, area)!!
        listOf(palette.top, palette.bottom, palette.left, palette.right).forEach { colors ->
            assertTrue(colors.isNotEmpty()); assertTrue(colors.all { it == gray })
        }
    }
    @Test fun differentRowsProduceSmoothedColorFillRatherThanDuplicatedArtwork() {
        val pixels = frame()
        for (y in 40..71) for (x in 16..127) pixels[y * 160 + x] = 0xff223344.toInt()
        val palette = CarPlayBackgroundPalette.sample(pixels, 160, 80, area)!!
        assertEquals(gray, palette.right[10]); assertEquals(0xff223344.toInt(), palette.right[50])
        assertEquals(palette.top.last(), palette.right.first())
        assertEquals(palette.bottom.last(), palette.right.last())
    }
    @Test fun boundedSamplingRejectsOversizedFramesAndSkipsTinySafeRectangles() {
        assertNull(CarPlayBackgroundPalette.sample(frame(),160,80,CarPlayBackgroundArea(.49f,.49f,.51f,.51f)))
        try { CarPlayBackgroundPalette.sample(IntArray(161*80),161,80,area); fail() } catch (_: IllegalArgumentException) {}
    }
    @Test fun optInRoundTripsWithoutChangingVideoSafeAreaOrPhoneOffer() {
        val settings = CarPlaySettings(safeTop=684, safeRight=1152, safeLeft=45)
        assertFalse(CarPlaySettings.decode("{}").matchBackground)
        val matching = settings.copy(matchBackground=true)
        assertEquals(matching,CarPlaySettings.decode(CarPlaySettings.encode(matching)))
        val viewport = CarPlayAutoScalePolicy.Input(2000,800,2000,800,200.0,200.0,200)
        val probe = VideoDecoderProbe { "qualified" }
        val base = settings.qualifiedDisplay(viewport,probe)
        val display = matching.qualifiedDisplay(viewport,probe)
        assertEquals(base.toAirPlayDisplay(),display.toAirPlayDisplay())
        assertEquals(base.width,display.width); assertEquals(base.height,display.height)
        assertNotEquals(base,display)
        val derived = CarPlayBackgroundArea.from(display)!!
        assertEquals(display.encodedSafeInsets!!.right.toFloat()/display.width,1-derived.right,.0001f)
        assertNull(CarPlayBackgroundArea.from(base))
        assertNull(CarPlayBackgroundArea.from(matching.copy(insetEntirePicture=true).qualifiedDisplay(viewport,probe)))
        assertNull(CarPlayBackgroundArea.from(matching.copy(safeTop=0,safeRight=0,safeLeft=0).qualifiedDisplay(viewport,probe)))
    }
    @Test fun captureLoopHasOneInFlightRequestAndRejectsStoppedOrDuplicateResults() {
        val callbacks = mutableListOf<(CarPlayBackgroundPalette?) -> Unit>()
        val scheduled = mutableListOf<Pair<Runnable,Long>>()
        val published = mutableListOf<CarPlayBackgroundPalette?>()
        val loop = CarPlayBackgroundLoop({ callbacks += it },{task,delay -> scheduled += task to delay},
            { task -> scheduled.removeAll { it.first == task } },{ published += it })
        loop.start(); loop.start()
        assertEquals(1,callbacks.size); assertTrue(scheduled.isEmpty())
        val palette=CarPlayBackgroundPalette.sample(frame(),160,80,area)!!
        callbacks[0](palette)
        assertEquals(listOf(palette),published); assertEquals(400L,scheduled.single().second)
        scheduled.removeAt(0).first.run()
        assertEquals(2,callbacks.size)
        callbacks[0](palette) // a prior request cannot complete a newer in-flight request
        assertTrue(scheduled.isEmpty())
        loop.close(); callbacks[1](palette)
        assertEquals(listOf(palette,null),published); assertTrue(scheduled.isEmpty())
        loop.start(); assertEquals(2,callbacks.size)
    }
    @Test fun captureFailureClearsTheFillAndRetriesAtBoundedInterval() {
        var completion: ((CarPlayBackgroundPalette?) -> Unit)? = null
        var delay = 0L
        val outputs = mutableListOf<CarPlayBackgroundPalette?>()
        val loop=CarPlayBackgroundLoop({completion=it},{_,ms->delay=ms},{},{outputs+=it})
        loop.start(); completion!!(null)
        assertEquals(listOf<CarPlayBackgroundPalette?>(null),outputs); assertEquals(400L,delay)
        loop.close()
    }
}
