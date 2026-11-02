package com.wolver.touchguard

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.Toast

class TouchGuardAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: TouchGuardAccessibilityService? = null
            private set

        private val IGNORED_TRANSIENT_PACKAGES = setOf(
            "com.android.systemui",
            "android",
            "com.transsion.gamespace",
            "com.transsion.xarena",
            "com.transsion.smartpanel",
            "com.transsion.kolun.assistant",
            "com.google.android.inputmethod.latin",
            "com.android.permissioncontroller"
        )
    }

    private lateinit var prefManager: PrefManager
    private lateinit var windowManager: WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private val zoneViews = mutableMapOf<Int, ZoneOverlayView>()
    private val zoneParams = mutableMapOf<Int, WindowManager.LayoutParams>()

    private var pillView: FloatingPillView? = null
    private var pillParams: WindowManager.LayoutParams? = null

    private var menuView: FloatingMenuView? = null

    private var currentForegroundPackage: String = ""
    private var lastDetectedGamePackage: String = ""
    private var overlaysAttached = false

    private var volUpPressed = false
    private var volDownPressed = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        prefManager = PrefManager(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createOverlayViews()
        refreshOverlaysVisibilityAndState()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val detectedPkg = resolveActiveApplicationPackage(event)
        if (detectedPkg.isNotEmpty() && detectedPkg != currentForegroundPackage) {
            val previousPkg = currentForegroundPackage
            currentForegroundPackage = detectedPkg

            val targetGames = prefManager.selectedPackages
            val isTargetGameNow = targetGames.contains(detectedPkg)
            val wasTargetGameBefore = targetGames.contains(previousPkg)

            // Instant Free Fire / Game Launch Auto-Activation
            if (isTargetGameNow && !wasTargetGameBefore) {
                lastDetectedGamePackage = detectedPkg
                if (prefManager.isAutoLockOnGameLaunch) {
                    prefManager.isMasterEnabled = true
                    if (prefManager.currentMode == PrefManager.MODE_OFF) {
                        prefManager.currentMode = PrefManager.MODE_LOCK
                    }
                    Toast.makeText(
                        this,
                        "⚡ Wolver Touch Guard Auto-Activated for Game!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            refreshOverlaysVisibilityAndState()
        }
    }

    private fun resolveActiveApplicationPackage(event: AccessibilityEvent): String {
        val targetPackages = prefManager.selectedPackages

        // 1. Check event package directly first for instant Free Fire detection
        val directEventPkg = event.packageName?.toString().orEmpty()
        if (directEventPkg.isNotEmpty() && targetPackages.contains(directEventPkg)) {
            return directEventPkg
        }

        // 2. Inspect interactive windows on screen
        try {
            val interactiveWindows = windows
            if (!interactiveWindows.isNullOrEmpty()) {
                for (win in interactiveWindows) {
                    if (win.type == AccessibilityWindowInfo.TYPE_APPLICATION) {
                        val pkg = win.root?.packageName?.toString().orEmpty()
                        if (pkg.isNotEmpty() && targetPackages.contains(pkg)) {
                            return pkg
                        }
                    }
                }
                for (win in interactiveWindows) {
                    if (win.type == AccessibilityWindowInfo.TYPE_APPLICATION) {
                        val pkg = win.root?.packageName?.toString().orEmpty()
                        if (pkg == packageName) {
                            return pkg
                        }
                    }
                }
                for (win in interactiveWindows) {
                    if (win.type == AccessibilityWindowInfo.TYPE_APPLICATION) {
                        val pkg = win.root?.packageName?.toString().orEmpty()
                        if (pkg.isNotEmpty() && !IGNORED_TRANSIENT_PACKAGES.contains(pkg)) {
                            return pkg
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }

        if (directEventPkg.isNotEmpty() && !IGNORED_TRANSIENT_PACKAGES.contains(directEventPkg)) {
            return directEventPkg
        }

        return currentForegroundPackage
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                volUpPressed = (event.action == KeyEvent.ACTION_DOWN)
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                volDownPressed = (event.action == KeyEvent.ACTION_DOWN)
            }
        }
        if (volUpPressed && volDownPressed && overlaysAttached) {
            volUpPressed = false
            volDownPressed = false

            // Vol Up + Vol Down restores floating toggle if hidden
            if (prefManager.hideFloatingToggleAlways || prefManager.isPillHiddenManually) {
                prefManager.hideFloatingToggleAlways = false
                prefManager.isPillHiddenManually = false
                Toast.makeText(this, "🔘 Floating Toggle Restored!", Toast.LENGTH_SHORT).show()
            } else {
                val nextMode = if (prefManager.currentMode == PrefManager.MODE_LOCK) {
                    PrefManager.MODE_EDIT
                } else {
                    PrefManager.MODE_LOCK
                }
                prefManager.currentMode = nextMode
                val modeName = if (nextMode == PrefManager.MODE_LOCK) "LOCK MODE (Blocking ON)" else "EDIT MODE"
                Toast.makeText(this, modeName, Toast.LENGTH_SHORT).show()
            }
            refreshOverlaysVisibilityAndState()
            return true
        }
        return super.onKeyEvent(event)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE && prefManager.isAutoLandscapeEnabled) {
            if (prefManager.currentMode == PrefManager.MODE_OFF) {
                prefManager.currentMode = PrefManager.MODE_LOCK
            }
        }
        refreshOverlaysVisibilityAndState()
    }

    fun showFloatingMenuFromApp() {
        mainHandler.post {
            showFloatingMenu()
        }
    }

    private fun showFloatingMenu() {
        if (menuView != null) return

        val menu = FloatingMenuView(
            context = this,
            prefManager = prefManager,
            onStateChanged = {
                refreshOverlaysVisibilityAndState()
            },
            onHideToggleOnlyClicked = {
                prefManager.isPillHiddenManually = true
                prefManager.hideFloatingToggleAlways = true
                prefManager.currentMode = PrefManager.MODE_LOCK
                refreshOverlaysVisibilityAndState()
                Toast.makeText(
                    this,
                    "🚫 Toggle Hidden! Touch Blocking is 100% ACTIVE. (Vol Up + Vol Down to unhide)",
                    Toast.LENGTH_LONG
                ).show()
            },
            onOpenAppClicked = {
                try {
                    val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    if (intent != null) startActivity(intent)
                } catch (_: Exception) {
                }
            },
            onDismissClicked = {
                dismissFloatingMenu()
            }
        )

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        )

        applyDisplayRefreshRate(params)

        menuView = menu
        try {
            windowManager.addView(menu, params)
        } catch (_: Exception) {
            menuView = null
        }
    }

    private fun dismissFloatingMenu() {
        menuView?.let {
            try {
                if (it.parent != null) windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }
        menuView = null
    }

    private fun applyDisplayRefreshRate(params: WindowManager.LayoutParams) {
        try {
            val disp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            }
            val maxRate = disp?.supportedModes?.maxOfOrNull { it.refreshRate } ?: 144f
            if (maxRate > 60f) {
                params.preferredRefreshRate = maxRate
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val bestMode = disp?.supportedModes?.maxByOrNull { it.refreshRate }
                    if (bestMode != null) {
                        params.preferredDisplayModeId = bestMode.modeId
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun createOverlayViews() {
        if (overlaysAttached) return

        for (zoneId in 1..PrefManager.TOTAL_ZONES) {
            val config = prefManager.getZoneConfig(zoneId)
            val view = ZoneOverlayView(
                context = this,
                zoneId = zoneId,
                title = config.title,
                onBoundsUpdated = { id, newX, newY, newW, newH ->
                    prefManager.saveZoneBounds(id, newX, newY, newW, newH)
                    val lp = zoneParams[id]
                    val v = zoneViews[id]
                    if (lp != null && v != null && v.parent != null) {
                        lp.x = newX
                        lp.y = newY
                        lp.width = newW
                        lp.height = newH
                        try {
                            windowManager.updateViewLayout(v, lp)
                        } catch (_: Exception) {
                        }
                    }
                }
            )

            view.isPillAt = { rawX, rawY ->
                val pill = pillView
                if (pill != null && pill.visibility == View.VISIBLE) {
                    val loc = IntArray(2)
                    pill.getLocationOnScreen(loc)
                    val pLeft = loc[0].toFloat() - 8f
                    val pTop = loc[1].toFloat() - 8f
                    val pRight = pLeft + pill.width.toFloat() + 16f
                    val pBottom = pTop + pill.height.toFloat() + 16f
                    rawX in pLeft..pRight && rawY in pTop..pBottom
                } else {
                    false
                }
            }

            view.onPillTouchForward = { ev ->
                val pill = pillView
                if (pill != null && pill.visibility == View.VISIBLE) {
                    val loc = IntArray(2)
                    pill.getLocationOnScreen(loc)
                    val copy = MotionEvent.obtain(ev)
                    copy.setLocation(ev.rawX - loc[0].toFloat(), ev.rawY - loc[1].toFloat())
                    pill.dispatchTouchEvent(copy)
                    copy.recycle()
                }
            }

            val params = createBaseLayoutParams(
                x = config.x,
                y = config.y,
                width = config.width,
                height = config.height,
                touchable = true
            )

            zoneViews[zoneId] = view
            zoneParams[zoneId] = params

            try {
                windowManager.addView(view, params)
            } catch (_: Exception) {
            }
        }

        val pill = FloatingPillView(
            context = this,
            onPillTapped = {
                showFloatingMenu()
            },
            onCollapseToggled = { collapsed ->
                prefManager.isPillCollapsed = collapsed
                refreshOverlaysVisibilityAndState()
            },
            onHideCompletelyClicked = {
                prefManager.hideFloatingToggleAlways = true
                prefManager.isPillHiddenManually = true
                prefManager.currentMode = PrefManager.MODE_LOCK
                refreshOverlaysVisibilityAndState()
                Toast.makeText(
                    this,
                    "🚫 Toggle Hidden! Touch Blocking remains ACTIVE. (Vol Up + Vol Down to unhide)",
                    Toast.LENGTH_LONG
                ).show()
            },
            onPositionMoved = { newX, newY ->
                prefManager.pillX = newX
                prefManager.pillY = newY
                val lp = pillParams
                val v = pillView
                if (lp != null && v != null && v.parent != null) {
                    lp.x = newX
                    lp.y = newY
                    try {
                        windowManager.updateViewLayout(v, lp)
                    } catch (_: Exception) {
                    }
                }
            }
        )

        val pParams = createBaseLayoutParams(
            x = prefManager.pillX,
            y = prefManager.pillY,
            width = WindowManager.LayoutParams.WRAP_CONTENT,
            height = WindowManager.LayoutParams.WRAP_CONTENT,
            touchable = true
        )

        pillView = pill
        pillParams = pParams

        try {
            windowManager.addView(pill, pParams)
            overlaysAttached = true
        } catch (_: Exception) {
        }
    }

    private fun createBaseLayoutParams(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        touchable: Boolean
    ): WindowManager.LayoutParams {
        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_SPLIT_TOUCH or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED

        if (!touchable) {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }

        val params = WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = x
        params.y = y

        applyDisplayRefreshRate(params)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        return params
    }

    fun refreshOverlaysVisibilityAndState() {
        mainHandler.post {
            refreshInternal()
        }
    }

    private fun refreshInternal() {
        if (!overlaysAttached) {
            createOverlayViews()
        }

        val masterOn = prefManager.isMasterEnabled
        val filterOn = prefManager.isAppFilterEnabled
        val allowedPackages = prefManager.selectedPackages
        val mode = prefManager.currentMode

        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        // Use reliable static activity state instead of fragile accessibility package events
        val isSelfApp = MainActivity.isActivityInForeground

        // Touch blocking should be active when:
        // 1. Inside MainActivity: only if user specifically chose "View Blockage Bars" or is in Edit mode
        // 2. Outside MainActivity:
        //    - If Filter is OFF: Always active
        //    - If Filter is ON: Active if package matches, or device is Landscape, or foreground package is unknown
        val shouldActivateZones: Boolean = if (!masterOn || mode == PrefManager.MODE_OFF) {
            false
        } else if (isSelfApp) {
            prefManager.showOverlayInApp || (mode == PrefManager.MODE_EDIT)
        } else {
            if (!filterOn) {
                true
            } else {
                val pkgMatch = currentForegroundPackage.isNotEmpty() && allowedPackages.contains(currentForegroundPackage)
                val landscapeGameMatch = isLandscape && prefManager.isAutoLandscapeEnabled
                val unknownForeground = currentForegroundPackage.isEmpty()
                pkgMatch || landscapeGameMatch || unknownForeground
            }
        }

        // Apply or restore Exact Numerical DPI Sensitivity
        if (shouldActivateZones && prefManager.isDpiEngineEnabled) {
            DpiSensitivityEngine.activateDpi(this, prefManager)
        } else if (!masterOn || mode == PrefManager.MODE_OFF || !prefManager.isDpiEngineEnabled) {
            DpiSensitivityEngine.deactivateDpi(this, prefManager)
        }

        val opacity = prefManager.editOpacityPercent
        val lockBorder = prefManager.showLockBorder

        // Clean base flags with FLAG_SPLIT_TOUCH and FLAG_HARDWARE_ACCELERATED for 144Hz multi-touch
        val baseFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_SPLIT_TOUCH or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED

        for (zoneId in 1..PrefManager.TOTAL_ZONES) {
            val view = zoneViews[zoneId] ?: continue
            val params = zoneParams[zoneId] ?: continue
            val cfg = prefManager.getZoneConfig(zoneId)

            if (!shouldActivateZones || !cfg.enabled) {
                view.visibility = View.GONE
            } else {
                view.visibility = View.VISIBLE
                params.x = cfg.x
                params.y = cfg.y
                params.width = cfg.width
                params.height = cfg.height

                // Keep high refresh rate for 144Hz
                applyDisplayRefreshRate(params)

                // Inside MainActivity in LOCK mode: allow clicks to pass to MainActivity UI
                // Outside MainActivity (games, Free Fire): SWALLOW 100% of touches
                params.flags = if (isSelfApp && mode == PrefManager.MODE_LOCK) {
                    baseFlags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                } else {
                    baseFlags
                }

                view.updateState(
                    x = cfg.x,
                    y = cfg.y,
                    w = cfg.width,
                    h = cfg.height,
                    mode = mode,
                    opacityPercent = opacity,
                    lockBorder = lockBorder
                )

                try {
                    windowManager.updateViewLayout(view, params)
                } catch (_: Exception) {
                }
            }
        }

        // Floating pill toggle is 100% independent from zone blocking
        pillView?.let { pill ->
            val pParams = pillParams ?: return@let
            val shouldHidePill =
                prefManager.hideFloatingToggleAlways ||
                prefManager.isPillHiddenManually ||
                (mode == PrefManager.MODE_LOCK && prefManager.hidePillInLockMode)

            val canShowPillInContext = masterOn && (shouldActivateZones || isSelfApp || currentForegroundPackage.isEmpty())

            if (!canShowPillInContext || shouldHidePill) {
                pill.visibility = View.GONE
                dismissFloatingMenu()
            } else {
                pill.visibility = View.VISIBLE
                pParams.x = prefManager.pillX
                pParams.y = prefManager.pillY
                pParams.flags = baseFlags
                applyDisplayRefreshRate(pParams)

                pill.updatePillState(
                    mode = mode,
                    collapsed = prefManager.isPillCollapsed,
                    x = prefManager.pillX,
                    y = prefManager.pillY
                )
                try {
                    windowManager.updateViewLayout(pill, pParams)
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun removeAllOverlays() {
        dismissFloatingMenu()
        for ((_, view) in zoneViews) {
            try {
                if (view.parent != null) windowManager.removeView(view)
            } catch (_: Exception) {
            }
        }
        zoneViews.clear()
        zoneParams.clear()

        pillView?.let {
            try {
                if (it.parent != null) windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }
        pillView = null
        pillParams = null
        overlaysAttached = false
    }

    override fun onInterrupt() {
    }

    override fun onDestroy() {
        DpiSensitivityEngine.deactivateDpi(this, prefManager)
        removeAllOverlays()
        instance = null
        super.onDestroy()
    }
}
