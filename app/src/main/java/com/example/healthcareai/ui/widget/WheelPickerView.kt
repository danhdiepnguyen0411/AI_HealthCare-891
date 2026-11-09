package com.example.healthcareai.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.OverScroller
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.example.healthcareai.R
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A smooth, customizable vertical wheel picker view for hours, minutes, or arbitrary items.
 *
 * Supports:
 * - Infinite cyclic looping (wrap-around)
 * - Inertial flinging via [OverScroller] and smooth center-snapping
 * - Dynamic scaling and alpha fading for off-center items matching design
 * - Touch disallow intercept so scrolling does not conflict with parent scroll views
 * - Tap to center on visible off-center items
 * - Subtle haptic clock tick feedback on item transitions
 */
class WheelPickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density

    // Configuration
    var visibleItemCount: Int = 5
        set(value) {
            field = if (value % 2 == 0) value + 1 else value
            requestLayout()
        }

    var isCyclic: Boolean = true

    var selectedTextColor: Int = ContextCompat.getColor(context, R.color.text_primary)
    var normalTextColor: Int = ContextCompat.getColor(context, R.color.color_subtext)

    var selectedTextSize: Float = android.util.TypedValue.applyDimension(
        android.util.TypedValue.COMPLEX_UNIT_SP,
        30f,
        resources.displayMetrics
    )
    var normalTextSize: Float = android.util.TypedValue.applyDimension(
        android.util.TypedValue.COMPLEX_UNIT_SP,
        22f,
        resources.displayMetrics
    )
    var minTextSize: Float = android.util.TypedValue.applyDimension(
        android.util.TypedValue.COMPLEX_UNIT_SP,
        17f,
        resources.displayMetrics
    )

    private val boldTypeface: Typeface? by lazy {
        try {
            ResourcesCompat.getFont(context, R.font.urbanist_bold)
        } catch (_: Exception) {
            Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    private val regularTypeface: Typeface? by lazy {
        try {
            ResourcesCompat.getFont(context, R.font.urbanist_medium)
        } catch (_: Exception) {
            Typeface.DEFAULT
        }
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    // Items and scrolling state
    private val items = mutableListOf<String>()
    private var itemHeight: Float = 0f
    private var scrollYOffset: Int = 0
    private var isDragging: Boolean = false
    private var pendingSelectedPosition: Int? = null
    private var lastNotifiedPosition: Int = -1
    private var lastTickPosition: Int = -1

    private val scroller = OverScroller(context)

    private var onItemSelectedListener: ((position: Int, item: String) -> Unit)? = null

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            isDragging = true
            if (!scroller.isFinished) {
                scroller.abortAnimation()
            }
            return true
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            scrollYOffset += distanceY.toInt()
            if (!isCyclic && itemHeight > 0f) {
                val maxScrollY = ((items.size - 1) * itemHeight).toInt()
                scrollYOffset = scrollYOffset.coerceIn(0, maxScrollY)
            }
            checkHapticTick()
            invalidate()
            return true
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (itemHeight <= 0f || items.isEmpty()) return false
            val minFlingY = if (isCyclic) Int.MIN_VALUE / 2 else 0
            val maxFlingY = if (isCyclic) Int.MAX_VALUE / 2 else ((items.size - 1) * itemHeight).toInt()
            scroller.fling(
                0,
                scrollYOffset,
                0,
                -velocityY.toInt(),
                0,
                0,
                minFlingY,
                maxFlingY
            )
            postInvalidateOnAnimation()
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            if (itemHeight <= 0f || items.isEmpty()) return false
            val clickOffsetFromCenter = e.y - height / 2f
            val steps = (clickOffsetFromCenter / itemHeight).roundToInt()
            if (steps != 0) {
                smoothScrollBy((steps * itemHeight).toInt())
                return true
            }
            return super.onSingleTapUp(e)
        }
    }

    private val gestureDetector = GestureDetector(context, gestureListener)

    fun setItems(newItems: List<String>) {
        items.clear()
        items.addAll(newItems)
        lastNotifiedPosition = -1
        lastTickPosition = -1
        pendingSelectedPosition?.let { pos ->
            pendingSelectedPosition = null
            setSelectedPosition(pos, animated = false)
        } ?: run {
            if (itemHeight > 0f) {
                invalidate()
            }
        }
    }

    fun setSelectedPosition(position: Int, animated: Boolean = false) {
        if (items.isEmpty()) {
            pendingSelectedPosition = position
            return
        }
        val safePos = getNormalizedPosition(position)
        if (itemHeight <= 0f) {
            pendingSelectedPosition = safePos
            return
        }
        val targetScrollY = (safePos * itemHeight).toInt()
        if (animated) {
            val dy = targetScrollY - scrollYOffset
            scroller.startScroll(0, scrollYOffset, 0, dy, 250)
            postInvalidateOnAnimation()
        } else {
            scroller.abortAnimation()
            scrollYOffset = targetScrollY
            lastNotifiedPosition = safePos
            invalidate()
        }
    }

    fun getSelectedPosition(): Int {
        if (items.isEmpty() || itemHeight <= 0f) return 0
        val centerIndex = (scrollYOffset / itemHeight).roundToInt()
        return getNormalizedPosition(centerIndex)
    }

    fun getSelectedItem(): String? {
        val pos = getSelectedPosition()
        return items.getOrNull(pos)
    }

    fun setOnItemSelectedListener(listener: (position: Int, item: String) -> Unit) {
        this.onItemSelectedListener = listener
    }

    fun smoothScrollBy(dy: Int) {
        if (itemHeight <= 0f || items.isEmpty()) return
        val targetScrollY = scrollYOffset + dy
        val clampedTarget = if (isCyclic) {
            targetScrollY
        } else {
            val maxScrollY = ((items.size - 1) * itemHeight).toInt()
            targetScrollY.coerceIn(0, maxScrollY)
        }
        scroller.startScroll(0, scrollYOffset, 0, clampedTarget - scrollYOffset, 250)
        postInvalidateOnAnimation()
    }

    private fun smoothScrollToNearest() {
        if (itemHeight <= 0f || items.isEmpty()) return
        val targetIndex = (scrollYOffset / itemHeight).roundToInt()
        val targetScrollY = (targetIndex * itemHeight).toInt()
        val dy = targetScrollY - scrollYOffset
        if (dy != 0) {
            scroller.startScroll(0, scrollYOffset, 0, dy, 200)
            postInvalidateOnAnimation()
        } else {
            notifySelectedPosition()
        }
    }

    private fun notifySelectedPosition() {
        if (items.isEmpty() || itemHeight <= 0f) return
        val rawIndex = (scrollYOffset / itemHeight).roundToInt()
        val normPos = getNormalizedPosition(rawIndex)

        // Reset scrollYOffset to base modulo when resting to prevent integer overflow over time
        if (isCyclic) {
            scrollYOffset = (normPos * itemHeight).toInt()
        }

        if (normPos != lastNotifiedPosition) {
            lastTickPosition = normPos
            lastNotifiedPosition = normPos
            onItemSelectedListener?.invoke(normPos, items[normPos])
        }
    }

    private fun checkHapticTick() {
        if (itemHeight <= 0f || items.isEmpty()) return
        val currentRawIndex = (scrollYOffset / itemHeight).roundToInt()
        val norm = getNormalizedPosition(currentRawIndex)
        if (norm != lastTickPosition) {
            lastTickPosition = norm
            try {
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            } catch (_: Exception) {}
        }
    }

    private fun getNormalizedPosition(index: Int): Int {
        if (items.isEmpty()) return 0
        val count = items.size
        return ((index % count) + count) % count
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        itemHeight = h / visibleItemCount.toFloat()
        pendingSelectedPosition?.let { pos ->
            pendingSelectedPosition = null
            setSelectedPosition(pos, animated = false)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = (visibleItemCount * 40 * density).toInt()
        val height = resolveSize(desiredHeight, heightMeasureSpec)
        val desiredWidth = (80 * density).toInt()
        val width = resolveSize(desiredWidth, widthMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                isDragging = true
                if (!scroller.isFinished) {
                    scroller.abortAnimation()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                isDragging = true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                isDragging = false
                if (scroller.isFinished) {
                    smoothScrollToNearest()
                }
            }
        }
        return gestureDetector.onTouchEvent(event) || true
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollYOffset = scroller.currY
            checkHapticTick()
            postInvalidateOnAnimation()
        } else if (!isDragging) {
            if (itemHeight > 0f && items.isNotEmpty()) {
                val targetIndex = (scrollYOffset / itemHeight).roundToInt()
                val targetScrollY = (targetIndex * itemHeight).toInt()
                if (scrollYOffset != targetScrollY) {
                    smoothScrollToNearest()
                } else {
                    notifySelectedPosition()
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (items.isEmpty() || itemHeight <= 0f) return

        val centerY = height / 2f
        val centerIndex = (scrollYOffset / itemHeight).roundToInt()

        // Render visible items in range: -3 to +3 around centerIndex
        for (offset in -3..3) {
            val itemIndex = centerIndex + offset
            if (!isCyclic && (itemIndex < 0 || itemIndex >= items.size)) {
                continue
            }

            val normPos = getNormalizedPosition(itemIndex)
            val text = items[normPos]

            val itemCenterY = centerY + (itemIndex * itemHeight - scrollYOffset)
            val distFromCenter = abs(itemCenterY - centerY)

            if (distFromCenter > itemHeight * 2.5f) {
                continue
            }

            // Fraction from 0f (exact center) to 2.5f (at visibility edge)
            val fraction = (distFromCenter / itemHeight).coerceIn(0f, 2.5f)

            // Dynamic text size interpolation
            val textSize = if (fraction <= 1f) {
                selectedTextSize - (selectedTextSize - normalTextSize) * fraction
            } else {
                normalTextSize - (normalTextSize - minTextSize) * ((fraction - 1f) / 1.5f).coerceIn(0f, 1f)
            }

            // Dynamic alpha interpolation
            val alpha = if (fraction <= 1f) {
                (255 - (255 - 110) * fraction).toInt()
            } else {
                (110 - (110 - 25) * ((fraction - 1f) / 1.5f)).toInt()
            }.coerceIn(0, 255)

            textPaint.textSize = textSize
            textPaint.alpha = alpha

            if (fraction < 0.4f) {
                textPaint.color = selectedTextColor
                textPaint.alpha = 255
                textPaint.typeface = boldTypeface
            } else {
                textPaint.color = normalTextColor
                textPaint.alpha = alpha
                textPaint.typeface = regularTypeface
            }

            val fontMetrics = textPaint.fontMetrics
            val baseline = itemCenterY - (fontMetrics.ascent + fontMetrics.descent) / 2f
            canvas.drawText(text, width / 2f, baseline, textPaint)
        }
    }
}
