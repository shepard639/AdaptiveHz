package com.antigravity.adaptivehz.core

import kotlin.math.roundToInt

/**
 * Pure domain logic resolver for Samsung One UI display frequencies and device profiles.
 * Free of Android framework lifecycle and Context dependencies for fast, deterministic unit testing.
 */
object RefreshRateModeResolver {

    const val SAMSUNG_MODE_STANDARD = 0 // 60Hz
    const val SAMSUNG_MODE_HIGH = 1     // 120Hz (or higher adaptive step)

    /**
     * Maps target refresh rate to Samsung's proprietary `refresh_rate_mode` (0 = 60Hz, 1 = 120Hz).
     */
    fun resolveSamsungMode(targetHz: Int): Int {
        return if (targetHz <= 60) SAMSUNG_MODE_STANDARD else SAMSUNG_MODE_HIGH
    }

    /**
     * Resolves refresh rate in Hz from Samsung's proprietary mode integer.
     */
    fun resolveHzFromSamsungMode(samsungMode: Int, fallbackHz: Int = 120): Int {
        return when (samsungMode) {
            SAMSUNG_MODE_STANDARD -> 60
            SAMSUNG_MODE_HIGH -> 120
            else -> fallbackHz
        }
    }

    /**
     * Determines whether a device model represents a Samsung Foldable (Fold / Flip).
     */
    fun isFoldableDevice(model: String, hasHingeSensor: Boolean = false): Boolean {
        if (hasHingeSensor) return true
        if (model.isBlank()) return false
        val cleanModel = model.trim()
        return cleanModel.contains("Fold", ignoreCase = true) ||
                cleanModel.contains("Flip", ignoreCase = true) ||
                cleanModel.matches(Regex(".*SM-[Ff]\\d{3}.*"))
    }

    /**
     * Normalizes and cleans raw hardware refresh rate values (e.g. 60.0001f, 120.0004f, 96.0f)
     * into sorted, distinct integer frequencies in Hz.
     */
    fun normalizeFrequencies(rawRates: List<Float>): List<Int> {
        val distinct = rawRates
            .map { it.roundToInt() }
            .filter { it > 0 }
            .distinct()
            .sorted()
        return if (distinct.isNotEmpty()) distinct else listOf(60, 120)
    }

    /**
     * Validates and clamps a requested refresh rate against the device's supported hardware rates.
     */
    fun clampTargetHz(requestedHz: Int, supportedRates: List<Int>, fallback: Int = 120): Int {
        if (supportedRates.isEmpty()) return fallback
        if (supportedRates.contains(requestedHz)) return requestedHz

        return supportedRates.minByOrNull { kotlin.math.abs(it - requestedHz) } ?: fallback
    }

    /**
     * Computes the peak and min refresh rate floats for One UI power saving bypass.
     */
    fun calculatePsmBypassParameters(targetHz: Int): Pair<Float, Float> {
        val validHz = if (targetHz > 0) targetHz.toFloat() else 120.0f
        return Pair(validHz, validHz)
    }

    /**
     * Checks if a given frequency is considered high (>= 90Hz).
     */
    fun isHighRefreshRate(hz: Int): Boolean {
        return hz >= 90
    }
}
