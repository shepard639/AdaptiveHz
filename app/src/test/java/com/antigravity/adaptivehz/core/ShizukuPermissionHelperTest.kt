package com.antigravity.adaptivehz.core

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShizukuPermissionHelperTest {

    @Test
    fun testShizukuStateEnumsExist() {
        val states = ShizukuPermissionHelper.ShizukuState.entries
        assertEquals(4, states.size)
        assertNotNull(ShizukuPermissionHelper.ShizukuState.valueOf("READY"))
        assertNotNull(ShizukuPermissionHelper.ShizukuState.valueOf("NOT_RUNNING"))
        assertNotNull(ShizukuPermissionHelper.ShizukuState.valueOf("PERMISSION_NEEDED"))
        assertNotNull(ShizukuPermissionHelper.ShizukuState.valueOf("UNAVAILABLE"))
    }

    @Test
    fun testShizukuStateHandlingWithoutBinder() {
        // In JVM test environment without Shizuku service, checkShizukuState returns NOT_RUNNING or UNAVAILABLE safely
        val state = ShizukuPermissionHelper.checkShizukuState()
        assertTrue(
            state == ShizukuPermissionHelper.ShizukuState.NOT_RUNNING ||
                    state == ShizukuPermissionHelper.ShizukuState.UNAVAILABLE
        )
    }
}
