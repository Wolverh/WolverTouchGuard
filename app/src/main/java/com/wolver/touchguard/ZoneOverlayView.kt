package com.wolver.touchguard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.max

@SuppressLint("ViewConstructor")
class ZoneOverlayView(
    context: Context,
    val zoneId: Int,
    private val title: String,
    private val onBoundsUpdated: (zoneId: Int, x: Int, y: Int, width: Int, height: Int) -> Unit
) : View(context) {

    companion object {
        private const val HANDLE_NONE = 0
        private const val HANDLE_TOP_LEFT = 1
        private const val HANDLE_TOP_RIGHT = 2
        private const val HANDLE_BOTTOM_LEFT = 3
        private const val HANDLE_BOTTOM_RIGHT = 4
    }

    var isPillAt: ((rawX: Float, rawY: Float) -> Boolean)? = null
    var onPillTouchForward: ((event: MotionEvent) -> Unit)? = null

    private var currentMode: Int = PrefManager.MODE_LOCK
    private var editOpacityPercent: Int = 50
    private var showLockBorder: Boolean = false

    private var posX: Int = 0
    private var posY: Int = 0
    private var zoneW: Int = 200
    private var zoneH: Int = 200

    private val density = resources.displayMetrics.density
    private val handleSizePx = 30f * density
    private val touchSlopHandlePx = 42f * density
    private val minWidthPx = (30f * density).toInt()
    private val minHeightPx = (30f * density).toInt()

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }

    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#EEFFFFFF")
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 11f * density
        isFakeBoldText = true
        setShadowLayer(4f, 0f, 0f, Color.BLACK)
    }

    private val handleRect = RectF()

    private var activeHandle = HANDLE_NONE
    private var initialTouchRawX = 0f
    private var initialTouchRawY = 0f
    private var initialX = 0
    private var initialY = 0
    private var initialW = 0
    private var initialH = 0

    fun updateState(
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        mode: Int,
        opacityPercent: Int,
        lockBorder: Boolean
    ) {
        posX = x
        posY = y
        zoneW = max(minWidthPx, w)
        zoneH = max(minHeightPx, h)
        currentMode = mode
        editOpacityPercent = opacityPercent
        showLockBorder = lockBorder
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        when (currentMode) {
            PrefManager.MODE_EDIT -> {
                val alpha = ((editOpacityPercent / 100f) * 255).toInt().coerceIn(30, 225)
                fillPaint.color = Color.argb(alpha, 255, 23, 68)
                canvas.drawRect(0f, 0f, w, h, fillPaint)

                borderPaint.color = Color.parseColor("#FFFF1744")
                borderPaint.strokeWidth = 2f * density
                canvas.drawRect(1f, 1f, w - 1f, h - 1f, borderPaint)

                handleRect.set(0f, 0f, handleSizePx, handleSizePx)
                canvas.drawRect(handleRect, handlePaint)

                handleRect.set(w - handleSizePx, 0f, w, handleSizePx)
                canvas.drawRect(handleRect, handlePaint)

                handleRect.set(0f, h - handleSizePx, handleSizePx, h)
                canvas.drawRect(handleRect, handlePaint)

                handleRect.set(w - handleSizePx, h - handleSizePx, w, h)
                canvas.drawRect(handleRect, handlePaint)

                val info = "Z$zoneId [${zoneW}x${zoneH}]"
                val textY = (h / 2f).coerceIn(18f * density, (h - 8f * density).coerceAtLeast(18f * density))
                canvas.drawText(info, 8f * density, textY, textPaint)
            }

            PrefManager.MODE_LOCK -> {
                if (showLockBorder) {
                    fillPaint.color = Color.argb(18, 0, 230, 118)
                    canvas.drawRect(0f, 0f, w, h, fillPaint)
                    borderPaint.color = Color.parseColor("#5500E676")
                    borderPaint.strokeWidth = 1.5f * density
                    canvas.drawRect(1f, 1f, w - 1f, h - 1f, borderPaint)
                } else {
                    // Draw a 1/255 alpha pixel so Android hardware compositor NEVER optimizes away hit-testing
                    fillPaint.color = Color.argb(1, 0, 0, 0)
                    canvas.drawRect(0f, 0f, w, h, fillPaint)
                }
            }

            PrefManager.MODE_OFF -> {
            }
        }
    }

    private fun detectTouchedHandle(localX: Float, localY: Float): Int {
        val w = width.toFloat()
        val h = height.toFloat()

        val nearLeft = localX <= touchSlopHandlePx
        val nearRight = localX >= w - touchSlopHandlePx
        val nearTop = localY <= touchSlopHandlePx
        val nearBottom = localY >= h - touchSlopHandlePx

        return when {
            nearBottom && nearRight -> HANDLE_BOTTOM_RIGHT
            nearTop && nearLeft -> HANDLE_TOP_LEFT
            nearTop && nearRight -> HANDLE_TOP_RIGHT
            nearBottom && nearLeft -> HANDLE_BOTTOM_LEFT
            else -> HANDLE_NONE
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isPillAt?.invoke(event.rawX, event.rawY) == true) {
            onPillTouchForward?.invoke(event)
            return true
        }

        // In LOCK mode: swallow 100% of touches silently so accidental touches never reach the game
        if (currentMode == PrefManager.MODE_LOCK) {
            return true
        }

        if (currentMode == PrefManager.MODE_OFF) {
            return false
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialTouchRawX = event.rawX
                initialTouchRawY = event.rawY
                initialX = posX
                initialY = posY
                initialW = zoneW
                initialH = zoneH
                activeHandle = detectTouchedHandle(event.x, event.y)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - initialTouchRawX).toInt()
                val dy = (event.rawY - initialTouchRawY).toInt()

                val metrics = resources.displayMetrics
                val maxScreenW = maxOf(metrics.widthPixels, metrics.heightPixels, 2644)
                val maxScreenH = maxOf(metrics.widthPixels, metrics.heightPixels, 2644)

                when (activeHandle) {
                    HANDLE_BOTTOM_RIGHT -> {
                        zoneW = (initialW + dx).coerceIn(minWidthPx, maxScreenW)
                        zoneH = (initialH + dy).coerceIn(minHeightPx, maxScreenH)
                    }

                    HANDLE_TOP_LEFT -> {
                        val maxNewX = (initialX + initialW - minWidthPx).coerceAtLeast(0)
                        val maxNewY = (initialY + initialH - minHeightPx).coerceAtLeast(0)
                        val newX = (initialX + dx).coerceIn(0, maxNewX)
                        val newY = (initialY + dy).coerceIn(0, maxNewY)
                        posX = newX
                        posY = newY
                        zoneW = (initialW - (newX - initialX)).coerceIn(minWidthPx, maxScreenW)
                        zoneH = (initialH - (newY - initialY)).coerceIn(minHeightPx, maxScreenH)
                    }

                    HANDLE_TOP_RIGHT -> {
                        val maxNewY = (initialY + initialH - minHeightPx).coerceAtLeast(0)
                        val newY = (initialY + dy).coerceIn(0, maxNewY)
                        posY = newY
                        zoneW = (initialW + dx).coerceIn(minWidthPx, maxScreenW)
                        zoneH = (initialH - (newY - initialY)).coerceIn(minHeightPx, maxScreenH)
                    }

                    HANDLE_BOTTOM_LEFT -> {
                        val maxNewX = (initialX + initialW - minWidthPx).coerceAtLeast(0)
                        val newX = (initialX + dx).coerceIn(0, maxNewX)
                        posX = newX
                        zoneW = (initialW - (newX - initialX)).coerceIn(minWidthPx, maxScreenW)
                        zoneH = (initialH + dy).coerceIn(minHeightPx, maxScreenH)
                    }

                    else -> {
                        posX = (initialX + dx).coerceIn(0, (maxScreenW - zoneW).coerceAtLeast(0))
                        posY = (initialY + dy).coerceIn(0, (maxScreenH - zoneH).coerceAtLeast(0))
                    }
                }

                onBoundsUpdated(zoneId, posX, posY, zoneW, zoneH)
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activeHandle = HANDLE_NONE
                onBoundsUpdated(zoneId, posX, posY, zoneW, zoneH)
                return true
            }
        }
        return true
    }
}
