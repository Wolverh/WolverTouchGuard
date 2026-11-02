package com.wolver.touchguard

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.ViewConfiguration
import java.util.Locale
import kotlin.math.ln
import kotlin.math.roundToInt

data class DpiSensitivityProfile(
    val targetDpi: Int,
    val baseDpi: Int,
    val yAxisBoostPercent: Int,
    val horizontalMultiplier: Float,
    val verticalHeadshotMultiplier: Float,
    val combinedEffectiveDpi: Int,
    val nativeTouchSlopPx: Int,
    val effectiveTouchSlopPx: Float,
    val calculatedPointerSpeed: Int,
    val scaledCompositorDensityDpi: Int,
    val baseHardwareDensityDpi: Int,
    val isSystemDpiActive: Boolean,
    val activationMethod: String
)

object DpiSensitivityEngine {

    const val DEFAULT_BASE_DPI = 371
    private const val KEY_POINTER_SPEED = "pointer_speed"
    private const val KEY_TOUCH_SENSITIVITY = "touch_sensitivity"
    private const val KEY_GLOVE_MODE = "glove_mode"
    private const val KEY_GAME_TOUCH_SENSITIVITY = "game_touch_sensitivity"

    fun canWriteSystemSettings(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.System.canWrite(context)
        } else {
            true
        }
    }

    fun hasSecureSettingsPermission(context: Context): Boolean {
        return try {
            context.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) {
            false
        }
    }

    fun computeProfile(context: Context, prefManager: PrefManager): DpiSensitivityProfile {
        val targetDpi = prefManager.exactTargetDpi.coerceIn(320, 3000)
        val baseDpi = prefManager.exactBaseDpi.coerceIn(300, 800)
        val yBoostPct = prefManager.yAxisBoostPercent.coerceIn(0, 150)

        val xMult = (targetDpi.toFloat() / baseDpi.toFloat()).coerceIn(0.5f, 8.0f)
        val yMult = (xMult * (1.0f + (yBoostPct.toFloat() / 100.0f))).coerceIn(0.5f, 12.0f)
        val combinedDpi = (baseDpi * ((xMult + yMult) / 2.0f)).roundToInt()

        val nativeSlop = try {
            ViewConfiguration.get(context).scaledTouchSlop
        } catch (_: Exception) {
            24
        }
        val effectiveSlop = (nativeSlop.toFloat() / yMult).coerceAtLeast(1.0f)

        val widthPx = try {
            val dm = context.resources.displayMetrics
            kotlin.math.min(dm.widthPixels, dm.heightPixels).coerceAtLeast(1080)
        } catch (_: Exception) {
            1080
        }

        // Exact physical density calculation: density = widthPx * 160 / dp
        val targetHardwareDensity = (widthPx.toFloat() * 160f / targetDpi.toFloat()).roundToInt().coerceIn(120, 720)
        val baseHardwareDensity = (widthPx.toFloat() * 160f / baseDpi.toFloat()).roundToInt().coerceIn(120, 720)

        val pointerSpeedStep = if (yMult <= 1.0f) {
            0
        } else {
            val logSteps = (ln(yMult.toDouble()) / ln(1.18)).roundToInt()
            logSteps.coerceIn(1, 7)
        }

        val method = if (prefManager.isDpiAppliedToSystem) {
            if (hasSecureSettingsPermission(context)) "SecureSettings / wm density" else "Hardware Digitizer Boost"
        } else {
            "Standby"
        }

        return DpiSensitivityProfile(
            targetDpi = targetDpi,
            baseDpi = baseDpi,
            yAxisBoostPercent = yBoostPct,
            horizontalMultiplier = xMult,
            verticalHeadshotMultiplier = yMult,
            combinedEffectiveDpi = combinedDpi,
            nativeTouchSlopPx = nativeSlop,
            effectiveTouchSlopPx = effectiveSlop,
            calculatedPointerSpeed = pointerSpeedStep,
            scaledCompositorDensityDpi = targetHardwareDensity,
            baseHardwareDensityDpi = baseHardwareDensity,
            isSystemDpiActive = prefManager.isDpiAppliedToSystem,
            activationMethod = method
        )
    }

    fun formatTelemetryReport(profile: DpiSensitivityProfile): String {
        val xStr = String.format(Locale.US, "%.2fx", profile.horizontalMultiplier)
        val yStr = String.format(Locale.US, "%.2fx", profile.verticalHeadshotMultiplier)
        val statusText = if (profile.isSystemDpiActive) "⚡ ACTIVE (${profile.activationMethod})" else "⏸️ DEACTIVATED (Default: ${profile.baseDpi} DP)"
        return buildString {
            appendLine("STATUS: $statusText")
            appendLine("● TARGET DPI              : ${profile.targetDpi} DP (Default: ${profile.baseDpi} DP)")
            appendLine("● SENSITIVITY MULTIPLIER  : $xStr X-Axis • $yStr Y-Axis Headshot Drag")
            appendLine("● SYSTEM DENSITY (DPI)    : ${profile.scaledCompositorDensityDpi} dpi (Stock: ${profile.baseHardwareDensityDpi} dpi)")
            append("● HARDWARE DIGITIZER GAIN : +${profile.calculatedPointerSpeed} / 7 • Touch Slop: ${String.format(Locale.US, "%.1f", profile.effectiveTouchSlopPx)} px")
        }
    }

    private fun runCommand(cmd: Array<String>): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(cmd)
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (_: Exception) {
            false
        }
    }

    fun activateDpi(context: Context, prefManager: PrefManager): DpiSensitivityProfile {
        prefManager.isDpiEngineEnabled = true
        val profile = computeProfile(context, prefManager)

        // 1. Try Root su wm density
        val rootSuccess = runCommand(arrayOf("su", "-c", "wm density ${profile.scaledCompositorDensityDpi}"))

        // 2. Try Settings.Secure & Settings.Global
        var secureSuccess = false
        try {
            val resolver = context.contentResolver
            Settings.Secure.putString(resolver, "display_density_forced", profile.scaledCompositorDensityDpi.toString())
            Settings.Global.putString(resolver, "display_density_forced", profile.scaledCompositorDensityDpi.toString())
            secureSuccess = true
        } catch (_: Exception) {
        }

        // 3. Try direct shell exec wm density
        if (!rootSuccess && !secureSuccess) {
            runCommand(arrayOf("wm", "density", profile.scaledCompositorDensityDpi.toString()))
        }

        // 4. Scale DisplayMetrics on context
        try {
            val dm = context.resources.displayMetrics
            dm.xdpi = dm.xdpi * profile.horizontalMultiplier
            dm.ydpi = dm.ydpi * profile.verticalHeadshotMultiplier
        } catch (_: Exception) {
        }

        // 5. Apply OEM and System touch velocity & sampling flags
        if (canWriteSystemSettings(context)) {
            try {
                val resolver = context.contentResolver
                if (!prefManager.isDpiAppliedToSystem) {
                    val currentSpeed = try { Settings.System.getInt(resolver, KEY_POINTER_SPEED, 0) } catch (_: Exception) { 0 }
                    val currentTouchSens = try { Settings.System.getInt(resolver, KEY_TOUCH_SENSITIVITY, 0) } catch (_: Exception) { 0 }
                    prefManager.savedOriginalPointerSpeed = currentSpeed
                    prefManager.savedOriginalTouchSensitivity = currentTouchSens
                }
                Settings.System.putInt(resolver, KEY_POINTER_SPEED, profile.calculatedPointerSpeed)
                Settings.System.putInt(resolver, KEY_TOUCH_SENSITIVITY, 1)
                Settings.System.putInt(resolver, "high_touch_sensitivity_enable", 1)
                Settings.System.putInt(resolver, "touch_response_rate", 2)
                Settings.System.putInt(resolver, "transsion_touch_speed", 100)
                Settings.System.putInt(resolver, KEY_GLOVE_MODE, 1)
                Settings.System.putInt(resolver, KEY_GAME_TOUCH_SENSITIVITY, profile.combinedEffectiveDpi.coerceIn(360, 3000))
            } catch (_: Exception) {
            }
        }

        prefManager.isDpiAppliedToSystem = true
        return profile
    }

    fun deactivateDpi(context: Context, prefManager: PrefManager): DpiSensitivityProfile {
        prefManager.isDpiEngineEnabled = false
        val profile = computeProfile(context, prefManager)

        // 1. Try Root su wm density reset
        runCommand(arrayOf("su", "-c", "wm density reset"))

        // 2. Try Settings.Secure & Settings.Global reset
        try {
            val resolver = context.contentResolver
            Settings.Secure.putString(resolver, "display_density_forced", null)
            Settings.Global.putString(resolver, "display_density_forced", null)
        } catch (_: Exception) {
        }

        // 3. Try direct shell exec wm density reset
        runCommand(arrayOf("wm", "density", "reset"))

        // 4. Restore original system pointer speed and touch flags
        if (canWriteSystemSettings(context)) {
            try {
                val resolver = context.contentResolver
                Settings.System.putInt(resolver, KEY_POINTER_SPEED, prefManager.savedOriginalPointerSpeed.coerceIn(-7, 7))
                Settings.System.putInt(resolver, KEY_TOUCH_SENSITIVITY, prefManager.savedOriginalTouchSensitivity)
                Settings.System.putInt(resolver, KEY_GLOVE_MODE, 0)
                Settings.System.putInt(resolver, "high_touch_sensitivity_enable", 0)
            } catch (_: Exception) {
            }
        }

        prefManager.isDpiAppliedToSystem = false
        return profile
    }
}
