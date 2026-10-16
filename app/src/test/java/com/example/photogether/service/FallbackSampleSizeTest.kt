package com.example.photogether.service

import org.junit.Assert.assertEquals
import org.junit.Test

class FallbackSampleSizeTest {
    @Test fun withinTargetIsOne() = assertEquals(1, fallbackSampleSize(4000, 3000, 4096))
    @Test fun justBelowTwiceTargetIsOne() = assertEquals(1, fallbackSampleSize(8000, 6000, 4096))
    @Test fun twiceTargetIsTwo() = assertEquals(2, fallbackSampleSize(8192, 6144, 4096))
    @Test fun hugeImage() = assertEquals(4, fallbackSampleSize(16384, 12000, 4096))
    @Test fun portraitUsesLongestSide() = assertEquals(2, fallbackSampleSize(6000, 9000, 4096).coerceAtMost(2))
    @Test fun unknownBounds() = assertEquals(1, fallbackSampleSize(0, 0, 4096))
}
