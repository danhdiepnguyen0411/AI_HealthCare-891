package com.example.healthcareai.ui.heartrate.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.res.ResourcesCompat
import com.example.healthcareai.R

/**
 * Custom Bar Chart matching Figma Statistics Overview design:
 * - 0..100 Y-axis labels with dashed horizontal grid lines and upward arrow axis.
 * - 7 rounded capsule bars for Mon..Sun with coral gradient.
 * - Mon..Sun X-axis day labels below each bar.
 * - Smooth entrance animation.
 */
class WeeklyHeartRateChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 7 days (Mon=0, Tue=1, Wed=2, Thu=3, Fri=4, Sat=5, Sun=6)
    // Value is average BPM or null if no measurement on that day.
    private var dayBpms: FloatArray = FloatArray(7) { 0f }
    private var maxScale: Float = 100f
    private var animProgress: Float = 1f
    private val dayLabels = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    private val yTicks = intArrayOf(0, 20, 40, 60, 80, 100)

    private val urbanistTypeface: Typeface? by lazy {
        try {
            ResourcesCompat.getFont(context, R.font.urbanist_medium)
        } catch (_: Exception) {
            Typeface.DEFAULT
        }
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#7E8A9C")
        textSize = dpToPx(11f)
        textAlign = Paint.Align.RIGHT
    }

    private val xLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#56627A")
        textSize = dpToPx(12f)
        textAlign = Paint.Align.CENTER
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E8EDF4")
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1f)
        pathEffect = DashPathEffect(floatArrayOf(dpToPx(4f), dpToPx(3f)), 0f)
    }

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D1D7E2")
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1f)
    }

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val arrowPath = Path()
    private val barRect = RectF()

    init {
        urbanistTypeface?.let {
            textPaint.typeface = it
            xLabelPaint.typeface = it
        }
    }

    fun setData(dailyBpms: List<Int?>, animate: Boolean = true) {
        dayBpms = FloatArray(7)
        var highest = 100f
        for (i in 0 until 7) {
            val v = if (i < dailyBpms.size) dailyBpms[i]?.toFloat() ?: 0f else 0f
            dayBpms[i] = v
            if (v > highest) highest = v
        }
        maxScale = if (highest > 100f) ((highest / 20f).toInt() + 1) * 20f else 100f

        if (animate) {
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 600
                interpolator = DecelerateInterpolator()
                addUpdateListener {
                    animProgress = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        } else {
            animProgress = 1f
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val paddingLeft = paddingLeft.toFloat()
        val paddingTop = paddingTop.toFloat() + dpToPx(12f) // Room for top arrow
        val paddingRight = paddingRight.toFloat()
        val paddingBottom = paddingBottom.toFloat() + dpToPx(24f) // Room for X day labels

        val yLabelWidth = dpToPx(28f)
        val chartLeft = paddingLeft + yLabelWidth + dpToPx(8f)
        val chartRight = w - paddingRight
        val chartTop = paddingTop
        val chartBottom = h - paddingBottom
        val chartHeight = chartBottom - chartTop
        val chartWidth = chartRight - chartLeft

        if (chartHeight <= 0 || chartWidth <= 0) return

        // 1. Draw horizontal dashed lines & Y labels
        for (tick in yTicks) {
            val fraction = tick / maxScale
            val y = chartBottom - (fraction * chartHeight)

            // Y label text (e.g. "100", "80"...)
            canvas.drawText(
                tick.toString(),
                chartLeft - dpToPx(6f),
                y + dpToPx(4f),
                textPaint
            )

            // Horizontal gridline
            canvas.drawLine(chartLeft, y, chartRight, y, gridPaint)
        }

        // 2. Draw vertical Y-axis line with upward arrow
        canvas.drawLine(chartLeft, chartBottom, chartLeft, chartTop - dpToPx(6f), axisPaint)

        arrowPath.reset()
        val arrowTipY = chartTop - dpToPx(10f)
        val arrowTipX = chartLeft
        arrowPath.moveTo(arrowTipX, arrowTipY)
        arrowPath.lineTo(arrowTipX - dpToPx(3.5f), arrowTipY + dpToPx(5f))
        arrowPath.lineTo(arrowTipX + dpToPx(3.5f), arrowTipY + dpToPx(5f))
        arrowPath.close()
        axisPaint.style = Paint.Style.FILL
        canvas.drawPath(arrowPath, axisPaint)
        axisPaint.style = Paint.Style.STROKE

        // 3. Draw 7 columns for Mon..Sun
        val columnWidth = chartWidth / 7f
        val barWidth = dpToPx(18f).coerceAtMost(columnWidth * 0.65f)
        val cornerRadius = barWidth / 2f

        for (i in 0 until 7) {
            val colCenterX = chartLeft + (i + 0.5f) * columnWidth

            // Draw X-axis label
            canvas.drawText(
                dayLabels[i],
                colCenterX,
                h - dpToPx(6f),
                xLabelPaint
            )

            // Draw bar if BPM > 0
            val bpm = dayBpms[i]
            if (bpm > 0f) {
                val barTargetHeight = (bpm / maxScale).coerceIn(0f, 1f) * chartHeight
                val currentBarHeight = barTargetHeight * animProgress
                val barTop = chartBottom - currentBarHeight
                val barLeft = colCenterX - (barWidth / 2f)
                val barRight = colCenterX + (barWidth / 2f)

                barRect.set(barLeft, barTop, barRight, chartBottom)

                barPaint.shader = LinearGradient(
                    colCenterX, barTop, colCenterX, chartBottom,
                    Color.parseColor("#FF767B"),
                    Color.parseColor("#FA5C65"),
                    Shader.TileMode.CLAMP
                )

                canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, barPaint)
            }
        }
    }

    private fun dpToPx(dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }
}
