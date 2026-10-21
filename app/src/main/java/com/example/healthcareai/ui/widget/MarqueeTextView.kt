package com.example.healthcareai.ui.widget

import android.content.Context
import android.graphics.Rect
import android.text.TextUtils
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

/**
 * Custom TextView that enables continuous marquee scrolling
 * when the text length exceeds the view width.
 */
class MarqueeTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    init {
        ellipsize = TextUtils.TruncateAt.MARQUEE
        marqueeRepeatLimit = -1
        isSingleLine = true
        isSelected = true
        isFocusable = false
        isClickable = false
        isLongClickable = false
    }

    override fun isFocused(): Boolean = true

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(true, direction, previouslyFocusedRect)
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        if (hasWindowFocus) {
            super.onWindowFocusChanged(true)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val mode = MeasureSpec.getMode(widthMeasureSpec)
        val size = MeasureSpec.getSize(widthMeasureSpec)
        if (mode == MeasureSpec.AT_MOST && size > 0 && measuredWidth > size) {
            setMeasuredDimension(size, measuredHeight)
        }
    }
}
