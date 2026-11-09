package com.example.healthcareai.ui.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
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
    private var animStartLeft = 0f
    private var animStartRight = 0f
    private var hasCalculatedInitialBounds = false

    private var animator: ValueAnimator? = null

    private var cachedAvatarsRow: ViewGroup? = null
    private var cachedScrollContainer: HorizontalScrollView? = null
    private var cachedAddBtn: android.view.View? = null

    private fun getAvatarsRow(): ViewGroup? {
        if (cachedAvatarsRow == null) {
            cachedAvatarsRow = findViewById(R.id.layoutFamilyAvatarsRow)
        }
        return cachedAvatarsRow
    }

    private fun getScrollContainer(): HorizontalScrollView? {
        if (cachedScrollContainer == null) {
            cachedScrollContainer = findViewById(R.id.scrollFamilyAvatars)
        }
        return cachedScrollContainer
    }

    private fun getAddBtn(): android.view.View? {
        if (cachedAddBtn == null) {
            cachedAddBtn = findViewById(R.id.layoutMemberAdd)
        }
        return cachedAddBtn
    }

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
        val avatarsRow = getAvatarsRow()
        val scrollContainer = getScrollContainer()

        if (index < 0) {
            targetTabLeft = 0f
            targetTabRight = 0f
            currentTabLeft = 0f
            currentTabRight = 0f
            hasCalculatedInitialBounds = true
            invalidate()
            return
        }

        val child = avatarsRow?.getChildAt(index)
        val scrollX = scrollContainer?.scrollX ?: 0

        val targetLeft: Float
        val targetRight: Float
        val halfWidth = 44f * density

        if (child != null && child.width > 0) {
            val childCenter = (child.left + child.right) / 2f - scrollX
            targetLeft = childCenter - halfWidth
            targetRight = childCenter + halfWidth
        } else {
            // Fallback before initial child layout measurement
            val step = 88f * density // 64dp avatar + 24dp margin
            val start = 12f * density + index * step
            val childCenter = start + 32f * density
            targetLeft = childCenter - halfWidth
            targetRight = childCenter + halfWidth
        }

        targetTabLeft = targetLeft
        targetTabRight = targetRight

        if (!hasCalculatedInitialBounds || !animate) {
            hasCalculatedInitialBounds = true
            animator?.cancel()
            currentTabLeft = targetLeft
            currentTabRight = targetRight
            invalidate()
            return
        }

        animator?.cancel()
        animStartLeft = currentTabLeft
        animStartRight = currentTabRight

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 280
            interpolator = DecelerateInterpolator(1.8f)
            addUpdateListener { va ->
                val fraction = va.animatedFraction
                currentTabLeft = animStartLeft + (targetTabLeft - animStartLeft) * fraction
                currentTabRight = animStartRight + (targetTabRight - animStartRight) * fraction
                invalidate()
            }
            start()
        }
    }

    fun selectMember(index: Int, animate: Boolean = true, force: Boolean = false) {
        if (!force && selectedIndex == index && hasCalculatedInitialBounds) return
        selectedIndex = index
        calculateTabBounds(index, animate)
    }

    fun updateTabBoundsForScroll() {
        val density = resources.displayMetrics.density
        val avatarsRow = getAvatarsRow() ?: return
        val scrollContainer = getScrollContainer() ?: return
        if (selectedIndex < 0) return
        val child = avatarsRow.getChildAt(selectedIndex) ?: return
        if (child.width <= 0) return

        val scrollX = scrollContainer.scrollX
        val childCenter = (child.left + child.right) / 2f - scrollX
        val halfWidth = 44f * density
        val newTargetLeft = childCenter - halfWidth
        val newTargetRight = childCenter + halfWidth

        if (animator?.isRunning == true) {
            val delta = newTargetLeft - targetTabLeft
            targetTabLeft = newTargetLeft
            targetTabRight = newTargetRight
            animStartLeft += delta
            animStartRight += delta
            currentTabLeft += delta
            currentTabRight += delta
        } else {
            currentTabLeft = newTargetLeft
            currentTabRight = newTargetRight
            targetTabLeft = newTargetLeft
            targetTabRight = newTargetRight
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) {
            super.onDraw(canvas)
            return
        }

        val density = resources.displayMetrics.density
        val bottomRadius = 36f * density
        val avatarsRow = getAvatarsRow()
        val addBtn = getAddBtn()
        val addBtnIndex = avatarsRow?.indexOfChild(addBtn) ?: -1
        val memberCount = if (addBtnIndex >= 0) addBtnIndex else ((avatarsRow?.childCount ?: 1) - 1).coerceAtLeast(0)

        path.reset()

        if (memberCount == 0 || selectedIndex < 0) {
            // No family members yet: standard rounded rectangle card
            val topRadius = 26f * density
            rectF.set(0f, h - 2 * bottomRadius, 2 * bottomRadius, h)
            path.arcTo(rectF, 90f, 90f, false)

            path.lineTo(0f, topRadius)
            rectF.set(0f, 0f, 2 * topRadius, 2 * topRadius)
            path.arcTo(rectF, 180f, 90f, false)

            path.lineTo(w - topRadius, 0f)
            rectF.set(w - 2 * topRadius, 0f, w, 2 * topRadius)
            path.arcTo(rectF, 270f, 90f, false)

            path.lineTo(w, h - bottomRadius)
            rectF.set(w - 2 * bottomRadius, h - 2 * bottomRadius, w, h)
            path.arcTo(rectF, 0f, 90f, false)

            path.lineTo(bottomRadius, h)
            path.close()

            val saveCount = canvas.save()
            canvas.clipRect(0f, 0f, w, h)
            canvas.drawPath(path, bgPaint)
            canvas.restoreToCount(saveCount)
            super.onDraw(canvas)
            return
        }

        val concaveRadius = 24f * density
        val shelfTopRadius = 26f * density
        val shelfY = 108f * density

        val tabLeft = currentTabLeft
        val tabRight = currentTabRight
        val effectiveTabLeft = if (tabLeft in -2f * density..2f * density) 0f else tabLeft
        val tabWidth = (tabRight - effectiveTabLeft).coerceAtLeast(40f * density)
        val domeRadius = tabWidth / 2f

        // 1. Bottom-left corner: (bottomRadius, h) -> (0, h - bottomRadius)
        rectF.set(0f, h - 2 * bottomRadius, 2 * bottomRadius, h)
        path.arcTo(rectF, 90f, 90f, false)

        if (tabRight <= 0f || effectiveTabLeft >= w) {
            // Selected tab completely scrolled off-screen: clean horizontal shelf across card
            path.lineTo(0f, shelfY + shelfTopRadius)
            rectF.set(0f, shelfY, 2 * shelfTopRadius, shelfY + 2 * shelfTopRadius)
            path.arcTo(rectF, 180f, 90f, false)

            path.lineTo(w - shelfTopRadius, shelfY)
            rectF.set(w - 2 * shelfTopRadius, shelfY, w, shelfY + 2 * shelfTopRadius)
            path.arcTo(rectF, 270f, 90f, false)

            path.lineTo(w, h - bottomRadius)
        } else {
            // 2. Left side & shelf up to effectiveTabLeft
            if (effectiveTabLeft <= 0f) {
                path.lineTo(0f, domeRadius)
            } else {
                val availLeft = effectiveTabLeft
                val cornerR = shelfTopRadius.coerceAtMost(availLeft / 2f).coerceAtLeast(0f)
                val concaveR = concaveRadius.coerceAtMost(availLeft - cornerR).coerceAtLeast(0f)

                path.lineTo(0f, shelfY + cornerR)
                if (cornerR > 0f) {
                    rectF.set(0f, shelfY, 2 * cornerR, shelfY + 2 * cornerR)
                    path.arcTo(rectF, 180f, 90f, false)
                } else {
                    path.lineTo(0f, shelfY)
                }

                path.lineTo(effectiveTabLeft - concaveR, shelfY)
                if (concaveR > 0f) {
                    rectF.set(effectiveTabLeft - 2 * concaveR, shelfY - 2 * concaveR, effectiveTabLeft, shelfY)
                    path.arcTo(rectF, 90f, -90f, false)
                } else {
                    path.lineTo(effectiveTabLeft, shelfY)
                }

                path.lineTo(effectiveTabLeft, domeRadius)
            }

            // 3. Top dome over active member: (effectiveTabLeft, domeRadius) -> (tabRight, domeRadius)
            rectF.set(effectiveTabLeft, 0f, tabRight, 2 * domeRadius)
            path.arcTo(rectF, 180f, 180f, false)

            // 4. Right side down from tabRight to shelf & right edge
            if (tabRight >= w) {
                path.lineTo(w, domeRadius)
                path.lineTo(w, h - bottomRadius)
            } else {
                val availRight = w - tabRight
                val cornerR = shelfTopRadius.coerceAtMost(availRight / 2f).coerceAtLeast(0f)
                val concaveR = concaveRadius.coerceAtMost(availRight - cornerR).coerceAtLeast(0f)

                path.lineTo(tabRight, shelfY - concaveR)
                if (concaveR > 0f) {
                    rectF.set(tabRight, shelfY - 2 * concaveR, tabRight + 2 * concaveR, shelfY)
                    path.arcTo(rectF, 180f, -90f, false)
                } else {
                    path.lineTo(tabRight, shelfY)
                }

                path.lineTo(w - cornerR, shelfY)
                if (cornerR > 0f) {
                    rectF.set(w - 2 * cornerR, shelfY, w, shelfY + 2 * cornerR)
                    path.arcTo(rectF, 270f, 90f, false)
                } else {
                    path.lineTo(w, shelfY)
                }

                path.lineTo(w, h - bottomRadius)
            }
        }

        // 5. Bottom-right corner: (w, h - bottomRadius) -> (w - bottomRadius, h)
        rectF.set(w - 2 * bottomRadius, h - 2 * bottomRadius, w, h)
        path.arcTo(rectF, 0f, 90f, false)

        // 6. Bottom edge back to bottom-left: (w - bottomRadius, h) -> (bottomRadius, h)
        path.lineTo(bottomRadius, h)

        path.close()

        val saveCount = canvas.save()
        canvas.clipRect(0f, 0f, w, h)
        canvas.drawPath(path, bgPaint)
        canvas.restoreToCount(saveCount)

        super.onDraw(canvas)
    }
}
