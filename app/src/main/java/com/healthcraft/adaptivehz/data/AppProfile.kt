package com.healthcraft.adaptivehz.data

enum class ProfileMode {
    ADAPTIVE,    // 120Hz on interaction, drops to 60Hz when idle
    FORCE_60,    // Lock to 60Hz while foregrounded (ideal for video apps)
    FORCE_120    // Always keep at 120Hz
}

data class AppProfile(
    val packageName: String,
    val appName: String,
    val mode: ProfileMode = ProfileMode.ADAPTIVE,
    val isSystemApp: Boolean = false
)
