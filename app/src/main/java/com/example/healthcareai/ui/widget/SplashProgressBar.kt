package com.example.healthcareai.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

/**
 * Custom progress bar matching Figma node 1:418.
 * Features:
 * - Rounded track (height 10dp, corner radius 3dp, color rgba(255,255,255,0.32))
 * - Rounded active bar (height 10dp, corner radius 3dp, color #FFFFFF)
 * - Outer glow / aura spread around the active bar (spread 4dp, corner radius 7dp, color rgba(255,255,255,0.25))
 */
class SplashProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val haloSize = 4f * density
    private val trackHeight = 10f * density
    private val trackCornerRadius = 3f * density

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(82, 255, 255, 255) // rgba(255, 255, 255, 0.32)
    }

    private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(64, 255, 255, 255) // rgba(255, 255, 255, 0.25)
    }

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val trackRect = RectF()
    private val haloRect = RectF()
    private val barRect = RectF()

    var max: Int = 100
        set(value) {
            if (field != value && value > 0) {
                field = value
                postInvalidateOnAnimation()
            }
        }

    var progress: Int = 0
        set(value) {
            val clamped = value.coerceIn(0, max)
            if (field != clamped) {
                field = clamped
                postInvalidateOnAnimation()
            }
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = (trackHeight + 2f * haloSize + paddingTop + paddingBottom).toInt()
        val measuredHeight = resolveSize(desiredHeight, heightMeasureSpec)
        val measuredWidth = resolveSize((311f * density + 2f * haloSize).toInt(), widthMeasureSpec)
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val availableWidth = width - paddingLeft - paddingRight
        if (availableWidth <= 0) return

        val cy = paddingTop + (height - paddingTop - paddingBottom) / 2f
        val trackTop = cy - trackHeight / 2f
        val trackBottom = cy + trackHeight / 2f

        // Leave room for the 4dp halo on left and right edges
        val trackLeft = paddingLeft + haloSize
        val trackRight = width - paddingRight - haloSize

        if (trackRight <= trackLeft) return

        // 1. Draw background track (rgba(255, 255, 255, 0.32))
        trackRect.set(trackLeft, trackTop, trackRight, trackBottom)
        canvas.drawRoundRect(trackRect, trackCornerRadius, trackCornerRadius, trackPaint)

        // 2. Draw active progress with halo
        if (progress > 0) {
            val totalTrackWidth = trackRight - trackLeft
            val progressFraction = progress / max.toFloat()
            val rawBarWidth = totalTrackWidth * progressFraction

            val barRight = (trackLeft + rawBarWidth).coerceAtMost(trackRight)
            val currentBarWidth = barRight - trackLeft

            if (currentBarWidth > 0f) {
                val barRadius = min(trackCornerRadius, currentBarWidth / 2f)

                // Halo: spreads haloSize (4dp) outside the active bar
                val haloLeft = trackLeft - haloSize
                val haloTop = trackTop - haloSize
                val haloRight = barRight + haloSize
                val haloBottom = trackBottom + haloSize
                val currentHaloRadius = barRadius + haloSize

                haloRect.set(haloLeft, haloTop, haloRight, haloBottom)
                canvas.drawRoundRect(haloRect, currentHaloRadius, currentHaloRadius, haloPaint)

                // Active bar: pure white
                barRect.set(trackLeft, trackTop, barRight, trackBottom)
                canvas.drawRoundRect(barRect, barRadius, barRadius, barPaint)
            }
        }
    }
}

