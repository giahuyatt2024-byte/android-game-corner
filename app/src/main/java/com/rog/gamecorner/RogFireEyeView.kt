package com.rog.gamecorner

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.animation.DecelerateInterpolator
import android.view.View
import kotlin.math.max
import kotlin.math.min
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Canvas-based opening intro inspired by the uploaded sci-fi launcher clip.
 * It uses original GAME CORNER geometry instead of shipping a large video.
 */
class RogFireEyeView(context: Context) : View(context) {
    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1100L
        interpolator = DecelerateInterpolator()
    }
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val logoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT,
            android.graphics.Typeface.BOLD,
        )
    }
    private var progress = 0f
    var onAnimationFinished: (() -> Unit)? = null

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        animator.addUpdateListener {
            progress = it.animatedValue as Float
            invalidate()
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onAnimationFinished?.invoke()
            }
        })
    }

    fun start() {
        animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = min(width, height) * 0.36f

        drawBackground(canvas, cx, cy)
        drawCircuitLines(canvas, cx, cy, radius)
        drawHorizontalFlare(canvas, cx, cy, radius)

        val logoIn = easeOut(clamp((progress - 0.14f) / 0.28f))
        val vortex = easeIn(clamp((progress - 0.58f) / 0.42f))
        drawLogo(canvas, cx, cy, logoIn, vortex)
        drawVortex(canvas, cx, cy, radius, vortex)
    }

    private fun drawBackground(canvas: Canvas, cx: Float, cy: Float) {
        backgroundPaint.shader = RadialGradient(
            cx,
            cy,
            max(width, height) * 0.78f,
            intArrayOf(
                Color.rgb(20, 26, 50),
                Color.rgb(8, 11, 24),
                Color.rgb(2, 3, 8),
            ),
            floatArrayOf(0f, 0.46f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
    }

    private fun drawCircuitLines(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
    ) {
        val reveal = easeOut(clamp(progress / 0.52f))
        linePaint.shader = null
        linePaint.strokeWidth = max(1f, radius * 0.009f)

        for (index in 0 until 16) {
            val angle = -PI * 0.92 + index * (PI * 1.84 / 15.0)
            val inner = radius * (0.18f + (index % 3) * 0.04f)
            val outer = max(width, height) * (0.58f + (index % 4) * 0.06f) * reveal
            val bend = radius * (0.25f + (index % 4) * 0.09f)
            val x1 = cx + cos(angle).toFloat() * inner
            val y1 = cy + sin(angle).toFloat() * inner
            val x2 = cx + cos(angle).toFloat() * bend
            val y2 = cy + sin(angle).toFloat() * bend
            val x3 = cx + cos(angle + 0.08).toFloat() * outer
            val y3 = cy + sin(angle + 0.08).toFloat() * outer

            linePaint.color = Color.argb(
                (100f * reveal).toInt().coerceIn(0, 100),
                if (index % 2 == 0) 91 else 189,
                if (index % 2 == 0) 227 else 143,
                255,
            )
            canvas.drawLine(x1, y1, x2, y2, linePaint)
            canvas.drawLine(x2, y2, x3, y3, linePaint)
        }

        linePaint.color = Color.argb((75f * reveal).toInt(), 91, 227, 255)
        linePaint.strokeWidth = max(1f, radius * 0.006f)
        canvas.drawCircle(cx, cy, radius * (1.04f + reveal * 0.18f), linePaint)
        canvas.drawCircle(cx, cy, radius * (1.32f + reveal * 0.25f), linePaint)
    }

    private fun drawHorizontalFlare(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
    ) {
        val sweep = easeOut(clamp(progress / 0.34f))
        val fade = if (progress < 0.2f) sweep else clamp(1f - (progress - 0.2f) / 0.43f)
        if (fade <= 0f) return

        glowPaint.shader = LinearGradient(
            0f,
            0f,
            width.toFloat(),
            0f,
            intArrayOf(Color.TRANSPARENT, Color.rgb(91, 227, 255), Color.WHITE, Color.rgb(189, 143, 255), Color.TRANSPARENT),
            floatArrayOf(0f, 0.31f, 0.5f, 0.69f, 1f),
            Shader.TileMode.CLAMP,
        )
        glowPaint.alpha = (210f * fade).toInt()
        canvas.drawRect(0f, cy - radius * 0.012f, width.toFloat(), cy + radius * 0.012f, glowPaint)
        glowPaint.shader = RadialGradient(
            cx,
            cy,
            radius * (0.4f + sweep * 1.9f),
            Color.argb((255f * fade).toInt(), 255, 255, 255),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, radius * (0.45f + sweep * 1.45f), glowPaint)
        glowPaint.alpha = 255
    }

    private fun drawLogo(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        logoIn: Float,
        vortex: Float,
    ) {
        if (logoIn <= 0f) return
        val scale = (0.68f + logoIn * 0.32f) * (1f - vortex * 0.34f)
        val alpha = (255f * logoIn * (1f - vortex * 0.72f)).toInt().coerceIn(0, 255)
        val fontSize = min(width, height) * 0.11f

        canvas.save()
        canvas.scale(scale, scale, cx, cy)
        logoPaint.textSize = fontSize
        logoPaint.letterSpacing = 0.12f
        logoPaint.color = Color.argb(alpha, 237, 241, 255)
        logoPaint.setShadowLayer(fontSize * 0.45f, 0f, 0f, Color.argb(alpha, 91, 227, 255))

        val game = "GAME"
        val corner = "CORNER"
        val gap = fontSize * 0.44f
        val markWidth = fontSize * 0.85f
        val totalWidth = logoPaint.measureText(game) + gap + markWidth + gap + logoPaint.measureText(corner)
        val start = cx - totalWidth / 2f
        val baseline = cy + fontSize * 0.34f
        var cursor = start
        canvas.drawText(game, cursor + logoPaint.measureText(game) / 2f, baseline, logoPaint)
        cursor += logoPaint.measureText(game) + gap
        drawLogoMark(canvas, cursor + markWidth / 2f, cy, fontSize, alpha)
        cursor += markWidth + gap
        logoPaint.color = Color.argb(alpha, 189, 143, 255)
        canvas.drawText(corner, cursor + logoPaint.measureText(corner) / 2f, baseline, logoPaint)
        logoPaint.clearShadowLayer()
        canvas.restore()
    }

    private fun drawLogoMark(canvas: Canvas, cx: Float, cy: Float, size: Float, alpha: Int) {
        val mark = Path().apply {
            moveTo(cx - size * 0.38f, cy)
            lineTo(cx - size * 0.05f, cy - size * 0.27f)
            lineTo(cx + size * 0.12f, cy - size * 0.27f)
            lineTo(cx - size * 0.08f, cy)
            lineTo(cx + size * 0.12f, cy + size * 0.27f)
            lineTo(cx - size * 0.05f, cy + size * 0.27f)
            close()
        }
        linePaint.color = Color.argb(alpha, 91, 227, 255)
        linePaint.style = Paint.Style.FILL
        canvas.drawPath(mark, linePaint)
        linePaint.style = Paint.Style.STROKE
    }

    private fun drawVortex(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        vortex: Float,
    ) {
        if (vortex <= 0f) return
        val rotation = vortex * 115f
        canvas.save()
        canvas.rotate(rotation, cx, cy)
        linePaint.style = Paint.Style.STROKE
        linePaint.strokeWidth = max(1f, radius * 0.014f)
        for (index in 0 until 8) {
            val inset = radius * (0.18f + index * 0.08f)
            val rectLeft = cx - radius * (1.25f - index * 0.05f)
            val rectTop = cy - radius * (0.78f - index * 0.03f)
            val rectRight = cx + radius * (1.25f - index * 0.05f)
            val rectBottom = cy + radius * (0.78f - index * 0.03f)
            linePaint.color = Color.argb(
                (100f * vortex * (1f - index / 10f)).toInt(),
                if (index % 2 == 0) 91 else 189,
                if (index % 2 == 0) 227 else 143,
                255,
            )
            canvas.drawArc(
                rectLeft + inset,
                rectTop + inset,
                rectRight - inset,
                rectBottom - inset,
                -35f + index * 17f,
                220f,
                false,
                linePaint,
            )
        }
        canvas.restore()

        glowPaint.shader = RadialGradient(
            cx,
            cy,
            radius * (0.2f + vortex * 1.65f),
            Color.TRANSPARENT,
            Color.argb((245f * vortex).toInt(), 1, 2, 7),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, radius * (0.2f + vortex * 1.65f), glowPaint)
    }

    private fun clamp(value: Float): Float = value.coerceIn(0f, 1f)

    private fun easeOut(value: Float): Float = 1f - (1f - value) * (1f - value)

    private fun easeIn(value: Float): Float = value * value
}