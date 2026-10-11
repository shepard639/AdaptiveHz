package com.healthcraft.adaptivehz

import com.healthcraft.adaptivehz.data.OperationMode
import com.healthcraft.adaptivehz.data.ProfileMode
import com.healthcraft.adaptivehz.data.SettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRepositoryTest {

    @Test
    fun testDefaultVideoAppsConfiguredFor60Hz() {
        // Verify key video streaming apps are locked to 60Hz by default to save battery
        val youtubeMode = SettingsRepository.DEFAULT_VIDEO_APPS["com.google.android.youtube"]
        val rutubeMode = SettingsRepository.DEFAULT_VIDEO_APPS["ru.rutube.app"]
        val vkVideoMode = SettingsRepository.DEFAULT_VIDEO_APPS["com.vk.video"]
        val kinopoiskMode = SettingsRepository.DEFAULT_VIDEO_APPS["ru.kinopoisk"]
        val cameraMode = SettingsRepository.DEFAULT_VIDEO_APPS["com.sec.android.app.camera"]

        assertEquals(ProfileMode.FORCE_60, youtubeMode)
        assertEquals(ProfileMode.FORCE_60, rutubeMode)
        assertEquals(ProfileMode.FORCE_60, vkVideoMode)
        assertEquals(ProfileMode.FORCE_60, kinopoiskMode)
        assertEquals(ProfileMode.FORCE_60, cameraMode)
    }

    @Test
    fun testProfileModesExist() {
        val modes = ProfileMode.entries
        assertTrue(modes.contains(ProfileMode.ADAPTIVE))
        assertTrue(modes.contains(ProfileMode.FORCE_60))
        assertTrue(modes.contains(ProfileMode.FORCE_120))
    }

    @Test
    fun testOperationModesExist() {
        val modes = OperationMode.entries
        assertEquals(4, modes.size)
        assertTrue(modes.contains(OperationMode.ADAPTIVE))
        assertTrue(modes.contains(OperationMode.FIXED_60))
        assertTrue(modes.contains(OperationMode.FIXED_120))
        assertTrue(modes.contains(OperationMode.ECO))
    }

    @Test
    fun testAllMajorRussianVideoAppsCovered() {
        assertTrue(SettingsRepository.DEFAULT_VIDEO_APPS.containsKey("ru.rutube.app"))
        assertTrue(SettingsRepository.DEFAULT_VIDEO_APPS.containsKey("com.vk.video"))
        assertTrue(SettingsRepository.DEFAULT_VIDEO_APPS.containsKey("ru.kinopoisk"))
    }
}
