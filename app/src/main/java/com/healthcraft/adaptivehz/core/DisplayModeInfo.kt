package com.healthcraft.adaptivehz.core

/**
 * Represents a hardware-supported display refresh rate mode.
 */
data class DisplayModeInfo(
    val modeId: Int,
    val width: Int,
    val height: Int,
    val refreshRateHz: Int,
    val exactRefreshRate: Float
)
