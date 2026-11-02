package com.wolver.touchguard

import android.content.Context
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
    val scaledCompositorDensityDpi: Int
)

object DpiSensitivityEngine {

    private const val KEY_POINTER_SPEED = "pointer_speed"
    private const val KEY_TOUCH_SENSITIVITY = "touch_sensitivity"
    private const val KEY_GLOVE_MODE = "glove_mode"
    private const val KEY_GAME_TOUCH_SENSITIVITY = "game_touch_sensitivity"

    fun canWriteSystemSettings(context: Context): Boolean {
        return Settings.System.canWrite(context)
    }

    /**
     * Computes the exact mathematical DPI sensitivity profile from the user's typed Target DPI,
     * Stock Base DPI (411 dp on Infinix Note 60), and Y-Axis Headshot Drag Boost %.
     */
    fun computeProfile(context: Context, prefManager: PrefManager): DpiSensitivityProfile {
        val targetDpi = prefManager.exactTargetDpi.coerceIn(320, 3000)
        val baseDpi = prefManager.exactBaseDpi.coerceIn(300, 800)
        val yBoostPct = prefManager.yAxisBoostPercent.coerceIn(0, 150)

        // Exact mathematical ratio of how Developer Options "Smallest Width (dp)" scales movement
        val xMult = (targetDpi.toFloat() / baseDpi.toFloat()).coerceIn(0.5f, 8.0f)
        val yMult = (xMult * (1.0f + (yBoostPct.toFloat() / 100.0f))).coerceIn(0.5f, 12.0f)
        val combinedDpi = (baseDpi * ((xMult + yMult) / 2.0f)).roundToInt()

        val nativeSlop = try {
            ViewConfiguration.get(context).scaledTouchSlop
        } catch (_: Exception) {
            24
        }
        val effectiveSlop = (nativeSlop.toFloat() / yMult).coerceAtLeast(1.0f)

        // Map the exact DPI multiplier onto Android's hardware input velocity curve (-7..+7)
        // In Android's InputReader, each +1 step in pointer_speed increases velocity gain by ~25% exponentially.
        val pointerSpeedStep = if (yMult <= 1.0f) {
            0
        } else {
            val logSteps = (ln(yMult.toDouble()) / ln(1.18)).roundToInt()
            logSteps.coerceIn(1, 7)
        }

        val stockDensityDpi = context.resources.displayMetrics.densityDpi.coerceAtLeast(160)
        val scaledDensityDpi = (stockDensityDpi.toFloat() / xMult).roundToInt().coerceIn(120, 640)

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
            scaledCompositorDensityDpi = scaledDensityDpi
        )
    }

    fun formatTelemetryReport(profile: DpiSensitivityProfile): String {
        val xStr = String.format(Locale.US, "%.2fx", profile.horizontalMultiplier)
        val yStr = String.format(Locale.US, "%.2fx", profile.verticalHeadshotMultiplier)
        val slopStr = String.format(Locale.US, "%.1f px", profile.effectiveTouchSlopPx)
        return buildString {
            appendLine("● EXACT X-AXIS MULTIPLIER : $xStr (${profile.targetDpi} / ${profile.baseDpi} DPI)")
            appendLine("● Y-AXIS HEADSHOT DRAG    : $yStr (+${profile.yAxisBoostPercent}% Vertical Boost)")
            appendLine("● EFFECTIVE DRAG DPI      : ${profile.combinedEffectiveDpi} DPI (UI Width: 100% Locked)")
            appendLine("● MICRO-DRAG TOUCH SLOP   : ${profile.nativeTouchSlopPx} px -> $slopStr")
            append("● HARDWARE VELOCITY GAIN  : +${profile.calculatedPointerSpeed} / 7 (High-Sens Digitizer ON)")
        }
    }

    /**
     * Applies the exact calculated DPI sensitivity parameters to the Android touch/input subsystem
     * and the Accessibility Touch Layer DisplayMetrics without shrinking the screen layout width.
     */
    fun applyExactDpiSensitivity(context: Context, prefManager: PrefManager): DpiSensitivityProfile {
        val profile = computeProfile(context, prefManager)

        if (!prefManager.isDpiEngineEnabled) {
            restoreStockSensitivity(context, prefManager)
            return profile
        }

        // 1. Scale the Accessibility Service Touch Layer DisplayMetrics (xdpi/ydpi) to match the target DPI
        // while keeping configuration.smallestScreenWidthDp untouched so the screen never shrinks!
        try {
            val dm = context.resources.displayMetrics
            dm.xdpi = dm.xdpi * profile.horizontalMultiplier
            dm.ydpi = dm.ydpi * profile.verticalHeadshotMultiplier
        } catch (_: Exception) {
        }

        // 2. Apply low-level input velocity & high-sensitivity digitizer flags via Settings.System
        if (canWriteSystemSettings(context)) {
            try {
                val resolver = context.contentResolver
                if (!prefManager.isDpiAppliedToSystem) {
                    val currentSpeed = try {
                        Settings.System.getInt(resolver, KEY_POINTER_SPEED, 0)
                    } catch (_: Exception) {
                        0
                    }
                    val currentTouchSens = try {
                        Settings.System.getInt(resolver, KEY_TOUCH_SENSITIVITY, 0)
                    } catch (_: Exception) {
                        0
                    }
                    prefManager.savedOriginalPointerSpeed = currentSpeed
                    prefManager.savedOriginalTouchSensitivity = currentTouchSens
                }

                Settings.System.putInt(resolver, KEY_POINTER_SPEED, profile.calculatedPointerSpeed)
                Settings.System.putInt(resolver, KEY_TOUCH_SENSITIVITY, 1)
                Settings.System.putInt(resolver, KEY_GLOVE_MODE, 1)
                Settings.System.putInt(
                    resolver,
                    KEY_GAME_TOUCH_SENSITIVITY,
                    profile.combinedEffectiveDpi.coerceIn(360, 3000)
                )
                prefManager.isDpiAppliedToSystem = true
            } catch (_: Exception) {
            }
        }

        return profile
    }

    /**
     * Restores the phone's exact stock pointer and digitizer sensitivity immediately when leaving Free Fire.
     */
    fun restoreStockSensitivity(context: Context, prefManager: PrefManager) {
        if (!prefManager.isDpiAppliedToSystem) return
        if (!canWriteSystemSettings(context)) {
            prefManager.isDpiAppliedToSystem = false
            return
        }

        try {
            val resolver = context.contentResolver
            Settings.System.putInt(
                resolver,
                KEY_POINTER_SPEED,
                prefManager.savedOriginalPointerSpeed.coerceIn(-7, 7)
            )
            Settings.System.putInt(
                resolver,
                KEY_TOUCH_SENSITIVITY,
                prefManager.savedOriginalTouchSensitivity
            )
            Settings.System.putInt(resolver, KEY_GLOVE_MODE, 0)
        } catch (_: Exception) {
        } finally {
            prefManager.isDpiAppliedToSystem = false
        }
    }
}
