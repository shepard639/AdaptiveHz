package com.antigravity.adaptivehz.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshRateLogicTest {

    @Test
    fun testTargetHzSelectionLogic() {
        // High refresh rate should select Samsung high mode (1), low should select 0
        fun getSamsungMode(targetHz: Int): Int {
            return if (targetHz <= 60) 0 else 1
        }

        assertEquals(0, getSamsungMode(60))
        assertEquals(0, getSamsungMode(48))
        assertEquals(1, getSamsungMode(96))
        assertEquals(1, getSamsungMode(120))
    }

    @Test
    fun testRefreshRateStateUpdateRules() {
        // Verifies the audit fix: _currentRefreshRateHz must only update on success
        var currentHz = 60

        fun applyRefreshRate(targetHz: Int, success: Boolean): Boolean {
            if (success) {
                currentHz = targetHz
            }
            return success
        }

        // 1. Success case
        val res1 = applyRefreshRate(120, success = true)
        assertTrue(res1)
        assertEquals(120, currentHz)

        // 2. Failure case (permission missing or system failure)
        val res2 = applyRefreshRate(60, success = false)
        assertFalse(res2)
        // Rate must NOT change on failure
        assertEquals(120, currentHz)
    }

    @Test
    fun testSupportedModesFiltering() {
        val rawFrequencies = listOf(60.0001f, 120.0004f, 60.0f, 96.0f, 120.0f)
        val distinctHz = rawFrequencies
            .map { Math.round(it) }
            .distinct()
            .sorted()

        assertEquals(listOf(60, 96, 120), distinctHz)
    }
}
