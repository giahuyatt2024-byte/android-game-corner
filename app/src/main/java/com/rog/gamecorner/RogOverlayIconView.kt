package com.rog.gamecorner

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class RogOverlayIconView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var expanded = false

    init {
        contentDescription = "Mở GAME CORNER HUD"
        isClickable = true
    }

    fun setExpanded(value: Boolean) {
        expanded = value
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = width * 0.34f

        paint.shader = RadialGradient(
            cx,
            cy,
            width * 0.62f,
            intArrayOf(Color.argb(155, 72, 228, 255), Color.TRANSPARENT),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, width * 0.62f, paint)

        paint.shader = null
        paint.color = Color.rgb(13, 15, 23)
        canvas.drawCircle(cx, cy, radius * 1.22f, paint)

        val hex = Path()
        for (i in 0 until 6) {
            val angle = Math.PI / 3 * i - Math.PI / 6
            val x = cx + cos(angle).toFloat() * radius
            val y = cy + sin(angle).toFloat() * radius
            if (i == 0) hex.moveTo(x, y) else hex.lineTo(x, y)
        }
        hex.close()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = width * 0.045f
        paint.color = if (expanded) Color.rgb(189, 143, 255) else Color.rgb(91, 227, 255)
        canvas.drawPath(hex, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(157, 105, 255)
        canvas.drawOval(
            cx - radius * 0.18f,
            cy - radius * 0.40f,
            cx + radius * 0.18f,
            cy + radius * 0.40f,
            paint,
        )
        paint.color = Color.rgb(239, 252, 255)
        canvas.drawCircle(cx, cy - radius * 0.24f, radius * 0.07f, paint)
    }
}