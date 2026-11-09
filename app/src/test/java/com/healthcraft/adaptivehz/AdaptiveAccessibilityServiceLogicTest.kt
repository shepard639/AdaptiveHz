package com.healthcraft.adaptivehz

import com.healthcraft.adaptivehz.data.ProfileMode
import com.healthcraft.adaptivehz.data.SettingsRepository
import com.healthcraft.adaptivehz.service.AdaptiveAccessibilityService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveAccessibilityServiceLogicTest {

    @Test
    fun testStaticIgnoredPackagesContainKeyboardsAndSystemUi() {
        val ignored = AdaptiveAccessibilityService.STATIC_IGNORED_PACKAGES

        // Keyboards
        assertTrue(ignored.contains("com.samsung.android.honeyboard"))
        assertTrue(ignored.contains("com.google.android.inputmethod.latin"))
        assertTrue(ignored.contains("com.touchtype.swiftkey"))
        assertTrue(ignored.contains("ru.yandex.keyboard"))

        // System overlays & OS
        assertTrue(ignored.contains("android"))
        assertTrue(ignored.contains("com.android.systemui"))
        assertTrue(ignored.contains("com.google.android.gms"))
        assertTrue(ignored.contains("com.samsung.android.app.cocktailbarservice"))
        assertTrue(ignored.contains("com.samsung.android.app.smartcapture"))
        assertTrue(ignored.contains("com.samsung.android.incallui"))
        assertTrue(ignored.contains("com.samsung.android.game.gametools"))

        // Legitimate user apps MUST NOT be ignored
        assertFalse(ignored.contains("com.google.android.youtube"))
        assertFalse(ignored.contains("ru.rutube.app"))
        assertFalse(ignored.contains("com.vk.video"))
        assertFalse(ignored.contains("com.sec.android.app.launcher"))
        assertFalse(ignored.contains("com.android.chrome"))
        assertFalse(ignored.contains("org.telegram.messenger"))
    }

    @Test
    fun testForegroundAppTransitionWithSystemOverlaySimulation() {
        // Simulation of user opening YouTube, followed by keyboard / system UI popping up
        var currentForeground = "com.sec.android.app.launcher"
        var currentMode = ProfileMode.ADAPTIVE

        fun simulateEvent(pkg: String, isWindowStateChanged: Boolean) {
            val isIgnored = AdaptiveAccessibilityService.STATIC_IGNORED_PACKAGES.contains(pkg) ||
                    pkg.isEmpty() || pkg == "com.healthcraft.adaptivehz"

            if (!isIgnored && pkg != currentForeground) {
                currentForeground = pkg
                currentMode = SettingsRepository.DEFAULT_VIDEO_APPS[pkg] ?: ProfileMode.ADAPTIVE
            }
        }

        // 1. User launches YouTube from Samsung Launcher
        simulateEvent("com.google.android.youtube", isWindowStateChanged = true)
        assertEquals("com.google.android.youtube", currentForeground)
        assertEquals(ProfileMode.FORCE_60, currentMode)

        // 2. YouTube opens search -> Samsung Honeyboard keyboard pops up
        simulateEvent("com.samsung.android.honeyboard", isWindowStateChanged = true)
        // Foreground app MUST NOT be hijacked by the keyboard
        assertEquals("com.google.android.youtube", currentForeground)
        assertEquals(ProfileMode.FORCE_60, currentMode)

        // 3. User receives a WhatsApp notification / pulls notification shade
        simulateEvent("com.android.systemui", isWindowStateChanged = true)
        assertEquals("com.google.android.youtube", currentForeground)
        assertEquals(ProfileMode.FORCE_60, currentMode)

        // 4. User touches/scrolls inside YouTube (dual-path check)
        simulateEvent("com.google.android.youtube", isWindowStateChanged = false)
        assertEquals("com.google.android.youtube", currentForeground)
        assertEquals(ProfileMode.FORCE_60, currentMode)

        // 5. User exits YouTube and returns to Samsung Launcher
        simulateEvent("com.sec.android.app.launcher", isWindowStateChanged = true)
        assertEquals("com.sec.android.app.launcher", currentForeground)
        assertEquals(ProfileMode.ADAPTIVE, currentMode)
    }

    @Test
    fun testScrollThrottlingWindow() {
        val throttleMs = AdaptiveAccessibilityService.SCROLL_THROTTLE_MS
        assertEquals(350L, throttleMs)

        var lastScheduleTime = 1000L
        val incomingScroll1 = 1100L // 100ms later -> should be throttled
        assertTrue(incomingScroll1 - lastScheduleTime < throttleMs)

        val incomingScroll2 = 1400L // 400ms later -> should trigger reset
        assertFalse(incomingScroll2 - lastScheduleTime < throttleMs)
    }
}
