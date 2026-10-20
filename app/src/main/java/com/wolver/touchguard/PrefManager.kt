package com.wolver.touchguard

import android.content.Context
import android.content.SharedPreferences

data class ZoneConfig(
    val id: Int,
    val title: String,
    var enabled: Boolean,
    var x: Int,
    var y: Int,
    var width: Int,
    var height: Int
)

class PrefManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "wolver_touch_guard_prefs"

        const val MODE_LOCK = 0
        const val MODE_EDIT = 1
        const val MODE_OFF = 2

        const val TOTAL_ZONES = 4

        private const val KEY_MASTER_ENABLED = "master_enabled"
        private const val KEY_APP_FILTER_ENABLED = "app_filter_enabled"
        private const val KEY_SELECTED_APPS = "selected_apps"
        private const val KEY_CURRENT_MODE = "current_mode"
        private const val KEY_EDIT_OPACITY = "edit_opacity"
        private const val KEY_LOCK_BORDER = "lock_border"
        private const val KEY_HIDE_PILL_IN_LOCK = "hide_pill_in_lock"
        private const val KEY_PILL_HIDDEN_MANUALLY = "pill_hidden_manually"
        private const val KEY_PILL_X = "pill_x"
        private const val KEY_PILL_Y = "pill_y"
        private const val KEY_PILL_COLLAPSED = "pill_collapsed"

        val DEFAULT_TARGET_PACKAGES = setOf(
            "com.dts.freefireth",
            "com.dts.freefiremax"
        )
    }

    var isMasterEnabled: Boolean
        get() = prefs.getBoolean(KEY_MASTER_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_MASTER_ENABLED, value).apply()

    var isAppFilterEnabled: Boolean
        get() = prefs.getBoolean(KEY_APP_FILTER_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_APP_FILTER_ENABLED, value).apply()

    var selectedPackages: Set<String>
        get() {
            val saved = prefs.getStringSet(KEY_SELECTED_APPS, null)
            return if (saved == null) {
                DEFAULT_TARGET_PACKAGES
            } else {
                HashSet(saved)
            }
        }
        set(value) {
            prefs.edit().putStringSet(KEY_SELECTED_APPS, HashSet(value)).apply()
        }

    var currentMode: Int
        get() = prefs.getInt(KEY_CURRENT_MODE, MODE_EDIT)
        set(value) = prefs.edit().putInt(KEY_CURRENT_MODE, value).apply()

    var editOpacityPercent: Int
        get() = prefs.getInt(KEY_EDIT_OPACITY, 40).coerceIn(15, 85)
        set(value) = prefs.edit().putInt(KEY_EDIT_OPACITY, value.coerceIn(15, 85)).apply()

    var showLockBorder: Boolean
        get() = prefs.getBoolean(KEY_LOCK_BORDER, false)
        set(value) = prefs.edit().putBoolean(KEY_LOCK_BORDER, value).apply()

    var hidePillInLockMode: Boolean
        get() = prefs.getBoolean(KEY_HIDE_PILL_IN_LOCK, false)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_PILL_IN_LOCK, value).apply()

    var isPillHiddenManually: Boolean
        get() = prefs.getBoolean(KEY_PILL_HIDDEN_MANUALLY, false)
        set(value) = prefs.edit().putBoolean(KEY_PILL_HIDDEN_MANUALLY, value).apply()

    var pillX: Int
        get() = prefs.getInt(KEY_PILL_X, 950)
        set(value) = prefs.edit().putInt(KEY_PILL_X, value).apply()

    var pillY: Int
        get() = prefs.getInt(KEY_PILL_Y, 24)
        set(value) = prefs.edit().putInt(KEY_PILL_Y, value).apply()

    var isPillCollapsed: Boolean
        get() = prefs.getBoolean(KEY_PILL_COLLAPSED, false)
        set(value) = prefs.edit().putBoolean(KEY_PILL_COLLAPSED, value).apply()

    fun isZoneEnabled(zoneId: Int): Boolean {
        val defaultOn = zoneId <= 3
        return prefs.getBoolean("zone_${zoneId}_enabled", defaultOn)
    }

    fun setZoneEnabled(zoneId: Int, enabled: Boolean) {
        prefs.edit().putBoolean("zone_${zoneId}_enabled", enabled).apply()
    }

    fun getZoneConfig(zoneId: Int): ZoneConfig {
        val defaultX: Int
        val defaultY: Int
        val defaultW: Int
        val defaultH: Int
        val title: String

        when (zoneId) {
            1 -> {
                title = "Z1: Left Edge & Palm"
                defaultX = 0
                defaultY = 0
                defaultW = 220
                defaultH = 600
            }
            2 -> {
                title = "Z2: Top Bar & Call Guard"
                defaultX = 220
                defaultY = 0
                defaultW = 1650
                defaultH = 85
            }
            3 -> {
                title = "Z3: Below 3rd-Finger"
                defaultX = 220
                defaultY = 270
                defaultW = 420
                defaultH = 330
            }
            else -> {
                title = "Z4: Right of 3rd-Finger"
                defaultX = 460
                defaultY = 85
                defaultW = 380
                defaultH = 515
            }
        }

        return ZoneConfig(
            id = zoneId,
            title = title,
            enabled = isZoneEnabled(zoneId),
            x = prefs.getInt("zone_${zoneId}_x", defaultX).coerceAtLeast(0),
            y = prefs.getInt("zone_${zoneId}_y", defaultY).coerceAtLeast(0),
            width = prefs.getInt("zone_${zoneId}_w", defaultW).coerceAtLeast(40),
            height = prefs.getInt("zone_${zoneId}_h", defaultH).coerceAtLeast(35)
        )
    }

    fun saveZoneBounds(zoneId: Int, x: Int, y: Int, width: Int, height: Int) {
        prefs.edit()
            .putInt("zone_${zoneId}_x", x.coerceAtLeast(0))
            .putInt("zone_${zoneId}_y", y.coerceAtLeast(0))
            .putInt("zone_${zoneId}_w", width.coerceAtLeast(40))
            .putInt("zone_${zoneId}_h", height.coerceAtLeast(35))
            .apply()
    }

    fun resetZonesToDefaults() {
        val editor = prefs.edit()
        for (id in 1..TOTAL_ZONES) {
            editor.remove("zone_${id}_x")
            editor.remove("zone_${id}_y")
            editor.remove("zone_${id}_w")
            editor.remove("zone_${id}_h")
            editor.putBoolean("zone_${id}_enabled", id <= 3)
        }
        editor.putInt(KEY_PILL_X, 950)
        editor.putInt(KEY_PILL_Y, 24)
        editor.putBoolean(KEY_PILL_COLLAPSED, false)
        editor.putBoolean(KEY_PILL_HIDDEN_MANUALLY, false)
        editor.apply()
    }
}
