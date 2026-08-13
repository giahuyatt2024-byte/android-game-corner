package com.rog.gamecorner

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.animation.ValueAnimator
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

enum class HudSetting {
    X_MODE,
    FPS,
    RAM,
    TEMPERATURE,
}

/**
 * Four-tile hexagonal HUD. A tile is deliberately a touch target larger than
 * its label, making it usable over a game without needing pixel precision.
 */
class HudOverlayView(context: Context) : View(context) {
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.5f)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    private var xMode = "OFF"
    private var fps = "--"
    private var ram = "--"
    private var temperature = "--"
    private var xModeEnabled = false
    private var pulse = 0f
    private val pulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1500L
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        addUpdateListener {
            pulse = it.animatedValue as Float
            invalidate()
        }
    }
    var onSettingChanged: ((HudSetting) -> Unit)? = null
    var onClose: (() -> Unit)? = null

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        pulseAnimator.start()
    }

    fun setValues(xMode: String, fps: String, ram: String, temperature: String) {
        this.xMode = xMode
        this.fps = fps
        this.ram = ram
        this.temperature = temperature
        invalidate()
    }

    fun setXMode(enabled: Boolean) {
        xModeEnabled = enabled
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val orbitX = width * 0.32f
        val orbitY = height * 0.33f
        drawHexTile(canvas, cx - orbitX, cy - orbitY, "X-MODE", xMode, HudSetting.X_MODE)
        drawHexTile(canvas, cx + orbitX, cy - orbitY, "FPS/Hz", fps, HudSetting.FPS)
        drawHexTile(canvas, cx - orbitX, cy + orbitY, "RAM", ram, HudSetting.RAM)
        drawHexTile(canvas, cx + orbitX, cy + orbitY, "TEMP", temperature, HudSetting.TEMPERATURE)

        fillPaint.color = Color.rgb(26, 29, 41)
        canvas.drawCircle(cx, cy, dp(35f), fillPaint)
        strokePaint.color = Color.argb(
            (180 + pulse * 70).toInt(),
            if (xModeEnabled) 189 else 91,
            if (xModeEnabled) 143 else 227,
            255,
        )
        strokePaint.setShadowLayer(dp(8f + pulse * 5f), 0f, 0f, Color.rgb(91, 227, 255))
        canvas.drawCircle(cx, cy, dp(35f), strokePaint)
        strokePaint.clearShadowLayer()
        textPaint.color = Color.rgb(189, 143, 255)
        textPaint.textSize = dp(13f)
        canvas.drawText("ROG", cx, cy + dp(5f), textPaint)
        textPaint.color = Color.rgb(155, 161, 179)
        textPaint.textSize = dp(8f)
        canvas.drawText("TAP", cx, cy + dp(18f), textPaint)
    }

    override fun onDetachedFromWindow() {
        pulseAnimator.cancel()
        super.onDetachedFromWindow()
    }

    private fun drawHexTile(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        label: String,
        value: String,
        setting: HudSetting,
    ) {
        val radius = dp(48f)
        val path = Path()
        for (i in 0 until 6) {
            val angle = Math.PI / 3 * i - Math.PI / 6
            val x = cx + cos(angle).toFloat() * radius
            val y = cy + sin(angle).toFloat() * radius
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        fillPaint.color = Color.argb(238, 17, 20, 30)
        canvas.drawPath(path, fillPaint)
        strokePaint.color = when (setting) {
            HudSetting.X_MODE -> if (value == "ON") {
                Color.rgb(189, 143, 255)
            } else {
                Color.rgb(75, 103, 139)
            }
            else -> Color.rgb(91, 227, 255)
        }
        canvas.drawPath(path, strokePaint)

        textPaint.textSize = dp(9f)
        textPaint.color = Color.rgb(153, 160, 179)
        canvas.drawText(label, cx, cy - dp(5f), textPaint)
        textPaint.textSize = dp(13f)
        textPaint.color = Color.WHITE
        canvas.drawText(value, cx, cy + dp(15f), textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked != MotionEvent.ACTION_UP) return true
        val centerX = width / 2f
        val centerY = height / 2f
        val x = event.x
        val y = event.y
        if (distance(x, y, centerX, centerY) < dp(42f)) {
            onClose?.invoke()
            return true
        }

        val setting = when {
            x < centerX && y < centerY -> HudSetting.X_MODE
            x >= centerX && y < centerY -> HudSetting.FPS
            x < centerX -> HudSetting.RAM
            else -> HudSetting.TEMPERATURE
        }
        onSettingChanged?.invoke(setting)
        performClick()
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float =
        kotlin.math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2))

    private fun dp(value: Float): Float =
        value * resources.displayMetrics.density
}