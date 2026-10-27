package com.example.healthcareai.ui.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import com.example.healthcareai.R
import com.example.healthcareai.data.local.PreferenceHelper
import com.example.healthcareai.databinding.DialogRateAppBinding
import com.example.healthcareai.util.DialogImmersiveHelper

class RateAppDialog(
    context: Context,
    private val onSubmit: (Int) -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogRateAppBinding
    private var currentRating: Int = 5
    private lateinit var starViews: List<ImageView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogRateAppBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val width = (context.resources.displayMetrics.widthPixels * 0.85f).toInt().coerceAtMost(
                (340 * context.resources.displayMetrics.density).toInt()
            )
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            setDimAmount(0.55f)
        }

        setupViews()
    }

    private fun setupViews() {
        starViews = listOf(
            binding.ivStar1,
            binding.ivStar2,
            binding.ivStar3,
            binding.ivStar4,
            binding.ivStar5
        )

        starViews.forEachIndexed { index, starView ->
            starView.setOnClickListener {
                updateRating(index + 1)
            }
        }

        updateRating(5)

        binding.btnSubmitRating.setOnClickListener {
            PreferenceHelper.getInstance(context).isAppRated = true
            dismiss()
            onSubmit(currentRating)
        }

        binding.btnMaybeLater.setOnClickListener {
            dismiss()
        }
    }

    private fun updateRating(rating: Int) {
        currentRating = rating

        // Update emoji with bounce animation
        val emojiRes = when (rating) {
            1 -> R.drawable.ic_rate_1_star
            2 -> R.drawable.ic_rate_2_star
            3 -> R.drawable.ic_rate_3_star
            4 -> R.drawable.ic_rate_4_star
            else -> R.drawable.ic_rate_5_star
        }
        binding.ivRateEmoji.setImageResource(emojiRes)
        binding.ivRateEmoji.scaleX = 0.82f
        binding.ivRateEmoji.scaleY = 0.82f
        binding.ivRateEmoji.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(220)
            .start()

        // Update stars filled / empty states
        starViews.forEachIndexed { index, starView ->
            if (index < rating) {
                starView.setImageResource(R.drawable.ic_star_filled)
            } else {
                starView.setImageResource(R.drawable.ic_star_empty)
            }
        }
    }

    companion object {
        fun show(
            context: Context,
            onSubmit: (Int) -> Unit
        ): RateAppDialog {
            val dialog = RateAppDialog(context, onSubmit)
            DialogImmersiveHelper.showImmersive(dialog)
            return dialog
        }
    }
}
