package com.example.healthcareai.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import kotlin.math.max

/**
 * Custom lightweight FlowLayout that automatically wraps child views to the next line
 * when they exceed the available horizontal width.
 */
class FlowLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private val horizontalGap: Int = (6 * resources.displayMetrics.density).toInt()
    private val verticalGap: Int = (6 * resources.displayMetrics.density).toInt()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val maxAvailableWidth = if (widthMode != MeasureSpec.UNSPECIFIED) {
            widthSize - paddingLeft - paddingRight
        } else {
            Int.MAX_VALUE
        }

        var currentLineWidth = 0
        var currentLineHeight = 0
        var totalHeight = paddingTop + paddingBottom
        var maxLineWidth = 0

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == GONE) continue

            measureChild(child, widthMeasureSpec, heightMeasureSpec)
            val childWidth = child.measuredWidth
            val childHeight = child.measuredHeight

            if (currentLineWidth + childWidth > maxAvailableWidth && currentLineWidth > 0) {
                totalHeight += currentLineHeight + verticalGap
                maxLineWidth = max(maxLineWidth, currentLineWidth - horizontalGap)
                currentLineWidth = childWidth + horizontalGap
                currentLineHeight = childHeight
            } else {
                currentLineWidth += childWidth + horizontalGap
                currentLineHeight = max(currentLineHeight, childHeight)
            }
        }

        totalHeight += currentLineHeight
        maxLineWidth = max(maxLineWidth, currentLineWidth - horizontalGap)

        val finalWidth = resolveSize(maxLineWidth + paddingLeft + paddingRight, widthMeasureSpec)
        val finalHeight = resolveSize(totalHeight, heightMeasureSpec)
        setMeasuredDimension(finalWidth, finalHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val availableWidth = r - l - paddingRight
        var currentX = paddingLeft
        var currentY = paddingTop
        var currentLineHeight = 0

        val lineViews = mutableListOf<View>()

        fun layoutLine(views: List<View>, lineY: Int, lineHeight: Int) {
            var x = paddingLeft
            for (view in views) {
                val top = lineY + (lineHeight - view.measuredHeight) / 2
                view.layout(x, top, x + view.measuredWidth, top + view.measuredHeight)
                x += view.measuredWidth + horizontalGap
            }
        }

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == GONE) continue

            val childWidth = child.measuredWidth
            val childHeight = child.measuredHeight

            if (currentX + childWidth > availableWidth && currentX > paddingLeft) {
                layoutLine(lineViews, currentY, currentLineHeight)
                lineViews.clear()
                currentX = paddingLeft
                currentY += currentLineHeight + verticalGap
                currentLineHeight = childHeight
            } else {
                currentLineHeight = max(currentLineHeight, childHeight)
            }

            lineViews.add(child)
            currentX += childWidth + horizontalGap
        }

        if (lineViews.isNotEmpty()) {
            layoutLine(lineViews, currentY, currentLineHeight)
        }
    }
}
