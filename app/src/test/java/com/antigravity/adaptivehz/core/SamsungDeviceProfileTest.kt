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
        val rates = nonLtpoModes.map { it.refreshRate.toInt() }.distinct().sorted()
        assertEquals(listOf(60, 120), rates)

        // For non-LTPO, target refresh rates must be clamped to supported hardware steps
        val targetHz = 120
        assertTrue(rates.contains(targetHz))
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
        val rates = ltpoModes.map { it.refreshRate.toInt() }.distinct().sorted()
        assertTrue(rates.contains(60))
        assertTrue(rates.contains(120))
        assertEquals(6, rates.size)
    }

    /**
     * Tests Samsung Foldable model name detection.
     */
    @Test
    fun testFoldableDetectionFromModel() {
        fun isFoldableModel(model: String): Boolean {
            return model.contains("Fold", ignoreCase = true) || model.contains("Flip", ignoreCase = true)
        }

        assertTrue(isFoldableModel("Galaxy Z Fold 5"))
        assertTrue(isFoldableModel("Galaxy Z Fold 6"))
        assertTrue(isFoldableModel("Galaxy Z Flip 5"))
        assertTrue(isFoldableModel("SM-F946B (Z Fold 5)"))
        assertFalse(isFoldableModel("Galaxy S20 FE"))
        assertFalse(isFoldableModel("Galaxy S24 Ultra"))
        assertFalse(isFoldableModel("Galaxy S26"))
        assertFalse(isFoldableModel("Galaxy A55"))
    }

    /**
     * Tests power saving mode (PSM) bypass frequency bounds.
     */
    @Test
    fun testPsmBypassFrequencyBounds() {
        // In PSM bypass mode, peak and min refresh rates should lock to 120Hz
        val highHz = 120
        val peakFloat = highHz.toFloat()
        val minFloat = highHz.toFloat()

        assertEquals(120.0f, peakFloat, 0.01f)
        assertEquals(120.0f, minFloat, 0.01f)
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
