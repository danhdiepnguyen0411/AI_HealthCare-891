package com.example.healthcareai.ui.heartrate.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.CornerPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

/**
 * High-performance real-time photoplethysmogram (PPG) pulse waveform view.
 * Draws smooth cardiac pulse curves with gradient fill under the curve.
 */
class PulseWaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val wavePath = Path()
    private val fillPath = Path()

    // Line Paint
    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF3B5C") // Crimson red
        style = Paint.Style.STROKE
        strokeWidth = 3.5f * resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        pathEffect = CornerPathEffect(14f)
    }

    // Glow / Fill Paint
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // Grid Line Paint
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#15FFFFFF") // 8% white grid
        style = Paint.Style.STROKE
        strokeWidth = 1f * resources.displayMetrics.density
    }

    // Leading Pulse Indicator Dot
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF5C7A")
        style = Paint.Style.FILL
    }
    private val dotGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#50FF3B5C")
        style = Paint.Style.FILL
    }

    private var signalSamples = FloatArray(0)
    private var isMeasuring = false

    fun setMeasuring(measuring: Boolean) {
        this.isMeasuring = measuring
        if (!measuring) {
            signalSamples = FloatArray(0)
        }
        postInvalidateOnAnimation()
    }

    /**
     * Updates the waveform with normalized PPG samples in range [-1.0, 1.0].
     */
    fun updateWaveform(samples: FloatArray) {
        this.signalSamples = samples
        postInvalidateOnAnimation()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (h > 0) {
            fillPaint.shader = LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                intArrayOf(
                    Color.parseColor("#44FF3B5C"),
                    Color.parseColor("#08FF3B5C"),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.6f, 1f),
                Shader.TileMode.CLAMP
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        // 1. Draw subtle background ECG / pulse grid lines
        val centerY = h / 2f
        canvas.drawLine(0f, centerY, w, centerY, gridPaint)
        val quarterH = h / 4f
        canvas.drawLine(0f, centerY - quarterH, w, centerY - quarterH, gridPaint)
        canvas.drawLine(0f, centerY + quarterH, w, centerY + quarterH, gridPaint)

        val stepX = w / 8f
        for (i in 1..7) {
            val gx = i * stepX
            canvas.drawLine(gx, 0f, gx, h, gridPaint)
        }

        // 2. Draw Waveform if we have samples
        val count = signalSamples.size
        if (count < 2) {
            // Draw gentle resting flat baseline
            wavePath.reset()
            wavePath.moveTo(0f, centerY)
            wavePath.lineTo(w, centerY)
            canvas.drawPath(wavePath, wavePaint)
            return
        }

        wavePath.reset()
        fillPath.reset()

        val paddingY = h * 0.15f
        val usableHeight = h - 2 * paddingY
        val dx = w / (count - 1).toFloat()

        var lastX = 0f
        var lastY = centerY

        for (i in 0 until count) {
            val x = i * dx
            // Invert Y because screen Y goes down: signal -1.0 -> bottom, +1.0 -> top
            val norm = signalSamples[i].coerceIn(-1.0f, 1.0f)
            val y = centerY - (norm * (usableHeight / 2f))

            if (i == 0) {
                wavePath.moveTo(x, y)
                fillPath.moveTo(x, h)
                fillPath.lineTo(x, y)
            } else {
                wavePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }

            lastX = x
            lastY = y
        }

        fillPath.lineTo(lastX, h)
        fillPath.close()

        // Draw gradient under curve
        canvas.drawPath(fillPath, fillPaint)

        // Draw pulse line
        canvas.drawPath(wavePath, wavePaint)

        // Draw glowing dot at leading right edge
        if (isMeasuring && count > 0) {
            val dotRadius = 4f * resources.displayMetrics.density
            val glowRadius = 8f * resources.displayMetrics.density
            canvas.drawCircle(lastX, lastY, glowRadius, dotGlowPaint)
            canvas.drawCircle(lastX, lastY, dotRadius, dotPaint)
        }
    }
}
