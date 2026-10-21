package com.example.healthcareai.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.max
import kotlin.math.min

class InteractiveCropImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val currentMatrix = Matrix()
    private val matrixValues = FloatArray(9)

    private var mode = NONE
    private val startPoint = PointF()
    private val midPoint = PointF()

    private var minScale = 1.0f
    private var maxScale = 5.0f

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            val currentScale = getCurrentScale()

            val targetScale = currentScale * scaleFactor
            val actualScale = when {
                targetScale < minScale -> minScale / currentScale
                targetScale > maxScale -> maxScale / currentScale
                else -> scaleFactor
            }

            currentMatrix.postScale(actualScale, actualScale, detector.focusX, detector.focusY)
            clampTranslation()
            imageMatrix = currentMatrix
            return true
        }
    })

    init {
        scaleType = ScaleType.MATRIX
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        fitImageToView()
    }

    override fun setImageURI(uri: Uri?) {
        super.setImageURI(uri)
        fitImageToView()
    }

    override fun setImageBitmap(bm: Bitmap?) {
        super.setImageBitmap(bm)
        fitImageToView()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            fitImageToView()
        }
    }

    fun fitImageToView() {
        val d = drawable ?: return
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        if (viewWidth <= 0f || viewHeight <= 0f) return

        val drawableWidth = d.intrinsicWidth.toFloat()
        val drawableHeight = d.intrinsicHeight.toFloat()
        if (drawableWidth <= 0f || drawableHeight <= 0f) return

        // Scale to fill the entire view (centerCrop)
        val scaleX = viewWidth / drawableWidth
        val scaleY = viewHeight / drawableHeight
        val scale = max(scaleX, scaleY)

        minScale = scale
        maxScale = scale * 4.0f

        currentMatrix.reset()
        currentMatrix.postScale(scale, scale)

        // Center within view
        val dx = (viewWidth - drawableWidth * scale) / 2f
        val dy = (viewHeight - drawableHeight * scale) / 2f
        currentMatrix.postTranslate(dx, dy)

        imageMatrix = currentMatrix
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mode = DRAG
                startPoint.set(event.x, event.y)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                mode = ZOOM
                midPoint.set((event.getX(0) + event.getX(1)) / 2f, (event.getY(0) + event.getY(1)) / 2f)
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == DRAG) {
                    val dx = event.x - startPoint.x
                    val dy = event.y - startPoint.y
                    currentMatrix.postTranslate(dx, dy)
                    clampTranslation()
                    imageMatrix = currentMatrix
                    startPoint.set(event.x, event.y)
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                mode = NONE
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mode = NONE
                clampTranslation()
                imageMatrix = currentMatrix
            }
        }
        return true
    }

    private fun getCurrentScale(): Float {
        currentMatrix.getValues(matrixValues)
        return matrixValues[Matrix.MSCALE_X]
    }

    private fun clampTranslation() {
        val d = drawable ?: return
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        if (viewWidth <= 0f || viewHeight <= 0f) return

        val dRect = RectF(0f, 0f, d.intrinsicWidth.toFloat(), d.intrinsicHeight.toFloat())
        currentMatrix.mapRect(dRect)

        var deltaX = 0f
        var deltaY = 0f

        // Horizontal clamp
        if (dRect.width() >= viewWidth) {
            if (dRect.left > 0f) {
                deltaX = -dRect.left
            } else if (dRect.right < viewWidth) {
                deltaX = viewWidth - dRect.right
            }
        } else {
            deltaX = (viewWidth - dRect.width()) / 2f - dRect.left
        }

        // Vertical clamp
        if (dRect.height() >= viewHeight) {
            if (dRect.top > 0f) {
                deltaY = -dRect.top
            } else if (dRect.bottom < viewHeight) {
                deltaY = viewHeight - dRect.bottom
            }
        } else {
            deltaY = (viewHeight - dRect.height()) / 2f - dRect.top
        }

        currentMatrix.postTranslate(deltaX, deltaY)
    }

    fun cropVisibleBitmap(): Bitmap? {
        val viewWidth = width
        val viewHeight = height
        if (viewWidth <= 0 || viewHeight <= 0) return null

        val bitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        draw(canvas)
        return bitmap
    }

    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
    }
}
