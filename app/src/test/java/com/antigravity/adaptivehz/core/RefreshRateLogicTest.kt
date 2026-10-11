package com.antigravity.adaptivehz.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshRateLogicTest {

    @Test
    fun testTargetHzSelectionLogic() {
        // High refresh rate should select Samsung high mode (1), low should select 0
        assertEquals(RefreshRateModeResolver.SAMSUNG_MODE_STANDARD, RefreshRateModeResolver.resolveSamsungMode(60))
        assertEquals(RefreshRateModeResolver.SAMSUNG_MODE_STANDARD, RefreshRateModeResolver.resolveSamsungMode(48))
        assertEquals(RefreshRateModeResolver.SAMSUNG_MODE_STANDARD, RefreshRateModeResolver.resolveSamsungMode(30))
        assertEquals(RefreshRateModeResolver.SAMSUNG_MODE_HIGH, RefreshRateModeResolver.resolveSamsungMode(96))
        assertEquals(RefreshRateModeResolver.SAMSUNG_MODE_HIGH, RefreshRateModeResolver.resolveSamsungMode(120))
        assertEquals(RefreshRateModeResolver.SAMSUNG_MODE_HIGH, RefreshRateModeResolver.resolveSamsungMode(144))
    }

    @Test
    fun testResolveHzFromSamsungMode() {
        assertEquals(60, RefreshRateModeResolver.resolveHzFromSamsungMode(RefreshRateModeResolver.SAMSUNG_MODE_STANDARD))
        assertEquals(120, RefreshRateModeResolver.resolveHzFromSamsungMode(RefreshRateModeResolver.SAMSUNG_MODE_HIGH))
        assertEquals(120, RefreshRateModeResolver.resolveHzFromSamsungMode(-1, fallbackHz = 120))
        assertEquals(60, RefreshRateModeResolver.resolveHzFromSamsungMode(999, fallbackHz = 60))
    }

    @Test
    fun testSupportedModesFilteringAndNormalization() {
        val rawFrequencies = listOf(60.0001f, 120.0004f, 60.0f, 96.0f, 120.0f, -1.0f, 0f)
        val distinctHz = RefreshRateModeResolver.normalizeFrequencies(rawFrequencies)

        assertEquals(listOf(60, 96, 120), distinctHz)
    }

    @Test
    fun testEmptyFrequenciesFallback() {
        val fallback = RefreshRateModeResolver.normalizeFrequencies(emptyList())
        assertEquals(listOf(60, 120), fallback)
    }

    @Test
    fun testClampTargetHz() {
        val supported = listOf(60, 96, 120)
        assertEquals(120, RefreshRateModeResolver.clampTargetHz(120, supported))
        assertEquals(96, RefreshRateModeResolver.clampTargetHz(96, supported))
        assertEquals(60, RefreshRateModeResolver.clampTargetHz(60, supported))
        // Closest match for unsupported 90Hz -> 96Hz
        assertEquals(96, RefreshRateModeResolver.clampTargetHz(90, supported))
        // Closest match for unsupported 50Hz -> 60Hz
        assertEquals(60, RefreshRateModeResolver.clampTargetHz(50, supported))
        // Empty supported rates fallback
        assertEquals(120, RefreshRateModeResolver.clampTargetHz(120, emptyList(), fallback = 120))
    }

    @Test
    fun testHighRefreshRateClassification() {
        assertFalse(RefreshRateModeResolver.isHighRefreshRate(60))
        assertFalse(RefreshRateModeResolver.isHighRefreshRate(48))
        assertTrue(RefreshRateModeResolver.isHighRefreshRate(90))
        assertTrue(RefreshRateModeResolver.isHighRefreshRate(96))
        assertTrue(RefreshRateModeResolver.isHighRefreshRate(120))
        assertTrue(RefreshRateModeResolver.isHighRefreshRate(144))
    }
}
