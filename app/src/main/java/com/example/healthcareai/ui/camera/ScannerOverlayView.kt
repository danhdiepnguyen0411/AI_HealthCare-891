package com.example.healthcareai.ui.camera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * ScannerOverlayView creates a camera scanner mask that dims the surrounding area
 * outside the viewfinder frame while leaving the inner target region transparent.
 */
class ScannerOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val path = Path()
    private val holeRect = RectF()
    private var cornerRadius = 24f * resources.displayMetrics.density

    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#73000000") // 45% black dimming outside viewfinder
        style = Paint.Style.FILL
    }

    fun setHoleRect(left: Float, top: Float, right: Float, bottom: Float) {
        if (holeRect.left == left && holeRect.top == top &&
            holeRect.right == right && holeRect.bottom == bottom
        ) return

        holeRect.set(left, top, right, bottom)
        updatePath()
        invalidate()
    }

    private fun updatePath() {
        path.reset()
        if (width > 0 && height > 0 && !holeRect.isEmpty) {
            path.fillType = Path.FillType.EVEN_ODD
            // Outer viewport (full screen)
            path.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
            // Inner transparent cutout matching the 4 corner brackets
            path.addRoundRect(holeRect, cornerRadius, cornerRadius, Path.Direction.CW)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updatePath()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!path.isEmpty) {
            canvas.drawPath(path, dimPaint)
        }
    }
}
