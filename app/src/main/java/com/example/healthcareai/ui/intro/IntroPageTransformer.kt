package com.example.healthcareai.ui.intro

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import com.example.healthcareai.R
import kotlin.math.abs

class IntroPageTransformer : ViewPager2.PageTransformer {

    override fun transformPage(page: View, position: Float) {
        val illustration = page.findViewById<View>(R.id.ivIllustration)
        val title = page.findViewById<View>(R.id.tvTitle)
        val description = page.findViewById<View>(R.id.tvDescription)

        val absPos = abs(position)
        val pageWidth = page.width.toFloat()

        if (position < -1f) {
            page.alpha = 0f
        } else if (position <= 1f) {
            page.alpha = 1f

            // Layered 3D parallax for the top illustration (full bleed, no shrinking)
            illustration?.let {
                it.translationX = position * pageWidth * 0.20f
                it.alpha = (1f - absPos * 0.40f).coerceIn(0f, 1f)
            }

            // Parallax movement & smooth fade on title
            title?.let {
                it.translationX = position * pageWidth * 0.35f
                it.alpha = (1f - absPos * 0.85f).coerceIn(0f, 1f)
            }

            // Parallax movement & smooth fade on description
            description?.let {
                it.translationX = position * pageWidth * 0.22f
                it.alpha = (1f - absPos * 0.85f).coerceIn(0f, 1f)
            }
        } else {
            page.alpha = 0f
        }
    }
}
