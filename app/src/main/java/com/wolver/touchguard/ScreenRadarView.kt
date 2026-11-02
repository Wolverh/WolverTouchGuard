package com.wolver.touchguard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class ScreenRadarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density

    var maxVirtualWidth: Float = 2644f
    var maxVirtualHeight: Float = 1208f

    var selectedZoneId: Int = 1
        set(value) {
            field = value
            invalidate()
        }

    var onZoneSelected: ((Int) -> Unit)? = null
    var prefManager: PrefManager? = null

    private val outerFramePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0D0D0D")
        style = Paint.Style.FILL
    }

    private val screenBezelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1C1C1C")
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#333333")
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#262626")
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
        pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f)
    }

    private val zoneFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val zoneStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 10f * density
        isFakeBoldText = true
        setShadowLayer(4f, 0f, 0f, Color.BLACK)
    }

    private val phoneScreenRect = RectF()
    private val zoneRect = RectF()

    init {
        val metrics = resources.displayMetrics
        val screenW = metrics.widthPixels.toFloat()
        val screenH = metrics.heightPixels.toFloat()
        maxVirtualWidth = maxOf(screenW, screenH, 2600f)
        maxVirtualHeight = minOf(screenW, screenH, 1200f)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val desiredHeight = if (heightMode == MeasureSpec.EXACTLY) {
            heightSize
        } else {
            // Keep a clean fixed 20:9 phone aspect ratio (approx 2.2:1)
            (width / 2.2f).toInt().coerceIn((140f * density).toInt(), (220f * density).toInt())
        }
        setMeasuredDimension(width, desiredHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val padX = 6f * density
        val padY = 6f * density
        phoneScreenRect.set(padX, padY, width - padX, height - padY)

        // Draw phone bezel
        canvas.drawRoundRect(phoneScreenRect, 12f * density, 12f * density, screenBezelPaint)
        canvas.drawRoundRect(phoneScreenRect, 12f * density, 12f * density, borderPaint)

        // Draw center screen alignment crosshair lines
        val midX = phoneScreenRect.centerX()
        val midY = phoneScreenRect.centerY()
        canvas.drawLine(midX, phoneScreenRect.top, midX, phoneScreenRect.bottom, gridPaint)
        canvas.drawLine(phoneScreenRect.left, midY, phoneScreenRect.right, midY, gridPaint)

        val prefs = prefManager ?: return
        val radarW = phoneScreenRect.width()
        val radarH = phoneScreenRect.height()

        val scaleX = radarW / maxVirtualWidth
        val scaleY = radarH / maxVirtualHeight

        // Draw unselected zones first
        for (id in 1..PrefManager.TOTAL_ZONES) {
            if (id == selectedZoneId) continue
            drawZoneOnRadar(canvas, prefs, id, scaleX, scaleY, isSelected = false)
        }

        // Draw selected zone on top
        drawZoneOnRadar(canvas, prefs, selectedZoneId, scaleX, scaleY, isSelected = true)

        // Screen watermark text
        labelPaint.color = Color.parseColor("#555555")
        labelPaint.textSize = 8.5f * density
        canvas.drawText("SCREEN MONITOR 2644x1208", phoneScreenRect.left + 8f * density, phoneScreenRect.top + 14f * density, labelPaint)
    }

    private fun drawZoneOnRadar(
        canvas: Canvas,
        prefs: PrefManager,
        zoneId: Int,
        scaleX: Float,
        scaleY: Float,
        isSelected: Boolean
    ) {
        val cfg = prefs.getZoneConfig(zoneId)
        if (!cfg.enabled) return

        val left = phoneScreenRect.left + (cfg.x * scaleX)
        val top = phoneScreenRect.top + (cfg.y * scaleY)
        val right = (left + (cfg.width * scaleX)).coerceAtMost(phoneScreenRect.right)
        val bottom = (top + (cfg.height * scaleY)).coerceAtMost(phoneScreenRect.bottom)

        zoneRect.set(left, top, right, bottom)

        if (isSelected) {
            zoneFillPaint.color = Color.argb(175, 255, 23, 68)
            zoneStrokePaint.color = Color.parseColor("#FFFF1744")
            zoneStrokePaint.strokeWidth = 2.5f * density
        } else {
            zoneFillPaint.color = Color.argb(85, 41, 121, 255)
            zoneStrokePaint.color = Color.parseColor("#882979FF")
            zoneStrokePaint.strokeWidth = 1.5f * density
        }

        canvas.drawRoundRect(zoneRect, 3f * density, 3f * density, zoneFillPaint)
        canvas.drawRoundRect(zoneRect, 3f * density, 3f * density, zoneStrokePaint)

        // Draw zone badge
        labelPaint.color = if (isSelected) Color.WHITE else Color.parseColor("#CCCCCC")
        labelPaint.textSize = 9.5f * density
        val text = "Z$zoneId"
        val textX = (zoneRect.left + 3f * density).coerceAtMost(phoneScreenRect.right - 22f * density)
        val textY = (zoneRect.top + 12f * density).coerceAtMost(phoneScreenRect.bottom - 4f * density)
        canvas.drawText(text, textX, textY, labelPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            val prefs = prefManager ?: return super.onTouchEvent(event)
            val radarW = phoneScreenRect.width()
            val radarH = phoneScreenRect.height()
            val scaleX = radarW / maxVirtualWidth
            val scaleY = radarH / maxVirtualHeight

            for (id in 1..PrefManager.TOTAL_ZONES) {
                val cfg = prefs.getZoneConfig(id)
                if (!cfg.enabled) continue
                val left = phoneScreenRect.left + (cfg.x * scaleX)
                val top = phoneScreenRect.top + (cfg.y * scaleY)
                val right = left + (cfg.width * scaleX)
                val bottom = top + (cfg.height * scaleY)

                val hitRect = RectF(
                    left - 12f * density,
                    top - 12f * density,
                    right + 12f * density,
                    bottom + 12f * density
                )

                if (hitRect.contains(event.x, event.y)) {
                    selectedZoneId = id
                    onZoneSelected?.invoke(id)
                    invalidate()
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }
}
