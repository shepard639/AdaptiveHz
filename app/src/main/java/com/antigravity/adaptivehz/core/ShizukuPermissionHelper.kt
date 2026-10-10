package com.antigravity.adaptivehz.core

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.reflect.Method

/**
 * Helper class to interact with Shizuku for on-device privilege management.
 * Allows granting WRITE_SECURE_SETTINGS without requiring a PC or USB cable.
 */
object ShizukuPermissionHelper {

    private const val TAG = "ShizukuHelper"
    const val SHIZUKU_REQUEST_CODE = 4001

    enum class ShizukuState {
        NOT_RUNNING,       // Shizuku service is not active
        PERMISSION_NEEDED, // Shizuku is running, but app is not authorized
        READY,             // Shizuku is running and authorized
        UNAVAILABLE        // Binder error or Shizuku not installed
    }

    /**
     * Checks current status of Shizuku service and app permissions.
     */
    fun checkShizukuState(): ShizukuState {
        return try {
            if (!Shizuku.pingBinder()) {
                ShizukuState.NOT_RUNNING
            } else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                ShizukuState.READY
            } else {
                ShizukuState.PERMISSION_NEEDED
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error checking Shizuku state: ${e.message}")
            ShizukuState.UNAVAILABLE
        }
    }

    /**
     * Requests authorization from Shizuku service.
     */
    fun requestPermission(requestCode: Int = SHIZUKU_REQUEST_CODE) {
        try {
            if (Shizuku.pingBinder()) {
                Shizuku.requestPermission(requestCode)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to request Shizuku permission", e)
        }
    }

    /**
     * Executes shell command via Shizuku privileged process.
     */
    private fun executeShizukuProcess(command: String): Process {
        val method: Method = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        )
        method.isAccessible = true
        val cmdArray = arrayOf("sh", "-c", command)
        return method.invoke(null, cmdArray, null, null) as Process
    }

    /**
     * Executes `pm grant <package> android.permission.WRITE_SECURE_SETTINGS` via Shizuku process.
     */
    suspend fun grantWriteSecureSettings(context: Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!Shizuku.pingBinder()) {
                return@withContext Result.failure(IllegalStateException("Shizuku service is not running"))
            }

            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                return@withContext Result.failure(SecurityException("Shizuku permission not granted by user"))
            }

            val packageName = context.packageName
            val cmd = "pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"
            Log.d(TAG, "Executing command via Shizuku: $cmd")

            val process = executeShizukuProcess(cmd)
            val finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return@withContext Result.failure(java.util.concurrent.TimeoutException("Shizuku command timed out after 5 seconds"))
            }
            val exitCode = process.exitValue()

            val errorOutput = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }
            val stdOutput = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }

            if (exitCode == 0) {
                Log.i(TAG, "WRITE_SECURE_SETTINGS granted successfully via Shizuku!")
                Result.success("Permission granted successfully")
            } else {
                val msg = if (errorOutput.isNotBlank()) errorOutput.trim() else "Exit code: $exitCode, $stdOutput"
                Log.e(TAG, "Shizuku command failed: $msg")
                Result.failure(RuntimeException(msg))
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Exception during Shizuku command execution", e)
            Result.failure(e)
        }
    }
}
