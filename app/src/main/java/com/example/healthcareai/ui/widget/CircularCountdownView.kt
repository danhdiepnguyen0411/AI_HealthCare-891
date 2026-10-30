package com.example.healthcareai.ui.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.example.healthcareai.R
import kotlin.math.ceil
import kotlin.math.min

class CircularCountdownView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val strokeWidthPx = 3.2f * density

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        color = Color.parseColor("#DDE4EE")
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.primary_color)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = ContextCompat.getColor(context, R.color.text_primary)
        textSize = android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_SP,
            13f,
            resources.displayMetrics
        )
        try {
            val tf = ResourcesCompat.getFont(context, R.font.urbanist_bold)
            typeface = tf ?: Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        } catch (_: Exception) {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    private val arcBounds = RectF()

    private var currentProgress: Float = 1.0f
    private var currentSeconds: Int = 5
    private var countdownAnimator: ValueAnimator? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val defaultSize = (38 * density).toInt()
        val width = resolveSize(defaultSize, widthMeasureSpec)
        val height = resolveSize(defaultSize, heightMeasureSpec)
        val size = min(width, height)
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val radius = (min(width, height) - strokeWidthPx) / 2f

        if (radius <= 0) return

        // 1. Inner white fill
        canvas.drawCircle(cx, cy, radius - strokeWidthPx / 2f, fillPaint)

        // 2. Background track ring
        canvas.drawCircle(cx, cy, radius, trackPaint)

        // 3. Progress arc (clockwise from 12 o'clock)
        if (currentProgress > 0f) {
            arcBounds.set(cx - radius, cy - radius, cx + radius, cy + radius)
            val sweepAngle = 360f * currentProgress
            canvas.drawArc(arcBounds, -90f, sweepAngle, false, progressPaint)
        }

        // 4. Center countdown text (e.g., "5s", "4s", "3s", "2s", "1s")
        val text = "${currentSeconds}s"
        val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(text, cx, textY, textPaint)
    }

    /**
     * Starts countdown for the specified duration (default 5000ms = 5s).
     */
    fun startCountdown(
        totalDurationMs: Long = 5000L,
        onTick: ((remainingSeconds: Int) -> Unit)? = null,
        onFinished: (() -> Unit)? = null
    ) {
        cancelCountdown()

        currentProgress = 1.0f
        currentSeconds = (totalDurationMs / 1000L).toInt().coerceAtLeast(1)
        invalidate()

        countdownAnimator = ValueAnimator.ofFloat(1.0f, 0.0f).apply {
            duration = totalDurationMs
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                val frac = animator.animatedValue as Float
                currentProgress = frac
                val remainingMs = (frac * totalDurationMs).toLong()
                val sec = ceil(remainingMs / 1000f).toInt().coerceAtLeast(1)
                if (sec != currentSeconds) {
                    currentSeconds = sec
                    onTick?.invoke(sec)
                }
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                private var isCancelled = false

                override fun onAnimationCancel(animation: Animator) {
                    isCancelled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (!isCancelled) {
                        currentProgress = 0f
                        invalidate()
                        onFinished?.invoke()
                    }
                }
            })
            start()
        }
    }

    fun pauseCountdown() {
        countdownAnimator?.pause()
    }

    fun resumeCountdown() {
        countdownAnimator?.resume()
    }

    fun cancelCountdown() {
        countdownAnimator?.cancel()
        countdownAnimator = null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelCountdown()
    }
}
