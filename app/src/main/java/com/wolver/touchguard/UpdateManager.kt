package com.wolver.touchguard

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
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
                // Append timestamp query parameter to bypass GitHub Raw CDN cache
                val cacheBustedUrl = "$VERSION_CHECK_URL?t=${System.currentTimeMillis()}"
                val url = URL(cacheBustedUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.requestMethod = "GET"
                conn.useCaches = false
                conn.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                conn.setRequestProperty("Pragma", "no-cache")

                if (conn.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = reader.readText()
                    reader.close()

                    val json = JSONObject(response)
                    val remoteCode = json.optInt("versionCode", 0)
                    val remoteName = json.optString("versionName", "")
                    val downloadUrl = json.optString("downloadUrl", "")
                    val changelog = json.optString("changelog", "Bug fixes and performance improvements.")

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
        if (activity.isFinishing || activity.isDestroyed) return

        AlertDialog.Builder(activity)
            .setTitle("🚀 Update Available: v${info.latestVersionName}")
            .setMessage("${info.changelog}\n\nClick 'Download & Install Now' to update directly inside the app.")
            .setPositiveButton("Download & Install Now") { _, _ ->
                startDirectInAppDownload(activity, info)
            }
            .setNeutralButton("Force Reinstall Current") { _, _ ->
                startDirectInAppDownload(activity, info)
            }
            .setNegativeButton("Later", null)
            .show()
    }

    fun startDirectInAppDownload(activity: Activity, info: UpdateInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!activity.packageManager.canRequestPackageInstalls()) {
                Toast.makeText(
                    activity,
                    "Please allow 'Install unknown apps' for Wolver Touch Guard first, then tap Update again.",
                    Toast.LENGTH_LONG
                ).show()
                try {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${activity.packageName}")
                    )
                    activity.startActivity(intent)
                } catch (_: Exception) {
                }
                return
            }
        }

        val density = activity.resources.displayMetrics.density
        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (20 * density).toInt()
            setPadding(pad, pad, pad, pad)
        }

        val tvStatus = TextView(activity).apply {
            text = "Connecting to GitHub Release server..."
            setTextColor(Color.BLACK)
            textSize = 14f
        }

        val progressBar = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = false
            max = 100
            progress = 0
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = (12 * density).toInt()
            layoutParams = lp
        }

        container.addView(tvStatus)
        container.addView(progressBar)

        val isCancelled = java.util.concurrent.atomic.AtomicBoolean(false)

        val progressDialog = AlertDialog.Builder(activity)
            .setTitle("Downloading v${info.latestVersionName}...")
            .setView(container)
            .setCancelable(false)
            .setNegativeButton("Cancel") { dialog, _ ->
                isCancelled.set(true)
                dialog.dismiss()
            }
            .create()

        progressDialog.show()

        val mainHandler = Handler(Looper.getMainLooper())

        Thread {
            var outputFile: File? = null
            try {
                val updateDir = activity.getExternalFilesDir(null) ?: activity.cacheDir
                if (!updateDir.exists()) updateDir.mkdirs()

                // Delete any old partial or cached APK files first
                updateDir.listFiles()?.forEach { file ->
                    if (file.name.endsWith(".apk")) {
                        try {
                            file.delete()
                        } catch (_: Exception) {
                        }
                    }
                }

                outputFile = File(updateDir, "WolverTouchGuard_v${info.latestVersionCode}.apk")

                val downloadUrlWithBust = if (info.downloadUrl.contains("?")) {
                    "${info.downloadUrl}&t=${System.currentTimeMillis()}"
                } else {
                    "${info.downloadUrl}?t=${System.currentTimeMillis()}"
                }

                var currentUrl = downloadUrlWithBust
                var redirectCount = 0
                var conn: HttpURLConnection? = null

                while (redirectCount < 5) {
                    val url = URL(currentUrl)
                    conn = (url.openConnection() as HttpURLConnection).apply {
                        instanceFollowRedirects = true
                        connectTimeout = 15000
                        readTimeout = 20000
                        requestMethod = "GET"
                        useCaches = false
                        setRequestProperty("Accept", "application/octet-stream")
                        setRequestProperty("Cache-Control", "no-cache")
                    }
                    conn.connect()

                    val code = conn.responseCode
                    if (code in 301..308) {
                        val location = conn.getHeaderField("Location")
                        conn.disconnect()
                        if (location.isNullOrEmpty()) break
                        currentUrl = location
                        redirectCount++
                    } else {
                        break
                    }
                }

                if (conn == null || conn.responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("Server returned HTTP ${conn?.responseCode}")
                }

                val fileLength = conn.contentLength
                val input = BufferedInputStream(conn.inputStream)
                val output = FileOutputStream(outputFile)

                val data = ByteArray(8192)
                var total: Long = 0
                var count: Int

                while (input.read(data).also { count = it } != -1) {
                    if (isCancelled.get()) {
                        input.close()
                        output.close()
                        outputFile.delete()
                        return@Thread
                    }
                    total += count
                    output.write(data, 0, count)

                    if (fileLength > 0) {
                        val percent = ((total * 100) / fileLength).toInt()
                        val mbDone = String.format("%.2f", total / (1024f * 1024f))
                        val mbTotal = String.format("%.2f", fileLength / (1024f * 1024f))
                        mainHandler.post {
                            progressBar.isIndeterminate = false
                            progressBar.progress = percent
                            tvStatus.text = "Downloaded: $mbDone MB / $mbTotal MB ($percent%)"
                        }
                    } else {
                        val mbDone = String.format("%.2f", total / (1024f * 1024f))
                        mainHandler.post {
                            progressBar.isIndeterminate = true
                            tvStatus.text = "Downloaded: $mbDone MB..."
                        }
                    }
                }

                output.flush()
                output.close()
                input.close()

                // Verify APK is valid and non-empty (> 100 KB)
                if (!outputFile.exists() || outputFile.length() < 100_000) {
                    throw Exception("Downloaded APK is incomplete or corrupted.")
                }

                mainHandler.post {
                    if (!activity.isFinishing && !activity.isDestroyed && progressDialog.isShowing) {
                        progressDialog.dismiss()
                    }
                    installApkDirect(activity, outputFile)
                }

            } catch (e: Exception) {
                outputFile?.let {
                    try {
                        if (it.exists()) it.delete()
                    } catch (_: Exception) {
                    }
                }
                mainHandler.post {
                    if (!activity.isFinishing && !activity.isDestroyed && progressDialog.isShowing) {
                        progressDialog.dismiss()
                    }
                    Toast.makeText(
                        activity,
                        "Download failed: ${e.message ?: "Check connection"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun installApkDirect(context: Context, apkFile: File) {
        try {
            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Installer error: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
