package com.wolver.touchguard

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
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
            "com.google.android.inputmethod.latin"
        )
    }

    private lateinit var prefManager: PrefManager
    private lateinit var windowManager: WindowManager

    private val zoneViews = mutableMapOf<Int, ZoneOverlayView>()
    private val zoneParams = mutableMapOf<Int, WindowManager.LayoutParams>()

    private var pillView: FloatingPillView? = null
    private var pillParams: WindowManager.LayoutParams? = null

    private var menuView: FloatingMenuView? = null

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
                        if (pkg == packageName) {
                            return pkg
                        }
                    }
                }
                for (win in interactiveWindows) {
                    if (win.type == AccessibilityWindowInfo.TYPE_APPLICATION) {
                        val pkg = win.root?.packageName?.toString().orEmpty()
                        if (pkg.isNotEmpty() &&
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

            // Vol Up + Vol Down immediately unhides toggle if hidden, or switches modes
            if (prefManager.hideFloatingToggleAlways || prefManager.isPillHiddenManually) {
                prefManager.hideFloatingToggleAlways = false
                prefManager.isPillHiddenManually = false
                Toast.makeText(this, "Floating toggle unhidden!", Toast.LENGTH_SHORT).show()
            } else {
                val nextMode = if (prefManager.currentMode == PrefManager.MODE_LOCK) {
                    PrefManager.MODE_EDIT
                } else {
                    PrefManager.MODE_LOCK
                }
                prefManager.currentMode = nextMode
            }
            refreshOverlaysVisibilityAndState()
            return true
        }
        return super.onKeyEvent(event)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        refreshOverlaysVisibilityAndState()
    }

    private fun showFloatingMenu() {
        if (menuView != null) return

        val menu = FloatingMenuView(
            context = this,
            onLockClicked = {
                prefManager.currentMode = PrefManager.MODE_LOCK
                refreshOverlaysVisibilityAndState()
                Toast.makeText(this, "Lock Mode Active (Touch Blocking ON)", Toast.LENGTH_SHORT).show()
            },
            onEditClicked = {
                prefManager.currentMode = PrefManager.MODE_EDIT
                prefManager.showOverlayInApp = true
                refreshOverlaysVisibilityAndState()
                Toast.makeText(this, "Edit Mode Active (Bars Visible)", Toast.LENGTH_SHORT).show()
            },
            onHideToggleClicked = {
                // Completely hide toggle when chosen from menu
                prefManager.hideFloatingToggleAlways = true
                prefManager.isPillHiddenManually = true
                refreshOverlaysVisibilityAndState()
                Toast.makeText(
                    this,
                    "Floating toggle is now HIDDEN. Re-enable in app or press Vol Up + Vol Down.",
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
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

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

            // Touch forwarding: ensures clicking the toggle always works even if inside a blocked zone!
            view.isPillAt = { rawX, rawY ->
                val pill = pillView
                if (pill != null && pill.visibility == View.VISIBLE) {
                    val loc = IntArray(2)
                    pill.getLocationOnScreen(loc)
                    val pLeft = loc[0].toFloat()
                    val pTop = loc[1].toFloat()
                    val pRight = pLeft + pill.width.toFloat()
                    val pBottom = pTop + pill.height.toFloat()
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
                // Clicking the toggle shows the Quick Menu with [Hide Toggle] option!
                showFloatingMenu()
            },
            onCollapseToggled = { collapsed ->
                prefManager.isPillCollapsed = collapsed
                refreshOverlaysVisibilityAndState()
            },
            onHideCompletelyClicked = {
                // Clicking ✕ on the pill hides it completely!
                prefManager.hideFloatingToggleAlways = true
                prefManager.isPillHiddenManually = true
                refreshOverlaysVisibilityAndState()
                Toast.makeText(
                    this,
                    "Floating toggle HIDDEN. Re-enable in app or press Vol Up + Vol Down.",
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
        val mode = prefManager.currentMode

        val isSelfApp = (currentForegroundPackage == packageName) || (currentForegroundPackage.isEmpty())
        val shouldShowForCurrentApp = if (isSelfApp) {
            prefManager.showOverlayInApp || (mode == PrefManager.MODE_EDIT)
        } else if (!filterOn) {
            true
        } else {
            currentForegroundPackage.isNotEmpty() && allowedPackages.contains(currentForegroundPackage)
        }

        val activeOverall = masterOn && shouldShowForCurrentApp
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

                if (isSelfApp && mode == PrefManager.MODE_LOCK) {
                    params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                } else {
                    params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
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

        pillView?.let { pill ->
            val pParams = pillParams ?: return@let
            val shouldHidePill =
                prefManager.hideFloatingToggleAlways ||
                prefManager.isPillHiddenManually ||
                (mode == PrefManager.MODE_LOCK && prefManager.hidePillInLockMode)

            if (!activeOverall || shouldHidePill) {
                pill.visibility = View.GONE
                dismissFloatingMenu()
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
        removeAllOverlays()
        instance = null
        super.onDestroy()
    }
}
