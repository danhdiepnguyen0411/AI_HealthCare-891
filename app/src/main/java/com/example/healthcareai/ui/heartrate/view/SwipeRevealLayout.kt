package com.example.healthcareai.ui.heartrate.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Outline
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.customview.widget.ViewDragHelper
import kotlin.math.abs

/**
 * Production-ready SwipeRevealLayout implementing Figma's swipe-to-reveal delete pattern:
 * - 20dp rounded capsule outline clipping ensuring all corners stay perfectly rounded.
 * - Solid red background container (#FF4D4F, 20dp radius) running underneath the entire row.
 * - Foreground card (#FFFFFF, 20dp radius, elevation 2dp) smoothly slides left up to 90dp.
 * - Beautiful seamless corners: the foreground card's right corners curve over the red background
 *   with NO sharp 90-degree corners or gray gaps sticking out!
 * - Perfectly coordinates with RecyclerView vertical scrolling.
 * - Closes automatically when another item opens.
 */
class SwipeRevealLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private var secondaryView: View? = null
    private var mainView: View? = null

    private var dragHelper: ViewDragHelper
    private var initialMotionX = 0f
    private var initialMotionY = 0f
    private var isDragging = false

    private var deleteCardBg: Drawable? = null

    var onSwipeListener: OnSwipeListener? = null

    interface OnSwipeListener {
        fun onOpened(layout: SwipeRevealLayout)
        fun onClosed(layout: SwipeRevealLayout)
    }

    init {
        dragHelper = ViewDragHelper.create(this, 1.0f, DragHelperCallback())

        // Ensure the entire row is clipped to Figma's 20dp rounded rectangle outline
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val radius = 20f * context.resources.displayMetrics.density
                outline.setRoundRect(0, 0, view.width, view.height, radius)
            }
        }
        clipToOutline = true
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        if (childCount >= 2) {
            secondaryView = getChildAt(0)
            mainView = getChildAt(1)
        }
        deleteCardBg = background
        updateBackgroundState()
    }

    private fun updateBackgroundState() {
        val offset = mainView?.left ?: 0
        if (offset == 0) {
            // When closed, hide red background completely to avoid any subpixel antialiasing bleed
            if (background != null) {
                background = null
            }
        } else {
            // When swiping or open, show the solid red 20dp background behind the card
            if (background == null && deleteCardBg != null) {
                background = deleteCardBg
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)

        // 1. Measure mainView first to determine true height of the card
        var cardHeight = 0
        mainView?.let { main ->
            measureChildWithMargins(main, widthMeasureSpec, 0, heightMeasureSpec, 0)
            val lp = main.layoutParams as MarginLayoutParams
            cardHeight = main.measuredHeight + lp.topMargin + lp.bottomMargin
        }

        // 2. Measure secondaryView with exact card height
        secondaryView?.let { sec ->
            val lp = sec.layoutParams as MarginLayoutParams
            val secHeight = (cardHeight - lp.topMargin - lp.bottomMargin).coerceAtLeast(0)
            val secHeightSpec = MeasureSpec.makeMeasureSpec(secHeight, MeasureSpec.EXACTLY)
            measureChildWithMargins(sec, widthMeasureSpec, 0, secHeightSpec, 0)
        }

        setMeasuredDimension(
            resolveSize(width, widthMeasureSpec),
            resolveSize(cardHeight, heightMeasureSpec)
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val parentRight = r - l - paddingRight
        val parentTop = paddingTop

        // Layout secondaryView pinned to the right
        secondaryView?.let { sec ->
            val lp = sec.layoutParams as MarginLayoutParams
            val secW = sec.measuredWidth
            val secH = sec.measuredHeight
            val secRight = parentRight - lp.rightMargin
            val secLeft = secRight - secW
            val secTop = parentTop + lp.topMargin
            val secBottom = secTop + secH
            sec.layout(secLeft, secTop, secRight, secBottom)
        }

        // Layout mainView in front (preserving horizontal offset)
        mainView?.let { main ->
            val lp = main.layoutParams as MarginLayoutParams
            val mainW = main.measuredWidth
            val mainH = main.measuredHeight
            val currentOffset = main.left
            val mainTop = parentTop + lp.topMargin
            val mainBottom = mainTop + mainH
            main.layout(currentOffset, mainTop, currentOffset + mainW, mainBottom)
        }
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return MarginLayoutParams(context, attrs)
    }

    override fun generateLayoutParams(p: LayoutParams?): LayoutParams {
        return MarginLayoutParams(p)
    }

    override fun checkLayoutParams(p: LayoutParams?): Boolean {
        return p is MarginLayoutParams
    }

    override fun computeScroll() {
        if (dragHelper.continueSettling(true)) {
            updateBackgroundState()
            postInvalidateOnAnimation()
        } else {
            updateBackgroundState()
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialMotionX = ev.x
                initialMotionY = ev.y
                isDragging = false
                dragHelper.processTouchEvent(ev)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = abs(ev.x - initialMotionX)
                val dy = abs(ev.y - initialMotionY)
                val slop = dragHelper.touchSlop

                if (dx > slop && dx > dy) {
                    isDragging = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                isDragging = false
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return dragHelper.shouldInterceptTouchEvent(ev) || isDragging
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        dragHelper.processTouchEvent(ev)

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialMotionX = ev.x
                initialMotionY = ev.y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = abs(ev.x - initialMotionX)
                val dy = abs(ev.y - initialMotionY)
                if (dx > dragHelper.touchSlop && dx > dy) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }

    val isOpen: Boolean
        get() {
            val secW = secondaryView?.measuredWidth ?: return false
            return (mainView?.left ?: 0) <= -secW / 2
        }

    fun open(animate: Boolean = true) {
        val secW = secondaryView?.measuredWidth ?: 0
        if (secW <= 0) return
        if (background == null && deleteCardBg != null) {
            background = deleteCardBg
        }
        if (animate) {
            mainView?.let {
                if (dragHelper.smoothSlideViewTo(it, -secW, it.top)) {
                    postInvalidateOnAnimation()
                }
            }
        } else {
            mainView?.offsetLeftAndRight(-secW - (mainView?.left ?: 0))
            requestLayout()
        }
        onSwipeListener?.onOpened(this)
    }

    fun close(animate: Boolean = true) {
        if (animate) {
            mainView?.let {
                if (dragHelper.smoothSlideViewTo(it, 0, it.top)) {
                    postInvalidateOnAnimation()
                }
            }
        } else {
            mainView?.let {
                val currentLeft = it.left
                if (currentLeft != 0) {
                    it.offsetLeftAndRight(-currentLeft)
                    requestLayout()
                }
            }
            updateBackgroundState()
        }
        onSwipeListener?.onClosed(this)
    }

    private inner class DragHelperCallback : ViewDragHelper.Callback() {

        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            return child == mainView
        }

        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
            val secW = secondaryView?.measuredWidth ?: 0
            return left.coerceIn(-secW, 0)
        }

        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
            return child.top
        }

        override fun getViewHorizontalDragRange(child: View): Int {
            return if (child == mainView) secondaryView?.measuredWidth ?: 0 else 0
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            val secW = secondaryView?.measuredWidth ?: 0
            val targetLeft = if (xvel < -500f || (xvel <= 0f && releasedChild.left < -secW / 2)) {
                -secW
            } else {
                0
            }

            if (dragHelper.settleCapturedViewAt(targetLeft, releasedChild.top)) {
                this@SwipeRevealLayout.postInvalidateOnAnimation()
            }

            if (targetLeft == 0) {
                onSwipeListener?.onClosed(this@SwipeRevealLayout)
            } else {
                onSwipeListener?.onOpened(this@SwipeRevealLayout)
            }
        }

        override fun onViewPositionChanged(changedView: View, left: Int, top: Int, dx: Int, dy: Int) {
            super.onViewPositionChanged(changedView, left, top, dx, dy)
            updateBackgroundState()
            invalidate()
        }
    }
}
