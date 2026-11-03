package com.example.healthcareai.ui.camera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.healthcareai.R
import com.example.healthcareai.data.model.DetectionBox

class DetectionBoxOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F67FE")
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * resources.displayMetrics.density
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#150F67FE")
        style = Paint.Style.FILL
    }

    private val cornerRadius = 8f * resources.displayMetrics.density
    private val badgeDrawable = ContextCompat.getDrawable(context, R.drawable.ic_detection_box)
    private val badgeSize = (22 * resources.displayMetrics.density).toInt()

    private var boxRect: RectF? = null

    fun setDetectionBox(box: DetectionBox?, imgWidth: Int, imgHeight: Int) {
        if (box == null || !box.isValid()) {
            boxRect = null
            invalidate()
            return
        }

        val vw = width.toFloat()
        val vh = height.toFloat()
        if (vw <= 0 || vh <= 0) {
            post { setDetectionBox(box, imgWidth, imgHeight) }
            return
        }

        // Determine if coordinates are normalized (0..1) or pixel based
        val (left, top, w, h) = if (box.width <= 1.0f && box.height <= 1.0f) {
            listOf(box.x * vw, box.y * vh, box.width * vw, box.height * vh)
        } else if (imgWidth > 0 && imgHeight > 0) {
            val scaleX = vw / imgWidth
            val scaleY = vh / imgHeight
            val scale = minOf(scaleX, scaleY)
            val offsetX = (vw - imgWidth * scale) / 2f
            val offsetY = (vh - imgHeight * scale) / 2f
            listOf(
                offsetX + box.x * scale,
                offsetY + box.y * scale,
                box.width * scale,
                box.height * scale
            )
        } else {
            listOf(box.x, box.y, box.width, box.height)
        }

        boxRect = RectF(left, top, left + w, top + h)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val rect = boxRect ?: return

        // 1. Draw subtle transparent fill
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, fillPaint)

        // 2. Draw border
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, boxPaint)

        // 3. Draw badge icon at top-right corner of the bounding box
        badgeDrawable?.let { drawable ->
            val halfBadge = badgeSize / 2
            val badgeX = rect.right.toInt() - halfBadge
            val badgeY = rect.top.toInt() - halfBadge
            drawable.setBounds(badgeX, badgeY, badgeX + badgeSize, badgeY + badgeSize)
            drawable.draw(canvas)
        }
    }
}
