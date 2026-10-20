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

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#161616")
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#383838")
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#222222")
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
        setShadowLayer(3f, 0f, 0f, Color.BLACK)
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
        // Maintain roughly 2.1:1 landscape phone aspect ratio
        val height = (width / 2.15f).toInt()
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val pad = 8f * density
        phoneScreenRect.set(pad, pad, width - pad, height - pad)

        // Draw phone screen background
        canvas.drawRoundRect(phoneScreenRect, 14f * density, 14f * density, bgPaint)
        canvas.drawRoundRect(phoneScreenRect, 14f * density, 14f * density, borderPaint)

        // Draw center dashed crosshairs for screen orientation
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

        // Screen bezel label
        labelPaint.color = Color.parseColor("#777777")
        labelPaint.textSize = 9f * density
        canvas.drawText("SCREEN PREVIEW (LANDSCAPE)", phoneScreenRect.left + 10f * density, phoneScreenRect.top + 16f * density, labelPaint)
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
            zoneFillPaint.color = Color.argb(160, 255, 23, 68)
            zoneStrokePaint.color = Color.parseColor("#FFFF1744")
            zoneStrokePaint.strokeWidth = 2.5f * density
        } else {
            zoneFillPaint.color = Color.argb(80, 41, 121, 255)
            zoneStrokePaint.color = Color.parseColor("#662979FF")
            zoneStrokePaint.strokeWidth = 1.5f * density
        }

        canvas.drawRoundRect(zoneRect, 4f * density, 4f * density, zoneFillPaint)
        canvas.drawRoundRect(zoneRect, 4f * density, 4f * density, zoneStrokePaint)

        // Draw zone label
        labelPaint.color = if (isSelected) Color.WHITE else Color.parseColor("#DDDDDD")
        labelPaint.textSize = 10f * density
        val text = "Z$zoneId"
        val textX = (zoneRect.left + 4f * density).coerceAtMost(phoneScreenRect.right - 24f * density)
        val textY = (zoneRect.top + 14f * density).coerceAtMost(phoneScreenRect.bottom - 4f * density)
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
                    left - 10f * density,
                    top - 10f * density,
                    right + 10f * density,
                    bottom + 10f * density
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
