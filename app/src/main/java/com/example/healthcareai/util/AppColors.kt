package com.example.healthcareai.util

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.example.healthcareai.R

/**
 * AppColors - Centralized Design System Color Tokens & Utilities.
 *
 * Mapped 1:1 with Figma Design specifications:
 * - Primary Color:       #0F67FE
 * - Color text:          #242E49
 * - Text tertiary:       #BEC5D2
 * - Blue text:           #0843C6
 * - Backgr:              #F2F5F9
 * - Card color:          Gradient 0% #0843C6 -> 100% #335EDF
 * - Darker blue:         #4E75F7
 * - Gradient red:        Gradient 0% #FF8082 -> 100% #F64A4D
 * - Secondary color btn: #D6E8FF
 */
object AppColors {

    // =========================================================================
    // 1. Direct @ColorInt Constants (Fast, context-free, zero runtime overhead)
    // =========================================================================
    @ColorInt const val PRIMARY: Int = 0xFF0F67FE.toInt()
    @ColorInt const val COLOR_TEXT: Int = 0xFF242E49.toInt()
    @ColorInt const val TEXT_TERTIARY: Int = 0xFFBEC5D2.toInt()
    @ColorInt const val BLUE_TEXT: Int = 0xFF0843C6.toInt()
    @ColorInt const val BACKGR: Int = 0xFFF2F5F9.toInt()
    @ColorInt const val DARKER_BLUE: Int = 0xFF4E75F7.toInt()
    @ColorInt const val SECONDARY_COLOR_BTN: Int = 0xFFD6E8FF.toInt()

    // Gradient stops
    @ColorInt const val CARD_GRADIENT_START: Int = 0xFF0843C6.toInt()
    @ColorInt const val CARD_GRADIENT_END: Int = 0xFF335EDF.toInt()

    @ColorInt const val GRADIENT_RED_START: Int = 0xFFFF8082.toInt()
    @ColorInt const val GRADIENT_RED_END: Int = 0xFFF64A4D.toInt()

    // AI Scan tokens
    @ColorInt const val SCAN_CIRCLE_SKIN: Int = 0xFFFEF1E6.toInt()
    @ColorInt const val SCAN_CIRCLE_MOUTH: Int = 0xFFFEE6E6.toInt()
    @ColorInt const val SCAN_CIRCLE_EYE: Int = 0xFFE4F0FC.toInt()
    @ColorInt const val SCAN_CIRCLE_NAILS: Int = 0xFFEEE6FC.toInt()
    @ColorInt const val SCAN_CIRCLE_OTHER: Int = 0xFFDDF9E9.toInt()
    @ColorInt const val SCAN_CARD_SELECTED_BG: Int = 0xFFE8F1FF.toInt()
    @ColorInt const val SCAN_CARD_SELECTED_STROKE: Int = 0xFF0F67FE.toInt()
    @ColorInt const val SCAN_CARD_UNSELECTED_BG: Int = 0xFFFFFFFF.toInt()

    // Common system tones
    @ColorInt const val WHITE: Int = 0xFFFFFFFF.toInt()
    @ColorInt const val BLACK: Int = 0xFF000000.toInt()
    @ColorInt const val TRANSPARENT: Int = 0x00000000

    // =========================================================================
    // 2. Resource IDs (For XML & Framework bindings)
    // =========================================================================
    object Res {
        @ColorRes val PRIMARY = R.color.primary_color
        @ColorRes val COLOR_TEXT = R.color.color_text
        @ColorRes val TEXT_TERTIARY = R.color.text_tertiary
        @ColorRes val BLUE_TEXT = R.color.blue_text
        @ColorRes val BACKGR = R.color.backgr
        @ColorRes val DARKER_BLUE = R.color.darker_blue
        @ColorRes val SECONDARY_COLOR_BTN = R.color.secondary_color_btn
        @ColorRes val CARD_GRADIENT_START = R.color.card_gradient_start
        @ColorRes val CARD_GRADIENT_END = R.color.card_gradient_end
        @ColorRes val GRADIENT_RED_START = R.color.gradient_red_start
        @ColorRes val GRADIENT_RED_END = R.color.gradient_red_end

        @ColorRes val SCAN_CIRCLE_SKIN = R.color.scan_circle_skin
        @ColorRes val SCAN_CIRCLE_MOUTH = R.color.scan_circle_mouth
        @ColorRes val SCAN_CIRCLE_EYE = R.color.scan_circle_eye
        @ColorRes val SCAN_CIRCLE_NAILS = R.color.scan_circle_nails
        @ColorRes val SCAN_CIRCLE_OTHER = R.color.scan_circle_other
        @ColorRes val SCAN_CARD_SELECTED_BG = R.color.scan_card_selected_bg
        @ColorRes val SCAN_CARD_SELECTED_STROKE = R.color.scan_card_selected_stroke
        @ColorRes val SCAN_CARD_UNSELECTED_BG = R.color.scan_card_unselected_bg
    }

    // =========================================================================
    // 3. Programmatic Gradient Builders
    // =========================================================================

    /**
     * Creates a GradientDrawable for "Card color" (0% #0843C6 -> 100% #335EDF).
     *
     * @param orientation Gradient direction, defaults to LEFT_RIGHT.
     * @param cornerRadiusDp Corner radius in dp (automatically converted to px if context provided).
     * @param context Optional context to resolve dp into px.
     */
    fun createCardGradient(
        orientation: GradientDrawable.Orientation = GradientDrawable.Orientation.LEFT_RIGHT,
        cornerRadiusDp: Float = 16f,
        context: Context? = null
    ): GradientDrawable {
        val radiusPx = if (context != null) cornerRadiusDp * context.resources.displayMetrics.density else cornerRadiusDp
        return GradientDrawable(
            orientation,
            intArrayOf(CARD_GRADIENT_START, CARD_GRADIENT_END)
        ).apply {
            cornerRadius = radiusPx
        }
    }

    /**
     * Creates a GradientDrawable for "Gradient red" (0% #FF8082 -> 100% #F64A4D).
     *
     * @param orientation Gradient direction, defaults to LEFT_RIGHT.
     * @param cornerRadiusDp Corner radius in dp (automatically converted to px if context provided).
     * @param context Optional context to resolve dp into px.
     */
    fun createRedGradient(
        orientation: GradientDrawable.Orientation = GradientDrawable.Orientation.LEFT_RIGHT,
        cornerRadiusDp: Float = 16f,
        context: Context? = null
    ): GradientDrawable {
        val radiusPx = if (context != null) cornerRadiusDp * context.resources.displayMetrics.density else cornerRadiusDp
        return GradientDrawable(
            orientation,
            intArrayOf(GRADIENT_RED_START, GRADIENT_RED_END)
        ).apply {
            cornerRadius = radiusPx
        }
    }
}

// =============================================================================
// 4. Concise Kotlin Extensions for Context & View
// =============================================================================

/** Quick color resolver from Context: `context.color(R.color.primary_color)` */
fun Context.color(@ColorRes resId: Int): Int = ContextCompat.getColor(this, resId)

/** Quick color resolver from View: `view.color(R.color.primary_color)` */
fun View.color(@ColorRes resId: Int): Int = ContextCompat.getColor(context, resId)
