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
                    selectZoneForFineTune(id)
                }
            }
            zoneButtons.add(b)
            zoneSelectorRow.addView(b)
        }
        card.addView(zoneSelectorRow)

        tvSelectedZoneInfo = TextView(context).apply {
            text = "Editing Z1"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, (2 * density).toInt(), 0, (4 * density).toInt())
        }
        card.addView(tvSelectedZoneInfo)

        seekX = addSliderRow(card, "X Pos", 2600) { v -> updateSelectedZoneBounds(newX = v) }
        seekY = addSliderRow(card, "Y Pos", 1200) { v -> updateSelectedZoneBounds(newY = v) }
        seekW = addSliderRow(card, "Width", 2644) { v -> updateSelectedZoneBounds(newW = v.coerceAtLeast(30)) }
        seekH = addSliderRow(card, "Height", 1208) { v -> updateSelectedZoneBounds(newH = v.coerceAtLeast(30)) }

        addDivider(card)

        // 5. BOTTOM ROW: OPEN MAIN APP & CLOSE MENU
        val bottomRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val btnOpenApp = createCompactButton("📱 Full App Settings", "#242424", "#CCCCCC", 1f) {
            onOpenAppClicked()
            onDismissClicked()
        }
        val btnClose = createCompactButton("✕ Close Menu", "#1E1E1E", "#FF5252", 1f) {
            onDismissClicked()
        }

        bottomRow.addView(btnOpenApp)
        bottomRow.addView(btnClose)
        card.addView(bottomRow)

        scrollWrapper.addView(card)
        addView(scrollWrapper, scrollParams)

        selectZoneForFineTune(1)
    }

    private fun selectZoneForFineTune(zoneId: Int) {
        selectedZoneForFineTune = zoneId
        isProgrammaticUpdate = true

        zoneButtons.forEachIndexed { idx, btn ->
            val active = (idx + 1 == zoneId)
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor(if (active) "#FF1744" else "#242424"))
                cornerRadius = 6f * density
            }
            btn.background = bg
        }

        val cfg = prefManager.getZoneConfig(zoneId)
        tvSelectedZoneInfo.text = "${cfg.title}  [X:${cfg.x}  Y:${cfg.y}  W:${cfg.width}  H:${cfg.height}]"

        seekX.progress = cfg.x.coerceIn(0, seekX.max)
        seekY.progress = cfg.y.coerceIn(0, seekY.max)
        seekW.progress = cfg.width.coerceIn(30, seekW.max)
        seekH.progress = cfg.height.coerceIn(30, seekH.max)

        isProgrammaticUpdate = false
    }

    private fun updateSelectedZoneBounds(
        newX: Int? = null,
        newY: Int? = null,
        newW: Int? = null,
        newH: Int? = null
    ) {
        if (isProgrammaticUpdate) return
        val cfg = prefManager.getZoneConfig(selectedZoneForFineTune)
        val x = newX ?: cfg.x
        val y = newY ?: cfg.y
        val w = newW ?: cfg.width
        val h = newH ?: cfg.height

        // Automatically enable the zone if user is actively adjusting it
        if (!cfg.enabled) {
            prefManager.setZoneEnabled(selectedZoneForFineTune, true)
            zoneCheckBoxes[selectedZoneForFineTune]?.isChecked = true
        }

        // Switch to Edit Mode temporarily so user sees the bar moving live
        if (prefManager.currentMode == PrefManager.MODE_OFF) {
            prefManager.currentMode = PrefManager.MODE_EDIT
        }

        prefManager.saveZoneBounds(selectedZoneForFineTune, x, y, w, h)
        tvSelectedZoneInfo.text = "${cfg.title}  [X:$x  Y:$y  W:$w  H:$h]"
        onStateChanged()
    }

    private fun addSliderRow(
        parent: LinearLayout,
        label: String,
        maxVal: Int,
        onChanged: (Int) -> Unit
    ): SeekBar {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (30 * density).toInt()
            )
        }

        val tv = TextView(context).apply {
            text = label
            setTextColor(Color.parseColor("#CCCCCC"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            layoutParams = LinearLayout.LayoutParams((48 * density).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val btnMinus = Button(context).apply {
            text = "-10"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 0)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#2C2C2C"))
                cornerRadius = 4f * density
            }
            layoutParams = LinearLayout.LayoutParams((34 * density).toInt(), (24 * density).toInt())
        }

        val seek = SeekBar(context).apply {
            max = maxVal
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) onChanged(progress)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }

        val btnPlus = Button(context).apply {
            text = "+10"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 0)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#2C2C2C"))
                cornerRadius = 4f * density
            }
            layoutParams = LinearLayout.LayoutParams((34 * density).toInt(), (24 * density).toInt())
        }

        btnMinus.setOnClickListener {
            val next = (seek.progress - 10).coerceAtLeast(0)
            seek.progress = next
            onChanged(next)
        }

        btnPlus.setOnClickListener {
            val next = (seek.progress + 10).coerceAtMost(maxVal)
            seek.progress = next
            onChanged(next)
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
