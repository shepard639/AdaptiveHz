package com.antigravity.adaptivehz.core

import com.antigravity.adaptivehz.data.OperationMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SamsungDeviceProfileTest {

    data class MockDisplayMode(val id: Int, val refreshRate: Float)

    /**
     * Tests logic for non-LTPO devices (like S20 FE, A52, A54) which only support discrete 60Hz and 120Hz modes.
     */
    @Test
    fun testNonLtpoDiscreteModes() {
        val nonLtpoModes = listOf(
            MockDisplayMode(1, 60.0f),
            MockDisplayMode(2, 120.0f)
        )
        val rates = RefreshRateModeResolver.normalizeFrequencies(nonLtpoModes.map { it.refreshRate })
        assertEquals(listOf(60, 120), rates)

        // Target refresh rates must match supported hardware steps
        val clamped = RefreshRateModeResolver.clampTargetHz(120, rates)
        assertEquals(120, clamped)
    }

    /**
     * Tests logic for modern LTPO flagships (like S24 Ultra, S25, S26) which report dynamic multi-step frequencies.
     */
    @Test
    fun testLtpoMultiStepModes() {
        val ltpoModes = listOf(
            MockDisplayMode(1, 24.0f),
            MockDisplayMode(2, 30.0f),
            MockDisplayMode(3, 48.0f),
            MockDisplayMode(4, 60.0f),
            MockDisplayMode(5, 96.0f),
            MockDisplayMode(6, 120.0f)
        )
        val rates = RefreshRateModeResolver.normalizeFrequencies(ltpoModes.map { it.refreshRate })
        assertTrue(rates.contains(60))
        assertTrue(rates.contains(120))
        assertEquals(6, rates.size)
        assertEquals(listOf(24, 30, 48, 60, 96, 120), rates)
    }

    /**
     * Tests Samsung Foldable model name and hinge sensor detection directly via production RefreshRateModeResolver.
     */
    @Test
    fun testFoldableDetectionFromModelAndSensors() {
        // Model string detection
        assertTrue(RefreshRateModeResolver.isFoldableDevice("Galaxy Z Fold 5"))
        assertTrue(RefreshRateModeResolver.isFoldableDevice("Galaxy Z Fold 6"))
        assertTrue(RefreshRateModeResolver.isFoldableDevice("Galaxy Z Flip 5"))
        assertTrue(RefreshRateModeResolver.isFoldableDevice("SM-F946B (Z Fold 5)"))
        assertTrue(RefreshRateModeResolver.isFoldableDevice("SM-F731B"))

        // Non-foldable models
        assertFalse(RefreshRateModeResolver.isFoldableDevice("Galaxy S20 FE"))
        assertFalse(RefreshRateModeResolver.isFoldableDevice("Galaxy S24 Ultra"))
        assertFalse(RefreshRateModeResolver.isFoldableDevice("Galaxy S26"))
        assertFalse(RefreshRateModeResolver.isFoldableDevice("Galaxy A55"))
        assertFalse(RefreshRateModeResolver.isFoldableDevice(""))

        // Hinge sensor presence overrides model name
        assertTrue(RefreshRateModeResolver.isFoldableDevice("Generic Device", hasHingeSensor = true))
    }

    /**
     * Tests power saving mode (PSM) bypass frequency bounds via production resolver.
     */
    @Test
    fun testPsmBypassFrequencyBounds() {
        val (peak, min) = RefreshRateModeResolver.calculatePsmBypassParameters(120)
        assertEquals(120.0f, peak, 0.01f)
        assertEquals(120.0f, min, 0.01f)

        val (peak96, min96) = RefreshRateModeResolver.calculatePsmBypassParameters(96)
        assertEquals(96.0f, peak96, 0.01f)
        assertEquals(96.0f, min96, 0.01f)

        // Invalid input fallback
        val (peakDefault, minDefault) = RefreshRateModeResolver.calculatePsmBypassParameters(0)
        assertEquals(120.0f, peakDefault, 0.01f)
        assertEquals(120.0f, minDefault, 0.01f)
    }

    /**
     * Tests all 4 operation modes transitions.
     */
    @Test
    fun testAllOperationModesCoverage() {
        val modes = OperationMode.entries
        assertEquals(4, modes.size)
        assertTrue(modes.contains(OperationMode.ADAPTIVE))
        assertTrue(modes.contains(OperationMode.ECO))
        assertTrue(modes.contains(OperationMode.FIXED_60))
        assertTrue(modes.contains(OperationMode.FIXED_120))
    }
}
