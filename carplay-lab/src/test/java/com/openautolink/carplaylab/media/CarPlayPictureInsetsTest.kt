package com.openautolink.carplaylab.media

import org.junit.Assert.*
import org.junit.Test

class CarPlayPictureInsetsTest {
    @Test fun asymmetricMarginsLeaveTopAndRightSpaceWithoutStretching() {
        val bounds = CarPlayPictureInsets(top = 302, left = 45, right = 1205).bounds(2000, 800)
        assertEquals(CarPlayPictureInsets.Bounds(9, 24, 1750, 776), bounds)
    }
    @Test fun disabledMarginsPreserveFullParent() {
        assertEquals(CarPlayPictureInsets.Bounds(0, 0, 2000, 800), CarPlayPictureInsets().bounds(2000, 800))
    }
    @Test fun roundingAlwaysLeavesPositiveSpaceAtLargestSupportedMargins() {
        for (size in listOf(1, 2, 100, 2914, 16384)) {
            val b = CarPlayPictureInsets(4999, 4999, 4999, 4999).bounds(size, size)
            assertTrue(b.width > 0 && b.height > 0)
            assertTrue(b.left + b.width <= size && b.top + b.height <= size)
        }
    }
    @Test fun changingMarginsChangesImmutableRequestIdentity() {
        val original = CarPlayDisplayPreferences()
        val inset = original.copy(pictureInsets = CarPlayPictureInsets(top = 500))
        assertNotEquals(original, inset)
        assertEquals(inset, inset.copy())
    }
    @Test(expected = IllegalArgumentException::class) fun invalidMarginsAreRejected() {
        CarPlayPictureInsets(right = 5000)
    }
}
