package com.wolver.touchguard

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class ZoneConfig(
    val id: Int,
    val title: String,
    var enabled: Boolean,
    var x: Int,
    var y: Int,
    var width: Int,
    var height: Int
)

class PrefManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "wolver_touch_guard_prefs"
        private const val BACKUP_FILENAME = "wolver_touchguard_backup_v4.json"

        const val MODE_LOCK = 0
        const val MODE_EDIT = 1
        const val MODE_OFF = 2

        const val TOTAL_ZONES = 6

        private const val KEY_MASTER_ENABLED = "master_enabled"
        private const val KEY_APP_FILTER_ENABLED = "app_filter_enabled"
        private const val KEY_AUTO_LANDSCAPE_ENABLE = "auto_landscape_enable"
        private const val KEY_AUTO_LOCK_ON_GAME_LAUNCH = "auto_lock_on_game_launch"
        private const val KEY_SELECTED_APPS = "selected_apps"
        private const val KEY_CURRENT_MODE = "current_mode"
        private const val KEY_EDIT_OPACITY = "edit_opacity"
        private const val KEY_LOCK_BORDER = "lock_border"
        private const val KEY_HIDE_PILL_IN_LOCK = "hide_pill_in_lock"
        private const val KEY_HIDE_TOGGLE_ALWAYS = "hide_toggle_always"
        private const val KEY_SHOW_OVERLAY_IN_APP = "show_overlay_in_app"
        private const val KEY_PILL_HIDDEN_MANUALLY = "pill_hidden_manually"
        private const val KEY_PILL_X = "pill_x"
        private const val KEY_PILL_Y = "pill_y"
        private const val KEY_PILL_COLLAPSED = "pill_collapsed"

        val DEFAULT_TARGET_PACKAGES = setOf(
            "com.dts.freefireth",
            "com.dts.freefiremax"
        )
    }

    init {
        if (!prefs.contains(KEY_MASTER_ENABLED)) {
            restoreBackupFromFile()
        }
    }

    var isMasterEnabled: Boolean
        get() = prefs.getBoolean(KEY_MASTER_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_MASTER_ENABLED, value).apply()
            saveBackupToFile()
        }

    var isAppFilterEnabled: Boolean
        get() = prefs.getBoolean(KEY_APP_FILTER_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_APP_FILTER_ENABLED, value).apply()
            saveBackupToFile()
        }

    var isAutoLandscapeEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_LANDSCAPE_ENABLE, true)
        set(value) {
            prefs.edit().putBoolean(KEY_AUTO_LANDSCAPE_ENABLE, value).apply()
            saveBackupToFile()
        }

    var isAutoLockOnGameLaunch: Boolean
        get() = prefs.getBoolean(KEY_AUTO_LOCK_ON_GAME_LAUNCH, true)
        set(value) {
            prefs.edit().putBoolean(KEY_AUTO_LOCK_ON_GAME_LAUNCH, value).apply()
            saveBackupToFile()
        }

    var selectedPackages: Set<String>
        get() {
            val saved = prefs.getStringSet(KEY_SELECTED_APPS, null)
            return if (saved.isNullOrEmpty()) {
                DEFAULT_TARGET_PACKAGES
            } else {
                HashSet(saved)
            }
        }
        set(value) {
            prefs.edit().putStringSet(KEY_SELECTED_APPS, HashSet(value)).apply()
            saveBackupToFile()
        }

    var currentMode: Int
        get() = prefs.getInt(KEY_CURRENT_MODE, MODE_LOCK)
        set(value) {
            prefs.edit().putInt(KEY_CURRENT_MODE, value).apply()
            saveBackupToFile()
        }

    var editOpacityPercent: Int
        get() = prefs.getInt(KEY_EDIT_OPACITY, 50).coerceIn(15, 95)
        set(value) {
            prefs.edit().putInt(KEY_EDIT_OPACITY, value.coerceIn(15, 95)).apply()
            saveBackupToFile()
        }

    var showLockBorder: Boolean
        get() = prefs.getBoolean(KEY_LOCK_BORDER, false)
        set(value) {
            prefs.edit().putBoolean(KEY_LOCK_BORDER, value).apply()
            saveBackupToFile()
        }

    var hidePillInLockMode: Boolean
        get() = prefs.getBoolean(KEY_HIDE_PILL_IN_LOCK, false)
        set(value) {
            prefs.edit().putBoolean(KEY_HIDE_PILL_IN_LOCK, value).apply()
            saveBackupToFile()
        }

    var hideFloatingToggleAlways: Boolean
        get() = prefs.getBoolean(KEY_HIDE_TOGGLE_ALWAYS, false)
        set(value) {
            prefs.edit().putBoolean(KEY_HIDE_TOGGLE_ALWAYS, value).apply()
            saveBackupToFile()
        }

    var showOverlayInApp: Boolean
        get() = prefs.getBoolean(KEY_SHOW_OVERLAY_IN_APP, true)
        set(value) {
            prefs.edit().putBoolean(KEY_SHOW_OVERLAY_IN_APP, value).apply()
            saveBackupToFile()
        }

    var isPillHiddenManually: Boolean
        get() = prefs.getBoolean(KEY_PILL_HIDDEN_MANUALLY, false)
        set(value) {
            prefs.edit().putBoolean(KEY_PILL_HIDDEN_MANUALLY, value).apply()
            saveBackupToFile()
        }

    var pillX: Int
        get() = prefs.getInt(KEY_PILL_X, 950)
        set(value) {
            prefs.edit().putInt(KEY_PILL_X, value).apply()
            saveBackupToFile()
        }

    var pillY: Int
        get() = prefs.getInt(KEY_PILL_Y, 24)
        set(value) {
            prefs.edit().putInt(KEY_PILL_Y, value).apply()
            saveBackupToFile()
        }

    var isPillCollapsed: Boolean
        get() = prefs.getBoolean(KEY_PILL_COLLAPSED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_PILL_COLLAPSED, value).apply()
            saveBackupToFile()
        }

    fun isZoneEnabled(zoneId: Int): Boolean {
        val defaultOn = zoneId <= 3
        return prefs.getBoolean("zone_${zoneId}_enabled", defaultOn)
    }

    fun setZoneEnabled(zoneId: Int, enabled: Boolean) {
        prefs.edit().putBoolean("zone_${zoneId}_enabled", enabled).apply()
        saveBackupToFile()
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
                title = "Z2: Top Status & Call Bar"
                defaultX = 220
                defaultY = 0
                defaultW = 1650
                defaultH = 85
            }
            3 -> {
                title = "Z3: Below 3rd-Finger Claw"
                defaultX = 220
                defaultY = 270
                defaultW = 420
                defaultH = 330
            }
            4 -> {
                title = "Z4: Right of 3rd-Finger"
                defaultX = 640
                defaultY = 85
                defaultW = 380
                defaultH = 420
            }
            5 -> {
                title = "Z5: Above Joystick Guard"
                defaultX = 120
                defaultY = 600
                defaultW = 350
                defaultH = 180
            }
            else -> {
                title = "Z6: Right Edge / Fire Guard"
                defaultX = 2200
                defaultY = 0
                defaultW = 200
                defaultH = 600
            }
        }

        return ZoneConfig(
            id = zoneId,
            title = title,
            enabled = isZoneEnabled(zoneId),
            x = prefs.getInt("zone_${zoneId}_x", defaultX).coerceAtLeast(0),
            y = prefs.getInt("zone_${zoneId}_y", defaultY).coerceAtLeast(0),
            width = prefs.getInt("zone_${zoneId}_w", defaultW).coerceAtLeast(30),
            height = prefs.getInt("zone_${zoneId}_h", defaultH).coerceAtLeast(30)
        )
    }

    fun saveZoneBounds(zoneId: Int, x: Int, y: Int, width: Int, height: Int) {
        prefs.edit()
            .putInt("zone_${zoneId}_x", x.coerceAtLeast(0))
            .putInt("zone_${zoneId}_y", y.coerceAtLeast(0))
            .putInt("zone_${zoneId}_w", width.coerceAtLeast(30))
            .putInt("zone_${zoneId}_h", height.coerceAtLeast(30))
            .apply()
        saveBackupToFile()
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
        editor.putBoolean(KEY_HIDE_TOGGLE_ALWAYS, false)
        editor.putBoolean(KEY_SHOW_OVERLAY_IN_APP, true)
        editor.putInt(KEY_CURRENT_MODE, MODE_LOCK)
        editor.apply()
        saveBackupToFile()
    }

    private fun getBackupFiles(): List<File> {
        val files = mutableListOf<File>()
        try {
            files.add(File(context.filesDir, BACKUP_FILENAME))
            val extDir = context.getExternalFilesDir(null)
            if (extDir != null) {
                files.add(File(extDir, BACKUP_FILENAME))
            }
        } catch (_: Exception) {
        }
        return files
    }

    @Synchronized
    fun saveBackupToFile() {
        Thread {
            try {
                val json = JSONObject()
                json.put("master_enabled", isMasterEnabled)
                json.put("app_filter_enabled", isAppFilterEnabled)
                json.put("auto_landscape_enable", isAutoLandscapeEnabled)
                json.put("auto_lock_on_game_launch", isAutoLockOnGameLaunch)
                json.put("current_mode", currentMode)
                json.put("edit_opacity", editOpacityPercent)
                json.put("lock_border", showLockBorder)
                json.put("hide_toggle_always", hideFloatingToggleAlways)
                json.put("show_overlay_in_app", showOverlayInApp)
                json.put("pill_x", pillX)
                json.put("pill_y", pillY)

                val appsArray = JSONArray()
                selectedPackages.forEach { appsArray.put(it) }
                json.put("selected_apps", appsArray)

                val zonesArray = JSONArray()
                for (id in 1..TOTAL_ZONES) {
                    val cfg = getZoneConfig(id)
                    val zObj = JSONObject().apply {
                        put("id", cfg.id)
                        put("enabled", cfg.enabled)
                        put("x", cfg.x)
                        put("y", cfg.y)
                        put("w", cfg.width)
                        put("h", cfg.height)
                    }
                    zonesArray.put(zObj)
                }
                json.put("zones", zonesArray)

                val content = json.toString(2)
                for (f in getBackupFiles()) {
                    f.writeText(content)
                }
            } catch (_: Exception) {
            }
        }.start()
    }

    @Synchronized
    fun restoreBackupFromFile(): Boolean {
        try {
            for (f in getBackupFiles()) {
                if (f.exists() && f.length() > 10) {
                    val content = f.readText()
                    val json = JSONObject(content)
                    val editor = prefs.edit()

                    if (json.has("master_enabled")) editor.putBoolean(KEY_MASTER_ENABLED, json.getBoolean("master_enabled"))
                    if (json.has("app_filter_enabled")) editor.putBoolean(KEY_APP_FILTER_ENABLED, json.getBoolean("app_filter_enabled"))
                    if (json.has("auto_landscape_enable")) editor.putBoolean(KEY_AUTO_LANDSCAPE_ENABLE, json.getBoolean("auto_landscape_enable"))
                    if (json.has("auto_lock_on_game_launch")) editor.putBoolean(KEY_AUTO_LOCK_ON_GAME_LAUNCH, json.getBoolean("auto_lock_on_game_launch"))
                    if (json.has("current_mode")) editor.putInt(KEY_CURRENT_MODE, json.getInt("current_mode"))
                    if (json.has("edit_opacity")) editor.putInt(KEY_EDIT_OPACITY, json.getInt("edit_opacity"))
                    if (json.has("hide_toggle_always")) editor.putBoolean(KEY_HIDE_TOGGLE_ALWAYS, json.getBoolean("hide_toggle_always"))
                    if (json.has("pill_x")) editor.putInt(KEY_PILL_X, json.getInt("pill_x"))
                    if (json.has("pill_y")) editor.putInt(KEY_PILL_Y, json.getInt("pill_y"))

                    if (json.has("zones")) {
                        val zonesArray = json.getJSONArray("zones")
                        for (i in 0 until zonesArray.length()) {
                            val zObj = zonesArray.getJSONObject(i)
                            val id = zObj.getInt("id")
                            editor.putBoolean("zone_${id}_enabled", zObj.getBoolean("enabled"))
                            editor.putInt("zone_${id}_x", zObj.getInt("x"))
                            editor.putInt("zone_${id}_y", zObj.getInt("y"))
                            editor.putInt("zone_${id}_w", zObj.getInt("w"))
                            editor.putInt("zone_${id}_h", zObj.getInt("h"))
                        }
                    }

                    editor.apply()
                    return true
                }
            }
        } catch (_: Exception) {
        }
        return false
    }
}
