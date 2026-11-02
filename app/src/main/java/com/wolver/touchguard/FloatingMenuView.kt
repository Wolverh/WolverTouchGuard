package com.wolver.touchguard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import java.util.Locale

@SuppressLint("ViewConstructor")
class FloatingMenuView(
    context: Context,
    private val prefManager: PrefManager,
    private val onStateChanged: () -> Unit,
    private val onHideToggleOnlyClicked: () -> Unit,
    private val onOpenAppClicked: () -> Unit,
    private val onDismissClicked: () -> Unit
) : FrameLayout(context) {

    private val density = resources.displayMetrics.density
    private var selectedZoneForFineTune: Int = 1

    private lateinit var tvSelectedZoneInfo: TextView
    private lateinit var seekX: SeekBar
    private lateinit var seekY: SeekBar
    private lateinit var seekW: SeekBar
    private lateinit var seekH: SeekBar
    private val zoneButtons = mutableListOf<Button>()
    private val zoneCheckBoxes = mutableMapOf<Int, CheckBox>()
    private var isProgrammaticUpdate = false

    init {
        setBackgroundColor(Color.parseColor("#88000000"))
        setOnClickListener { onDismissClicked() }

        val scrollWrapper = ScrollView(context).apply {
            isFillViewport = false
        }

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#F5141414"))
                cornerRadius = 16f * density
                setStroke((1.5f * density).toInt(), Color.parseColor("#44FFFFFF"))
            }
            background = bg
            val p = (14 * density).toInt()
            setPadding(p, p, p, p)
            setOnClickListener { }
        }

        val metrics = resources.displayMetrics
        val cardWidth = (metrics.widthPixels * 0.88f).toInt().coerceAtMost((440 * density).toInt())

        val scrollParams = LayoutParams(
            cardWidth,
            LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }

        // HEADER
        val tvTitle = TextView(context).apply {
            text = "WOLVER IN-GAME CONTROL CENTER"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        val tvSub = TextView(context).apply {
            text = "Adjust zones, exclude individual blocks, or hide toggle while keeping Guard ON"
            setTextColor(Color.parseColor("#AAAAAA"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
            gravity = Gravity.CENTER
            setPadding(0, (2 * density).toInt(), 0, (10 * density).toInt())
        }
        card.addView(tvTitle)
        card.addView(tvSub)

        // 1. TOP ROW: MODE SWITCHERS
        val modeRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (8 * density).toInt()
            }
        }

        val btnLock = createCompactButton("🔒 LOCK ON", "#00C853", "#000000", 1f) {
            prefManager.currentMode = PrefManager.MODE_LOCK
            onStateChanged()
            onDismissClicked()
        }
        val btnEdit = createCompactButton("✏️ EDIT BARS", "#FF1744", "#FFFFFF", 1f) {
            prefManager.currentMode = PrefManager.MODE_EDIT
            prefManager.showOverlayInApp = true
            onStateChanged()
            onDismissClicked()
        }
        val btnPause = createCompactButton("⏸️ PAUSE", "#333333", "#FFB300", 0.8f) {
            prefManager.currentMode = PrefManager.MODE_OFF
            onStateChanged()
            onDismissClicked()
        }

        modeRow.addView(btnLock)
        modeRow.addView(btnEdit)
        modeRow.addView(btnPause)
        card.addView(modeRow)

        // 2. HIDE TOGGLE ONLY (KEEP BLOCKING ACTIVE)
        val btnHideToggleOnly = createFullButton(
            text = "🚫 HIDE TOGGLE BAR (KEEP BLOCKAGE 100% ACTIVE)",
            bgColor = "#D50000",
            textColor = "#FFFFFF"
        ) {
            // Force Lock Mode ON so blockage is guaranteed active when hiding toggle!
            prefManager.currentMode = PrefManager.MODE_LOCK
            onHideToggleOnlyClicked()
            onDismissClicked()
        }
        card.addView(btnHideToggleOnly)

        // 2.5 DPI ENGINE STATUS TELEMETRY
        val profile = DpiSensitivityEngine.computeProfile(context, prefManager)
        val tvDpiStatus = TextView(context).apply {
            val multStr = String.format(Locale.US, "%.2fx", profile.horizontalMultiplier)
            text = "⚡ DPI Engine: ${profile.targetDpi} DPI ($multStr Multiplier • +${profile.yAxisBoostPercent}% Y-Boost)"
            setTextColor(Color.parseColor("#00E676"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, (6 * density).toInt(), 0, (2 * density).toInt())
        }
        card.addView(tvDpiStatus)

        addDivider(card)

        // 3. INCLUDE / EXCLUDE SPECIFIC BLOCKS (ZONE CHECKBOXES Z1 - Z6)
        val tvIncludeHeader = TextView(context).apply {
            text = "ACTIVE BLOCKS (UNCHECK TO EXCLUDE A BLOCK):"
            setTextColor(Color.parseColor("#00E676"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, (4 * density).toInt())
        }
        card.addView(tvIncludeHeader)

        val gridRow1 = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        val gridRow2 = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }

        for (id in 1..PrefManager.TOTAL_ZONES) {
            val cfg = prefManager.getZoneConfig(id)
            val cb = CheckBox(context).apply {
                text = "Z$id (${cfg.title.substringAfter(": ").take(11)})"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                isChecked = cfg.enabled
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnCheckedChangeListener { _, isChecked ->
                    prefManager.setZoneEnabled(id, isChecked)
                    onStateChanged()
                }
            }
            zoneCheckBoxes[id] = cb
            if (id <= 3) gridRow1.addView(cb) else gridRow2.addView(cb)
        }
        card.addView(gridRow1)
        card.addView(gridRow2)

        addDivider(card)

        // 4. IN-GAME LIVE ZONE ADJUSTER (SELECT Z1-Z6 AND TWEAK X, Y, W, H)
        val tvAdjustHeader = TextView(context).apply {
            text = "LIVE IN-GAME BLOCK ADJUSTER (TAP ZONE TO EDIT):"
            setTextColor(Color.parseColor("#2979FF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, (6 * density).toInt())
        }
        card.addView(tvAdjustHeader)

        val zoneSelectorRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (6 * density).toInt() }
        }

        for (id in 1..PrefManager.TOTAL_ZONES) {
            val b = Button(context).apply {
                text = "Z$id"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setTextColor(Color.WHITE)
                isAllCaps = false
                setPadding(0, 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams(0, (34 * density).toInt(), 1f).apply {
                    marginEnd = if (id < PrefManager.TOTAL_ZONES) (3 * density).toInt() else 0
                }
                setOnClickListener {
                    selectZoneForAdjustment(id)
                }
            }
            zoneButtons.add(b)
            zoneSelectorRow.addView(b)
        }
        card.addView(zoneSelectorRow)

        tvSelectedZoneInfo = TextView(context).apply {
            setTextColor(Color.parseColor("#FFD600"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, (2 * density).toInt(), 0, (4 * density).toInt())
        }
        card.addView(tvSelectedZoneInfo)

        seekX = addSliderRow(card, "X Pos (Horizontal):", 2600) { v ->
            saveZoneFineTune(x = v)
        }
        seekY = addSliderRow(card, "Y Pos (Vertical):", 1200) { v ->
            saveZoneFineTune(y = v)
        }
        seekW = addSliderRow(card, "Width:", 2644, min = 30) { v ->
            saveZoneFineTune(w = v)
        }
        seekH = addSliderRow(card, "Height:", 1208, min = 30) { v ->
            saveZoneFineTune(h = v)
        }

        addDivider(card)

        // 5. BOTTOM ACTIONS
        val bottomRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (4 * density).toInt() }
        }

        val btnOpenFullApp = createCompactButton("📱 Full Settings App", "#1E88E5", "#FFFFFF", 1.2f) {
            onOpenAppClicked()
            onDismissClicked()
        }
        val btnClose = createCompactButton("✕ Close Menu", "#263238", "#ECEFF1", 1f) {
            onDismissClicked()
        }

        bottomRow.addView(btnOpenFullApp)
        bottomRow.addView(btnClose)
        card.addView(bottomRow)

        scrollWrapper.addView(card)
        addView(scrollWrapper, scrollParams)

        selectZoneForAdjustment(1)
    }

    private fun selectZoneForAdjustment(zoneId: Int) {
        selectedZoneForFineTune = zoneId
        val cfg = prefManager.getZoneConfig(zoneId)

        zoneButtons.forEachIndexed { index, b ->
            val isSelected = (index + 1) == zoneId
            b.background = GradientDrawable().apply {
                setColor(Color.parseColor(if (isSelected) "#FF1744" else "#212121"))
                cornerRadius = 6f * density
                setStroke((1 * density).toInt(), Color.parseColor(if (isSelected) "#FFFFFF" else "#424242"))
            }
        }

        tvSelectedZoneInfo.text = "Adjusting: ${cfg.title} [${cfg.width}x${cfg.height} px]"

        isProgrammaticUpdate = true
        seekX.progress = cfg.x.coerceIn(0, seekX.max)
        seekY.progress = cfg.y.coerceIn(0, seekY.max)
        seekW.progress = cfg.width.coerceIn(30, seekW.max)
        seekH.progress = cfg.height.coerceIn(30, seekH.max)
        isProgrammaticUpdate = false
    }

    private fun saveZoneFineTune(
        x: Int? = null,
        y: Int? = null,
        w: Int? = null,
        h: Int? = null
    ) {
        if (isProgrammaticUpdate) return
        val current = prefManager.getZoneConfig(selectedZoneForFineTune)
        val newX = x ?: current.x
        val newY = y ?: current.y
        val newW = (w ?: current.width).coerceAtLeast(30)
        val newH = (h ?: current.height).coerceAtLeast(30)

        prefManager.saveZoneBounds(selectedZoneForFineTune, newX, newY, newW, newH)
        tvSelectedZoneInfo.text = "Adjusting: ${current.title} [${newW}x${newH} px]"
        onStateChanged()
    }

    private fun addSliderRow(
        parent: LinearLayout,
        label: String,
        max: Int,
        min: Int = 0,
        onChange: (Int) -> Unit
    ): SeekBar {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (2 * density).toInt() }
        }

        val tv = TextView(context).apply {
            text = label
            setTextColor(Color.parseColor("#CCCCCC"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
            layoutParams = LinearLayout.LayoutParams((110 * density).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val seek = SeekBar(context).apply {
            this.max = max
            layoutParams = LinearLayout.LayoutParams(0, (30 * density).toInt(), 1f)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        onChange(progress.coerceAtLeast(min))
                    }
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }

        val btnMinus = Button(context).apply {
            text = "-"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams((28 * density).toInt(), (28 * density).toInt()).apply {
                marginEnd = (2 * density).toInt()
            }
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#333333"))
                cornerRadius = 4f * density
            }
            setOnClickListener {
                seek.progress = (seek.progress - 10).coerceAtLeast(min)
                onChange(seek.progress)
            }
        }

        val btnPlus = Button(context).apply {
            text = "+"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams((28 * density).toInt(), (28 * density).toInt()).apply {
                marginStart = (2 * density).toInt()
            }
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#333333"))
                cornerRadius = 4f * density
            }
            setOnClickListener {
                seek.progress = (seek.progress + 10).coerceAtMost(max)
                onChange(seek.progress)
            }
        }

        row.addView(tv)
        row.addView(btnMinus)
        row.addView(seek)
        row.addView(btnPlus)
        parent.addView(row)
        return seek
    }

    private fun addDivider(parent: LinearLayout) {
        val v = View(context).apply {
            setBackgroundColor(Color.parseColor("#2E2E2E"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (1 * density).toInt()
            ).apply {
                topMargin = (8 * density).toInt()
                bottomMargin = (8 * density).toInt()
            }
        }
        parent.addView(v)
    }

    private fun createCompactButton(
        text: String,
        bgColor: String,
        textColor: String,
        weight: Float,
        onClick: () -> Unit
    ): Button {
        return Button(context).apply {
            this.text = text
            setTextColor(Color.parseColor(textColor))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 0)
            background = GradientDrawable().apply {
                setColor(Color.parseColor(bgColor))
                cornerRadius = 8f * density
            }
            layoutParams = LinearLayout.LayoutParams(0, (38 * density).toInt(), weight).apply {
                marginEnd = (4 * density).toInt()
            }
            setOnClickListener { onClick() }
        }
    }

    private fun createFullButton(
        text: String,
        bgColor: String,
        textColor: String,
        onClick: () -> Unit
    ): Button {
        return Button(context).apply {
            this.text = text
            setTextColor(Color.parseColor(textColor))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(Color.parseColor(bgColor))
                cornerRadius = 8f * density
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (40 * density).toInt()
            )
            setOnClickListener { onClick() }
        }
    }
}
