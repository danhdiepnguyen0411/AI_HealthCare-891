package com.example.healthcareai.ui.heartrate.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.CornerPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

/**
 * Custom View that draws an authentic ECG / Photoplethysmogram (PPG) pulse wave line
 * ("mạch máu chạy qua trái tim") that spans horizontally across the screen and animates
 * continuously to simulate blood flow pulsing through the heart.
 */
class EcgPulseLineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF5B6E") // Vibrant blood vessel coral red
        style = Paint.Style.STROKE
        strokeWidth = 3.6f * resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        pathEffect = CornerPathEffect(14f)
    }

    private val wavePath = Path()
    private var phaseOffset = 0f
    private var animator: ValueAnimator? = null
    private var isPulsing = false

    // Standard ECG pulse pattern (dx fraction of cycle, dy fraction of amplitude)
    // Positive dy is upward peak, negative is downward dip
    private val ecgTemplate = listOf(
        Pair(0.00f, 0.00f),
        Pair(0.12f, 0.00f),
        Pair(0.18f, 0.12f),  // P wave
        Pair(0.24f, 0.00f),
        Pair(0.28f, -0.15f), // Q dip
        Pair(0.35f, 0.85f),  // R peak (tall)
        Pair(0.42f, -0.45f), // S dip
        Pair(0.48f, 0.00f),
        Pair(0.56f, 0.28f),  // T wave
        Pair(0.66f, 0.00f),
        Pair(1.00f, 0.00f)   // Isoelectric resting line
    )

    init {
        startFlowAnimation()
    }

    fun startFlowAnimation() {
        if (animator?.isRunning == true) return

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2400L // smooth heartbeat cycle duration
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { va ->
                phaseOffset = va.animatedValue as Float
                postInvalidateOnAnimation()
            }
            start()
        }
        isPulsing = true
    }

    fun stopFlowAnimation() {
        animator?.cancel()
        animator = null
        isPulsing = false
        postInvalidateOnAnimation()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isPulsing && animator?.isRunning != true) {
            startFlowAnimation()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopFlowAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val centerY = h / 2f
        val amplitude = h * 0.42f // Peak excursion amplitude

        // Wavelength: about 180dp so 2 to 3 heartbeats fit across typical screen
        val wavelength = 180f * resources.displayMetrics.density

        wavePath.reset()

        // Offset in pixels caused by phase animation (moves from right to left)
        val pixelOffset = (phaseOffset * wavelength)

        var x = -wavelength + (wavelength - (pixelOffset % wavelength))
        var isFirst = true

        while (x < w + wavelength) {
            val startX = x
            for (pt in ecgTemplate) {
                val px = startX + pt.first * wavelength
                val py = centerY - (pt.second * amplitude)

                if (isFirst) {
                    wavePath.moveTo(px, py)
                    isFirst = false
                } else {
                    wavePath.lineTo(px, py)
                }
            }
            x += wavelength
        }

        canvas.drawPath(wavePath, linePaint)
    }
}
