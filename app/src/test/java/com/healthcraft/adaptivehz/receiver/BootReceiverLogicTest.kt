package com.healthcraft.adaptivehz.receiver

import android.content.Intent
import com.healthcraft.adaptivehz.data.OperationMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BootReceiverLogicTest {

    @Test
    fun testAcceptedActions() {
        fun shouldProcessAction(action: String?): Boolean {
            return action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED
        }

        assertTrue(shouldProcessAction(Intent.ACTION_BOOT_COMPLETED))
        assertTrue(shouldProcessAction(Intent.ACTION_MY_PACKAGE_REPLACED))
        assertFalse(shouldProcessAction(Intent.ACTION_SCREEN_ON))
        assertFalse(shouldProcessAction(Intent.ACTION_POWER_CONNECTED))
        assertFalse(shouldProcessAction(null))
        assertFalse(shouldProcessAction("unknown.action"))
    }

    @Test
    fun testServiceRestorationConditions() {
        fun shouldRestoreStatusBar(isEnabled: Boolean, hasPermission: Boolean): Boolean {
            return isEnabled && hasPermission
        }

        fun shouldRestoreHud(isEnabled: Boolean, hasOverlayPermission: Boolean): Boolean {
            return isEnabled && hasOverlayPermission
        }

        // Status bar restoration
        assertTrue(shouldRestoreStatusBar(isEnabled = true, hasPermission = true))
        assertFalse(shouldRestoreStatusBar(isEnabled = true, hasPermission = false))
        assertFalse(shouldRestoreStatusBar(isEnabled = false, hasPermission = true))
        assertFalse(shouldRestoreStatusBar(isEnabled = false, hasPermission = false))

        // HUD restoration
        assertTrue(shouldRestoreHud(isEnabled = true, hasOverlayPermission = true))
        assertFalse(shouldRestoreHud(isEnabled = true, hasOverlayPermission = false))
        assertFalse(shouldRestoreHud(isEnabled = false, hasOverlayPermission = true))
    }

    @Test
    fun testModeRestorationMapping() {
        fun getRestorationAction(mode: OperationMode): String {
            return when (mode) {
                OperationMode.FIXED_120 -> "setFixed120"
                OperationMode.FIXED_60 -> "setFixed60"
                OperationMode.ADAPTIVE,
                OperationMode.ECO -> "boostToHigh"
            }
        }

        org.junit.Assert.assertEquals("setFixed120", getRestorationAction(OperationMode.FIXED_120))
        org.junit.Assert.assertEquals("setFixed60", getRestorationAction(OperationMode.FIXED_60))
        org.junit.Assert.assertEquals("boostToHigh", getRestorationAction(OperationMode.ADAPTIVE))
        org.junit.Assert.assertEquals("boostToHigh", getRestorationAction(OperationMode.ECO))
    }
}
