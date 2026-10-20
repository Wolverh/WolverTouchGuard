package com.wolver.touchguard

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object UpdateManager {

    private const val VERSION_CHECK_URL =
        "https://raw.githubusercontent.com/Wolverh/WolverTouchGuard/main/version.json"

    data class UpdateInfo(
        val hasUpdate: Boolean,
        val latestVersionCode: Int,
        val latestVersionName: String,
        val downloadUrl: String,
        val changelog: String
    )

    fun check(context: Context, callback: (UpdateInfo?) -> Unit) {
        val mainHandler = Handler(Looper.getMainLooper())

        Thread {
            try {
                val url = URL(VERSION_CHECK_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.requestMethod = "GET"
                conn.useCaches = false

                if (conn.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = reader.readText()
                    reader.close()

                    val json = JSONObject(response)
                    val remoteCode = json.optInt("versionCode", 0)
                    val remoteName = json.optString("versionName", "")
                    val downloadUrl = json.optString("downloadUrl", "")
                    val changelog = json.optString("changelog", "Bug fixes and improvements")

                    val currentCode = BuildConfig.VERSION_CODE
                    val hasUpdate = remoteCode > currentCode

                    val info = UpdateInfo(
                        hasUpdate = hasUpdate,
                        latestVersionCode = remoteCode,
                        latestVersionName = remoteName,
                        downloadUrl = downloadUrl,
                        changelog = changelog
                    )

                    mainHandler.post { callback(info) }
                } else {
                    mainHandler.post { callback(null) }
                }
            } catch (_: Exception) {
                mainHandler.post { callback(null) }
            }
        }.start()
    }

    fun showUpdateDialog(activity: Activity, info: UpdateInfo) {
        AlertDialog.Builder(activity)
            .setTitle("🚀 New Update Available (v${info.latestVersionName})")
            .setMessage("${info.changelog}\n\nClick 'Update Now' to download and install with 1-click.")
            .setPositiveButton("Update Now") { _, _ ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl))
                    activity.startActivity(intent)
                } catch (_: Exception) {
                }
            }
            .setNegativeButton("Later", null)
            .show()
    }
}
