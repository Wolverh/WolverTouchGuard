package com.wolver.touchguard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.hypot

@SuppressLint("ViewConstructor")
class FloatingPillView(
    context: Context,
    private val onPillTapped: () -> Unit,
    private val onCollapseToggled: (collapsed: Boolean) -> Unit,
    private val onHideCompletelyClicked: () -> Unit,
    private val onPositionMoved: (x: Int, y: Int) -> Unit
) : LinearLayout(context) {

    private val density = resources.displayMetrics.density
    private val tvDragAndStatus: TextView
    private val tvCollapseBtn: TextView
    private val tvHideFullBtn: TextView

    private var currentMode: Int = PrefManager.MODE_LOCK
    private var isCollapsed: Boolean = false

    private var posX: Int = 950
    private var posY: Int = 24
    private var downRawX = 0f
    private var downRawY = 0f
    private var startX = 0
    private var startY = 0
    private var isDragging = false

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        val bg = GradientDrawable().apply {
            setColor(Color.parseColor("#F2101010"))
            cornerRadius = 22f * density
            setStroke((1.8f * density).toInt(), Color.parseColor("#9900E676"))
        }
        background = bg
        val padH = (12 * density).toInt()
        val padV = (6 * density).toInt()
        setPadding(padH, padV, padH, padV)

        tvDragAndStatus = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding((6 * density).toInt(), (4 * density).toInt(), (8 * density).toInt(), (4 * density).toInt())
        }

        tvCollapseBtn = TextView(context).apply {
            text = " – "
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#E0E0E0"))
            gravity = Gravity.CENTER
            val p = (8 * density).toInt()
            setPadding(p, (2 * density).toInt(), p, (2 * density).toInt())
            setOnClickListener {
                isCollapsed = !isCollapsed
                applyVisualState()
                onCollapseToggled(isCollapsed)
            }
        }

        tvHideFullBtn = TextView(context).apply {
            text = " ✕ "
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#FF5252"))
            gravity = Gravity.CENTER
            val p = (8 * density).toInt()
            setPadding(p, (2 * density).toInt(), p, (2 * density).toInt())
            setOnClickListener {
                onHideCompletelyClicked()
            }
        }

        addView(tvDragAndStatus)
        addView(tvCollapseBtn)
        addView(tvHideFullBtn)

        setupTouchHandling()
        applyVisualState()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchHandling() {
        tvDragAndStatus.setOnTouchListener { _, event ->
            handleDragOrTap(event)
        }
        this.setOnTouchListener { _, event ->
            handleDragOrTap(event)
        }
    }

    private fun handleDragOrTap(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = event.rawX
                downRawY = event.rawY
                startX = posX
                startY = posY
                isDragging = false
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downRawX
                val dy = event.rawY - downRawY
                if (hypot(dx, dy) > (8 * density)) {
                    isDragging = true
                }
                if (isDragging) {
                    val metrics = resources.displayMetrics
                    val maxW = (metrics.widthPixels - width).coerceAtLeast(0)
                    val maxH = (metrics.heightPixels - height).coerceAtLeast(0)
                    posX = (startX + dx.toInt()).coerceIn(0, maxW)
                    posY = (startY + dy.toInt()).coerceIn(0, maxH)
                    onPositionMoved(posX, posY)
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    if (isCollapsed) {
                        isCollapsed = false
                        applyVisualState()
                        onCollapseToggled(false)
                    } else {
                        onPillTapped()
                    }
                }
                isDragging = false
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                return true
            }
        }
        return false
    }

    fun updatePillState(mode: Int, collapsed: Boolean, x: Int, y: Int) {
        currentMode = mode
        isCollapsed = collapsed
        posX = x
        posY = y
        applyVisualState()
    }

    private fun applyVisualState() {
        val bg = background as? GradientDrawable
        if (isCollapsed) {
            tvCollapseBtn.visibility = GONE
            tvHideFullBtn.visibility = GONE
            tvDragAndStatus.text = "WG ⚙"
            alpha = 0.75f
            when (currentMode) {
                PrefManager.MODE_LOCK -> {
                    tvDragAndStatus.setTextColor(Color.parseColor("#00E676"))
                    bg?.setStroke((1.5f * density).toInt(), Color.parseColor("#00E676"))
                }
                PrefManager.MODE_EDIT -> {
                    tvDragAndStatus.setTextColor(Color.parseColor("#FF1744"))
                    bg?.setStroke((1.5f * density).toInt(), Color.parseColor("#FF1744"))
                }
                else -> {
                    tvDragAndStatus.setTextColor(Color.parseColor("#9E9E9E"))
                    bg?.setStroke((1.5f * density).toInt(), Color.parseColor("#666666"))
                }
            }
            return
        }

        alpha = 0.96f
        tvCollapseBtn.visibility = VISIBLE
        tvHideFullBtn.visibility = VISIBLE
        when (currentMode) {
            PrefManager.MODE_LOCK -> {
                tvDragAndStatus.text = "🔒 LOCKED (Menu)"
                tvDragAndStatus.setTextColor(Color.parseColor("#00E676"))
                bg?.setStroke((1.8f * density).toInt(), Color.parseColor("#00E676"))
            }
            PrefManager.MODE_EDIT -> {
                tvDragAndStatus.text = "✏️ EDITING (Menu)"
                tvDragAndStatus.setTextColor(Color.parseColor("#FF1744"))
                bg?.setStroke((1.8f * density).toInt(), Color.parseColor("#FF1744"))
            }
            PrefManager.MODE_OFF -> {
                tvDragAndStatus.text = "⏸️ PAUSED (Menu)"
                tvDragAndStatus.setTextColor(Color.parseColor("#FFB300"))
                bg?.setStroke((1.8f * density).toInt(), Color.parseColor("#FFB300"))
            }
        }
    }
}
