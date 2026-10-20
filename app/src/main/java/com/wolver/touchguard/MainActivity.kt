package com.wolver.touchguard

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class MainActivity : AppCompatActivity() {

    private lateinit var prefManager: PrefManager

    private lateinit var tvServiceStatus: TextView
    private lateinit var btnOpenAppInfo: Button
    private lateinit var btnOpenAccessibility: Button
    private lateinit var switchMasterEnable: SwitchCompat
    private lateinit var switchAppFilter: SwitchCompat
    private lateinit var tvSelectedAppsSummary: TextView
    private lateinit var btnSelectApps: Button

    private lateinit var switchViewBarsOnScreen: SwitchCompat
    private lateinit var switchShowFloatingToggle: SwitchCompat
    private lateinit var radarScreenPreview: ScreenRadarView

    private lateinit var switchZone1: SwitchCompat
    private lateinit var switchZone2: SwitchCompat
    private lateinit var switchZone3: SwitchCompat
    private lateinit var switchZone4: SwitchCompat

    private lateinit var switchHidePillInLock: SwitchCompat
    private lateinit var switchLockBorder: SwitchCompat
    private lateinit var tvOpacityValue: TextView
    private lateinit var seekBarEditOpacity: SeekBar

    private lateinit var btnSelectZ1: Button
    private lateinit var btnSelectZ2: Button
    private lateinit var btnSelectZ3: Button
    private lateinit var btnSelectZ4: Button
    private lateinit var tvEditingZoneTitle: TextView
    private lateinit var tvZoneX: TextView
    private lateinit var seekZoneX: SeekBar
    private lateinit var tvZoneY: TextView
    private lateinit var seekZoneY: SeekBar
    private lateinit var tvZoneW: TextView
    private lateinit var seekZoneW: SeekBar
    private lateinit var tvZoneH: TextView
    private lateinit var seekZoneH: SeekBar

    private lateinit var btnForceEditMode: Button
    private lateinit var btnForceLockMode: Button
    private lateinit var btnResetZones: Button

    private var currentlySelectedZoneForSliders: Int = 1
    private var isUpdatingSlidersProgrammatically: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefManager = PrefManager(this)
        bindViews()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        syncUiWithPrefs()
        updateServiceStatusBanner()
    }

    private fun bindViews() {
        tvServiceStatus = findViewById(R.id.tvServiceStatus)
        btnOpenAppInfo = findViewById(R.id.btnOpenAppInfo)
        btnOpenAccessibility = findViewById(R.id.btnOpenAccessibility)
        switchMasterEnable = findViewById(R.id.switchMasterEnable)
        switchAppFilter = findViewById(R.id.switchAppFilter)
        tvSelectedAppsSummary = findViewById(R.id.tvSelectedAppsSummary)
        btnSelectApps = findViewById(R.id.btnSelectApps)

        switchViewBarsOnScreen = findViewById(R.id.switchViewBarsOnScreen)
        switchShowFloatingToggle = findViewById(R.id.switchShowFloatingToggle)
        radarScreenPreview = findViewById(R.id.radarScreenPreview)
        radarScreenPreview.prefManager = prefManager

        switchZone1 = findViewById(R.id.switchZone1)
        switchZone2 = findViewById(R.id.switchZone2)
        switchZone3 = findViewById(R.id.switchZone3)
        switchZone4 = findViewById(R.id.switchZone4)

        switchHidePillInLock = findViewById(R.id.switchHidePillInLock)
        switchLockBorder = findViewById(R.id.switchLockBorder)
        tvOpacityValue = findViewById(R.id.tvOpacityValue)
        seekBarEditOpacity = findViewById(R.id.seekBarEditOpacity)

        btnSelectZ1 = findViewById(R.id.btnSelectZ1)
        btnSelectZ2 = findViewById(R.id.btnSelectZ2)
        btnSelectZ3 = findViewById(R.id.btnSelectZ3)
        btnSelectZ4 = findViewById(R.id.btnSelectZ4)
        tvEditingZoneTitle = findViewById(R.id.tvEditingZoneTitle)
        tvZoneX = findViewById(R.id.tvZoneX)
        seekZoneX = findViewById(R.id.seekZoneX)
        tvZoneY = findViewById(R.id.tvZoneY)
        seekZoneY = findViewById(R.id.seekZoneY)
        tvZoneW = findViewById(R.id.tvZoneW)
        seekZoneW = findViewById(R.id.seekZoneW)
        tvZoneH = findViewById(R.id.tvZoneH)
        seekZoneH = findViewById(R.id.seekZoneH)

        btnForceEditMode = findViewById(R.id.btnForceEditMode)
        btnForceLockMode = findViewById(R.id.btnForceLockMode)
        btnResetZones = findViewById(R.id.btnResetZones)
    }

    private fun syncUiWithPrefs() {
        switchMasterEnable.isChecked = prefManager.isMasterEnabled
        switchAppFilter.isChecked = prefManager.isAppFilterEnabled
        updateSelectedAppsSummaryText()

        switchViewBarsOnScreen.isChecked = prefManager.showOverlayInApp
        switchShowFloatingToggle.isChecked = !prefManager.hideFloatingToggleAlways

        switchZone1.isChecked = prefManager.isZoneEnabled(1)
        switchZone2.isChecked = prefManager.isZoneEnabled(2)
        switchZone3.isChecked = prefManager.isZoneEnabled(3)
        switchZone4.isChecked = prefManager.isZoneEnabled(4)

        switchHidePillInLock.isChecked = prefManager.hidePillInLockMode
        switchLockBorder.isChecked = prefManager.showLockBorder

        val opacity = prefManager.editOpacityPercent
        seekBarEditOpacity.progress = opacity
        tvOpacityValue.text = "Bar Red Opacity: $opacity%"

        loadZoneIntoSliders(currentlySelectedZoneForSliders)
        radarScreenPreview.invalidate()
    }

    private fun loadZoneIntoSliders(zoneId: Int) {
        currentlySelectedZoneForSliders = zoneId
        radarScreenPreview.selectedZoneId = zoneId
        isUpdatingSlidersProgrammatically = true

        val buttons = listOf(btnSelectZ1, btnSelectZ2, btnSelectZ3, btnSelectZ4)
        buttons.forEachIndexed { index, button ->
            val color = if (index + 1 == zoneId) "#FF1744" else "#1E1E1E"
            button.backgroundTintList = ColorStateList.valueOf(Color.parseColor(color))
        }

        val cfg = prefManager.getZoneConfig(zoneId)
        tvEditingZoneTitle.text = "Adjusting: ${cfg.title}"

        seekZoneX.progress = cfg.x.coerceIn(0, seekZoneX.max)
        tvZoneX.text = "Horizontal Position (X): ${cfg.x} px"

        seekZoneY.progress = cfg.y.coerceIn(0, seekZoneY.max)
        tvZoneY.text = "Vertical Position (Y): ${cfg.y} px"

        seekZoneW.progress = cfg.width.coerceIn(40, seekZoneW.max)
        tvZoneW.text = "Zone Width: ${cfg.width} px"

        seekZoneH.progress = cfg.height.coerceIn(35, seekZoneH.max)
        tvZoneH.text = "Zone Height: ${cfg.height} px"

        isUpdatingSlidersProgrammatically = false
        radarScreenPreview.invalidate()
    }

    private fun saveCurrentSliderValuesToZone() {
        if (isUpdatingSlidersProgrammatically) return
        val id = currentlySelectedZoneForSliders
        val newX = seekZoneX.progress
        val newY = seekZoneY.progress
        val newW = seekZoneW.progress.coerceAtLeast(40)
        val newH = seekZoneH.progress.coerceAtLeast(35)

        tvZoneX.text = "Horizontal Position (X): $newX px"
        tvZoneY.text = "Vertical Position (Y): $newY px"
        tvZoneW.text = "Zone Width: $newW px"
        tvZoneH.text = "Zone Height: $newH px"

        prefManager.saveZoneBounds(id, newX, newY, newW, newH)
        radarScreenPreview.invalidate()
        notifyServiceToRefresh()
    }

    private fun setupListeners() {
        btnOpenAppInfo.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(this, "Could not open App Info", Toast.LENGTH_SHORT).show()
            }
        }

        btnOpenAccessibility.setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            } catch (_: Exception) {
                Toast.makeText(this, "Could not open Accessibility Settings", Toast.LENGTH_SHORT).show()
            }
        }

        switchMasterEnable.setOnCheckedChangeListener { _, isChecked ->
            prefManager.isMasterEnabled = isChecked
            notifyServiceToRefresh()
            radarScreenPreview.invalidate()
        }

        switchAppFilter.setOnCheckedChangeListener { _, isChecked ->
            prefManager.isAppFilterEnabled = isChecked
            notifyServiceToRefresh()
        }

        btnSelectApps.setOnClickListener {
            showAppSelectionDialog()
        }

        switchViewBarsOnScreen.setOnCheckedChangeListener { _, isChecked ->
            prefManager.showOverlayInApp = isChecked
            if (isChecked) {
                prefManager.currentMode = PrefManager.MODE_EDIT
            }
            notifyServiceToRefresh()
            val msg = if (isChecked) "Blockage bars VISIBLE on screen" else "Blockage bars HIDDEN from screen"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        switchShowFloatingToggle.setOnCheckedChangeListener { _, isChecked ->
            prefManager.hideFloatingToggleAlways = !isChecked
            notifyServiceToRefresh()
            val msg = if (isChecked) "Floating toggle visible on screen" else "Floating toggle completely hidden"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        radarScreenPreview.onZoneSelected = { zoneId ->
            loadZoneIntoSliders(zoneId)
        }

        switchZone1.setOnCheckedChangeListener { _, isChecked ->
            prefManager.setZoneEnabled(1, isChecked)
            radarScreenPreview.invalidate()
            notifyServiceToRefresh()
        }

        switchZone2.setOnCheckedChangeListener { _, isChecked ->
            prefManager.setZoneEnabled(2, isChecked)
            radarScreenPreview.invalidate()
            notifyServiceToRefresh()
        }

        switchZone3.setOnCheckedChangeListener { _, isChecked ->
            prefManager.setZoneEnabled(3, isChecked)
            radarScreenPreview.invalidate()
            notifyServiceToRefresh()
        }

        switchZone4.setOnCheckedChangeListener { _, isChecked ->
            prefManager.setZoneEnabled(4, isChecked)
            radarScreenPreview.invalidate()
            notifyServiceToRefresh()
        }

        switchHidePillInLock.setOnCheckedChangeListener { _, isChecked ->
            prefManager.hidePillInLockMode = isChecked
            notifyServiceToRefresh()
        }

        switchLockBorder.setOnCheckedChangeListener { _, isChecked ->
            prefManager.showLockBorder = isChecked
            notifyServiceToRefresh()
        }

        seekBarEditOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val clamped = progress.coerceIn(15, 95)
                tvOpacityValue.text = "Bar Red Opacity: $clamped%"
                if (fromUser) {
                    prefManager.editOpacityPercent = clamped
                    notifyServiceToRefresh()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnSelectZ1.setOnClickListener { loadZoneIntoSliders(1) }
        btnSelectZ2.setOnClickListener { loadZoneIntoSliders(2) }
        btnSelectZ3.setOnClickListener { loadZoneIntoSliders(3) }
        btnSelectZ4.setOnClickListener { loadZoneIntoSliders(4) }

        val sliderListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    saveCurrentSliderValuesToZone()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

        seekZoneX.setOnSeekBarChangeListener(sliderListener)
        seekZoneY.setOnSeekBarChangeListener(sliderListener)
        seekZoneW.setOnSeekBarChangeListener(sliderListener)
        seekZoneH.setOnSeekBarChangeListener(sliderListener)

        btnForceEditMode.setOnClickListener {
            prefManager.isPillHiddenManually = false
            prefManager.showOverlayInApp = true
            prefManager.currentMode = PrefManager.MODE_EDIT
            switchViewBarsOnScreen.isChecked = true
            notifyServiceToRefresh()
            Toast.makeText(this, "Bars are now VISIBLE on screen for adjustment", Toast.LENGTH_SHORT).show()
        }

        btnForceLockMode.setOnClickListener {
            prefManager.currentMode = PrefManager.MODE_LOCK
            prefManager.showOverlayInApp = false
            switchViewBarsOnScreen.isChecked = false
            notifyServiceToRefresh()
            Toast.makeText(this, "Bars HIDDEN. Active touch blocking enabled.", Toast.LENGTH_SHORT).show()
        }

        btnResetZones.setOnClickListener {
            prefManager.resetZonesToDefaults()
            syncUiWithPrefs()
            notifyServiceToRefresh()
            Toast.makeText(this, "All zones restored to defaults", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateSelectedAppsSummaryText() {
        val selected = prefManager.selectedPackages
        if (selected.isEmpty()) {
            tvSelectedAppsSummary.text = "No apps selected (Select at least Free Fire)"
            tvSelectedAppsSummary.setTextColor(Color.parseColor("#FF1744"))
        } else {
            tvSelectedAppsSummary.text = "Active for (${selected.size}): ${selected.joinToString(", ")}"
            tvSelectedAppsSummary.setTextColor(Color.parseColor("#00E676"))
        }
    }

    private fun showAppSelectionDialog() {
        val pm = packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        data class AppEntry(val label: String, val pkg: String)

        val entriesMap = linkedMapOf<String, String>()
        entriesMap["com.dts.freefireth"] = "Free Fire (com.dts.freefireth)"
        entriesMap["com.dts.freefiremax"] = "Free Fire MAX (com.dts.freefiremax)"

        val userApps = installedApps
            .filter { app ->
                val isUserApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0 ||
                        (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                isUserApp && app.packageName != packageName
            }
            .map { app ->
                val name = pm.getApplicationLabel(app).toString()
                AppEntry("$name (${app.packageName})", app.packageName)
            }
            .sortedBy { it.label.lowercase() }

        for (item in userApps) {
            entriesMap[item.pkg] = item.label
        }

        val pkgList = entriesMap.keys.toList()
        val labelArray = entriesMap.values.toTypedArray()
        val currentSelected = prefManager.selectedPackages.toMutableSet()
        val checkedItems = BooleanArray(pkgList.size) { index ->
            currentSelected.contains(pkgList[index])
        }

        AlertDialog.Builder(this)
            .setTitle("Select Games for Touch Guard")
            .setMultiChoiceItems(labelArray, checkedItems) { _, which, isChecked ->
                val pkg = pkgList[which]
                if (isChecked) {
                    currentSelected.add(pkg)
                } else {
                    currentSelected.remove(pkg)
                }
            }
            .setPositiveButton("Save") { _, _ ->
                prefManager.selectedPackages = currentSelected
                updateSelectedAppsSummaryText()
                notifyServiceToRefresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateServiceStatusBanner() {
        val enabled = isAccessibilityServiceEnabled(this, TouchGuardAccessibilityService::class.java)
        if (enabled) {
            tvServiceStatus.text = "● ACTIVE — Accessibility Overlay Running"
            tvServiceStatus.setTextColor(Color.parseColor("#00E676"))
            notifyServiceToRefresh()
        } else {
            tvServiceStatus.text = "● INACTIVE — Enable in Accessibility Settings"
            tvServiceStatus.setTextColor(Color.parseColor("#FF1744"))
        }
    }

    private fun notifyServiceToRefresh() {
        TouchGuardAccessibilityService.instance?.refreshOverlaysVisibilityAndState()
    }

    private fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<*>): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        for (service in enabledServices) {
            val serviceInfo = service.resolveInfo.serviceInfo
            if (serviceInfo.packageName == context.packageName && serviceInfo.name == serviceClass.name) {
                return true
            }
        }
        return false
    }
}
