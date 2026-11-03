package com.example.healthcareai.ui.camera

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.min

class AiRotatingRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#E5EEFF")
        strokeCap = Paint.Cap.ROUND
    }

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val ringBounds = RectF()
    private var currentRotation = 0f
    private var rotationAnimator: ValueAnimator? = null

    // Sweep angle of the active loading segment (~135 degrees)
    private val sweepAngle = 135f

    private var sweepGradient: SweepGradient? = null
    private val gradientMatrix = Matrix()

    init {
        startAnimation()
    }

    fun startAnimation() {
        if (rotationAnimator?.isRunning == true) return
        rotationAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 1350L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                currentRotation = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun stopAnimation() {
        rotationAnimator?.cancel()
        rotationAnimator = null
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAnimation()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) {
            startAnimation()
        } else {
            stopAnimation()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val strokeWidth = 5.5f * resources.displayMetrics.density
        trackPaint.strokeWidth = strokeWidth
        arcPaint.strokeWidth = strokeWidth

        val halfStroke = strokeWidth / 2f + 1f * resources.displayMetrics.density
        val size = min(w, h).toFloat()
        val left = (w - size) / 2f + halfStroke
        val top = (h - size) / 2f + halfStroke
        val right = (w + size) / 2f - halfStroke
        val bottom = (h + size) / 2f - halfStroke
        ringBounds.set(left, top, right, bottom)

        val cx = w / 2f
        val cy = h / 2f

        // Smooth gradient along the arc: soft blue (#7EA8FF) to vibrant royal blue (#1E5CF7)
        val colors = intArrayOf(
            Color.parseColor("#7EA8FF"),
            Color.parseColor("#1E5CF7"),
            Color.TRANSPARENT,
            Color.TRANSPARENT
        )
        val positions = floatArrayOf(
            0f,
            sweepAngle / 360f,
            (sweepAngle + 1f) / 360f,
            1f
        )
        sweepGradient = SweepGradient(cx, cy, colors, positions)
        arcPaint.shader = sweepGradient
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (ringBounds.isEmpty) return

        val cx = width / 2f
        val cy = height / 2f
        val radius = ringBounds.width() / 2f

        // 1. Draw static full circular track (light blue #E5EEFF)
        canvas.drawCircle(cx, cy, radius, trackPaint)

        // 2. Draw rotating gradient arc with rounded caps
        // Rotate the gradient shader to match the current arc rotation
        sweepGradient?.let { shader ->
            gradientMatrix.reset()
            gradientMatrix.postRotate(currentRotation, cx, cy)
            shader.setLocalMatrix(gradientMatrix)
        }

        canvas.save()
        canvas.rotate(currentRotation, cx, cy)
        canvas.drawArc(ringBounds, 0f, sweepAngle, false, arcPaint)
        canvas.restore()
    }
}

