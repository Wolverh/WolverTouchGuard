package com.wolver.touchguard

import android.accessibilityservice.AccessibilityService
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.KeyEvent
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
            "com.google.android.inputmethod.latin"
        )
    }

    private lateinit var prefManager: PrefManager
    private lateinit var windowManager: WindowManager

    private val zoneViews = mutableMapOf<Int, ZoneOverlayView>()
    private val zoneParams = mutableMapOf<Int, WindowManager.LayoutParams>()

    private var pillView: FloatingPillView? = null
    private var pillParams: WindowManager.LayoutParams? = null

    private var currentForegroundPackage: String = ""
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

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            val detectedPkg = resolveActiveApplicationPackage(event)
            if (detectedPkg.isNotEmpty() && detectedPkg != currentForegroundPackage) {
                currentForegroundPackage = detectedPkg
                refreshOverlaysVisibilityAndState()
            }
        }
    }

    private fun resolveActiveApplicationPackage(event: AccessibilityEvent): String {
        try {
            val interactiveWindows = windows
            if (!interactiveWindows.isNullOrEmpty()) {
                val targetPackages = prefManager.selectedPackages
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
                        if (pkg.isNotEmpty() &&
                            pkg != packageName &&
                            !IGNORED_TRANSIENT_PACKAGES.contains(pkg)
                        ) {
                            return pkg
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }

        val eventPkg = event.packageName?.toString().orEmpty()
        if (eventPkg.isNotEmpty() &&
            eventPkg != packageName &&
            !IGNORED_TRANSIENT_PACKAGES.contains(eventPkg)
        ) {
            return eventPkg
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
            prefManager.isPillHiddenManually = false
            val nextMode = if (prefManager.currentMode == PrefManager.MODE_LOCK) {
                PrefManager.MODE_EDIT
            } else {
                PrefManager.MODE_LOCK
            }
            prefManager.currentMode = nextMode
            refreshOverlaysVisibilityAndState()
            return true
        }
        return super.onKeyEvent(event)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        refreshOverlaysVisibilityAndState()
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
            onModeCycleClicked = {
                val nextMode = when (prefManager.currentMode) {
                    PrefManager.MODE_LOCK -> PrefManager.MODE_EDIT
                    PrefManager.MODE_EDIT -> PrefManager.MODE_OFF
                    else -> PrefManager.MODE_LOCK
                }
                prefManager.currentMode = nextMode
                refreshOverlaysVisibilityAndState()
            },
            onCollapseToggled = { collapsed ->
                prefManager.isPillCollapsed = collapsed
                refreshOverlaysVisibilityAndState()
            },
            onHideCompletelyClicked = {
                prefManager.currentMode = PrefManager.MODE_LOCK
                prefManager.isPillHiddenManually = true
                refreshOverlaysVisibilityAndState()
                Toast.makeText(
                    this,
                    "Stealth Lock Active (100% Hidden). Press Vol Up + Vol Down together to unhide.",
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
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

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
        if (!overlaysAttached) {
            createOverlayViews()
        }

        val masterOn = prefManager.isMasterEnabled
        val filterOn = prefManager.isAppFilterEnabled
        val allowedPackages = prefManager.selectedPackages

        val shouldShowForCurrentApp = if (!filterOn) {
            true
        } else {
            currentForegroundPackage.isNotEmpty() && allowedPackages.contains(currentForegroundPackage)
        }

        val activeOverall = masterOn && shouldShowForCurrentApp
        val mode = prefManager.currentMode
        val opacity = prefManager.editOpacityPercent
        val lockBorder = prefManager.showLockBorder

        for (zoneId in 1..PrefManager.TOTAL_ZONES) {
            val view = zoneViews[zoneId] ?: continue
            val params = zoneParams[zoneId] ?: continue
            val cfg = prefManager.getZoneConfig(zoneId)

            if (!activeOverall || !cfg.enabled || mode == PrefManager.MODE_OFF) {
                view.visibility = View.GONE
            } else {
                view.visibility = View.VISIBLE
                params.x = cfg.x
                params.y = cfg.y
                params.width = cfg.width
                params.height = cfg.height

                params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()

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

        pillView?.let { pill ->
            val pParams = pillParams ?: return@let
            val shouldHidePillStealth =
                prefManager.isPillHiddenManually ||
                        (mode == PrefManager.MODE_LOCK && prefManager.hidePillInLockMode)

            if (!activeOverall || shouldHidePillStealth) {
                pill.visibility = View.GONE
            } else {
                pill.visibility = View.VISIBLE
                pParams.x = prefManager.pillX
                pParams.y = prefManager.pillY
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
        removeAllOverlays()
        instance = null
        super.onDestroy()
    }
}
