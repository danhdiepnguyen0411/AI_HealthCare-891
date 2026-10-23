package com.example.healthcareai.ui.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.example.healthcareai.R

class FamilyHubCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.brand_primary_light) // #D6E8FF
    }

    private val path = Path()
    private val rectF = RectF()

    private var selectedIndex = 0

    // Tab horizontal bounds (in pixels)
    private var currentTabLeft = 0f
    private var currentTabRight = 0f
    private var targetTabLeft = 0f
    private var targetTabRight = 0f
    private var hasCalculatedInitialBounds = false

    private var animator: ValueAnimator? = null

    init {
        setWillNotDraw(false)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateTabBounds(selectedIndex, false)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (!hasCalculatedInitialBounds || changed) {
            calculateTabBounds(selectedIndex, false)
        }
    }

    private fun calculateTabBounds(index: Int, animate: Boolean) {
        val density = resources.displayMetrics.density
        val targetLeft: Float
        val targetRight: Float

        val memberThang = findViewById<View>(R.id.layoutMemberThang)
        val memberWoman = findViewById<View>(R.id.layoutMemberWoman)
        val hasSecondMember = memberWoman != null && memberWoman.visibility == View.VISIBLE

        if (index == 0 || !hasSecondMember) {
            targetLeft = 0f
            targetRight = if (memberThang != null && memberThang.width > 0) {
                memberThang.right.toFloat() + 10f * density
            } else {
                86f * density
            }
        } else {
            targetLeft = if (memberWoman.width > 0) {
                memberWoman.left.toFloat() - 10f * density
            } else {
                92f * density
            }
            targetRight = if (memberWoman.width > 0) {
                memberWoman.right.toFloat() + 10f * density
            } else {
                178f * density
            }
        }

        targetTabLeft = targetLeft
        targetTabRight = targetRight

        if (!hasCalculatedInitialBounds || !animate) {
            hasCalculatedInitialBounds = true
            currentTabLeft = targetLeft
            currentTabRight = targetRight
            invalidate()
            return
        }

        animator?.cancel()
        val startLeft = currentTabLeft
        val startRight = currentTabRight

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 320
            interpolator = DecelerateInterpolator(1.8f)
            addUpdateListener { va ->
                val fraction = va.animatedFraction
                currentTabLeft = startLeft + (targetTabLeft - startLeft) * fraction
                currentTabRight = startRight + (targetTabRight - startRight) * fraction
                invalidate()
            }
            start()
        }
    }

    fun selectMember(index: Int, animate: Boolean = true) {
        if (selectedIndex == index && hasCalculatedInitialBounds) return
        selectedIndex = index
        calculateTabBounds(index, animate)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) {
            super.onDraw(canvas)
            return
        }

        val density = resources.displayMetrics.density
        // Large soft bottom corner radius (matching Figma and user's bottom pink circle)
        val bottomRadius = 36f * density
        // Wide, gentle sweeping concave curve (matching Figma and user's top pink circle)
        val memberWoman = findViewById<View>(R.id.layoutMemberWoman)
        val hasSecondMember = memberWoman != null && memberWoman.visibility == View.VISIBLE
        val concaveRadius = if (hasSecondMember) 28f * density else 20f * density
        val shelfTopRadius = 26f * density
        val shelfY = 108f * density

        val tabLeft = currentTabLeft
        val tabRight = currentTabRight
        val tabWidth = (tabRight - tabLeft).coerceAtLeast(40f * density)
        val domeRadius = tabWidth / 2f

        path.reset()

        // 1. Bottom-left corner with large 36dp radius
        rectF.set(0f, h - 2 * bottomRadius, 2 * bottomRadius, h)
        path.arcTo(rectF, 90f, 90f, false)

        // 2. Tab selection shape
        val isFlushLeft = tabLeft <= 4f * density

        if (isFlushLeft) {
            // Member 1 (Thang): flush with left edge
            path.lineTo(0f, domeRadius)

            // Soft continuous top dome over Member 1 (180° to 360°)
            rectF.set(0f, 0f, 2 * domeRadius, 2 * domeRadius)
            path.arcTo(rectF, 180f, 180f, false)

            // Right vertical side down to shelf
            path.lineTo(tabRight, shelfY - concaveRadius)

            // Large 28dp concave inverted curve into shelf
            rectF.set(tabRight, shelfY - 2 * concaveRadius, tabRight + 2 * concaveRadius, shelfY)
            path.arcTo(rectF, 180f, -90f, false)

            // Horizontal shelf to the right
            path.lineTo(w - shelfTopRadius, shelfY)
        } else {
            // Member 2 (Woman) or during animation
            path.lineTo(0f, shelfY + shelfTopRadius)

            // Shelf top-left rounded corner
            val leftCornerRadius = shelfTopRadius.coerceAtMost((tabLeft - concaveRadius) / 2f).coerceAtLeast(0f)
            if (leftCornerRadius > 0f) {
                rectF.set(0f, shelfY, 2 * leftCornerRadius, shelfY + 2 * leftCornerRadius)
                path.arcTo(rectF, 180f, 90f, false)
            } else {
                path.lineTo(0f, shelfY)
            }

            // Shelf horizontal edge up to concave curve start
            val leftConcaveStart = (tabLeft - concaveRadius).coerceAtLeast(leftCornerRadius)
            path.lineTo(leftConcaveStart, shelfY)

            // Large 28dp concave curve up into tabLeft
            rectF.set(tabLeft - 2 * concaveRadius, shelfY - 2 * concaveRadius, tabLeft, shelfY)
            path.arcTo(rectF, 90f, -90f, false)

            // Left vertical side up to dome start
            path.lineTo(tabLeft, domeRadius)

            // Soft continuous top dome over active member
            rectF.set(tabLeft, 0f, tabRight, 2 * domeRadius)
            path.arcTo(rectF, 180f, 180f, false)

            // Right vertical side down to shelf
            path.lineTo(tabRight, shelfY - concaveRadius)

            // Large 28dp concave curve down into shelf
            rectF.set(tabRight, shelfY - 2 * concaveRadius, tabRight + 2 * concaveRadius, shelfY)
            path.arcTo(rectF, 180f, -90f, false)

            // Horizontal shelf to the right
            path.lineTo(w - shelfTopRadius, shelfY)
        }

        // 3. Top-right corner of shelf
        rectF.set(w - 2 * shelfTopRadius, shelfY, w, shelfY + 2 * shelfTopRadius)
        path.arcTo(rectF, 270f, 90f, false)

        // 4. Right edge down
        path.lineTo(w, h - bottomRadius)

        // 5. Bottom-right corner with 36dp radius
        rectF.set(w - 2 * bottomRadius, h - 2 * bottomRadius, w, h)
        path.arcTo(rectF, 0f, 90f, false)

        // 6. Bottom edge back to bottom-left
        path.lineTo(bottomRadius, h)

        path.close()

        canvas.drawPath(path, bgPaint)
        super.onDraw(canvas)
    }
}
