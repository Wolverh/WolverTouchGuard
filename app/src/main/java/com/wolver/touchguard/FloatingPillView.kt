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
import kotlin.math.abs

@SuppressLint("ViewConstructor")
class FloatingPillView(
    context: Context,
    private val onModeCycleClicked: () -> Unit,
    private val onCollapseToggled: (collapsed: Boolean) -> Unit,
    private val onHideCompletelyClicked: () -> Unit,
    private val onPositionMoved: (x: Int, y: Int) -> Unit
) : LinearLayout(context) {

    private val density = resources.displayMetrics.density
    private val tvDragAndStatus: TextView
    private val tvCollapseBtn: TextView
    private val tvHideFullBtn: TextView

    private var currentMode: Int = PrefManager.MODE_EDIT
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
            setColor(Color.parseColor("#DD0A0A0A"))
            cornerRadius = 20f * density
            setStroke((1.5f * density).toInt(), Color.parseColor("#66FFFFFF"))
        }
        background = bg
        val padH = (10 * density).toInt()
        val padV = (5 * density).toInt()
        setPadding(padH, padV, padH, padV)

        tvDragAndStatus = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding((4 * density).toInt(), (2 * density).toInt(), (6 * density).toInt(), (2 * density).toInt())
        }

        tvCollapseBtn = TextView(context).apply {
            text = " – "
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#B0B0B0"))
            gravity = Gravity.CENTER
            setPadding((6 * density).toInt(), (2 * density).toInt(), (4 * density).toInt(), (2 * density).toInt())
            setOnClickListener {
                isCollapsed = !isCollapsed
                applyVisualState()
                onCollapseToggled(isCollapsed)
            }
        }

        tvHideFullBtn = TextView(context).apply {
            text = " ✕ "
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#FF5252"))
            gravity = Gravity.CENTER
            setPadding((4 * density).toInt(), (2 * density).toInt(), (4 * density).toInt(), (2 * density).toInt())
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
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = posX
                    startY = posY
                    isDragging = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downRawX).toInt()
                    val dy = (event.rawY - downRawY).toInt()
                    if (abs(dx) > (6 * density) || abs(dy) > (6 * density)) {
                        isDragging = true
                    }
                    if (isDragging) {
                        val metrics = resources.displayMetrics
                        posX = (startX + dx).coerceIn(0, (metrics.widthPixels - width).coerceAtLeast(0))
                        posY = (startY + dy).coerceIn(0, (metrics.heightPixels - height).coerceAtLeast(0))
                        onPositionMoved(posX, posY)
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        if (isCollapsed) {
                            isCollapsed = false
                            applyVisualState()
                            onCollapseToggled(false)
                        } else {
                            onModeCycleClicked()
                        }
                    }
                    isDragging = false
                    true
                }

                else -> false
            }
        }
    }

    fun updatePillState(mode: Int, collapsed: Boolean, x: Int, y: Int) {
        currentMode = mode
        isCollapsed = collapsed
        posX = x
        posY = y
        applyVisualState()
    }

    private fun applyVisualState() {
        if (isCollapsed) {
            tvCollapseBtn.visibility = GONE
            tvHideFullBtn.visibility = GONE
            tvDragAndStatus.text = "WG"
            alpha = 0.50f
            when (currentMode) {
                PrefManager.MODE_LOCK -> tvDragAndStatus.setTextColor(Color.parseColor("#00E676"))
                PrefManager.MODE_EDIT -> tvDragAndStatus.setTextColor(Color.parseColor("#FF1744"))
                else -> tvDragAndStatus.setTextColor(Color.parseColor("#9E9E9E"))
            }
            return
        }

        alpha = 0.92f
        tvCollapseBtn.visibility = VISIBLE
        tvHideFullBtn.visibility = VISIBLE
        when (currentMode) {
            PrefManager.MODE_LOCK -> {
                tvDragAndStatus.text = "LOCKED"
                tvDragAndStatus.setTextColor(Color.parseColor("#00E676"))
            }
            PrefManager.MODE_EDIT -> {
                tvDragAndStatus.text = "EDIT ZONES"
                tvDragAndStatus.setTextColor(Color.parseColor("#FF1744"))
            }
            PrefManager.MODE_OFF -> {
                tvDragAndStatus.text = "GUARD OFF"
                tvDragAndStatus.setTextColor(Color.parseColor("#9E9E9E"))
            }
        }
    }
}
