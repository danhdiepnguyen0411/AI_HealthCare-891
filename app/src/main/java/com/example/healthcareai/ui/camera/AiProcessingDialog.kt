package com.example.healthcareai.ui.camera

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.example.healthcareai.databinding.DialogAiProcessingBinding
import com.example.healthcareai.util.DialogImmersiveHelper

class AiProcessingDialog : DialogFragment() {

    private var _binding: DialogAiProcessingBinding? = null
    private val binding get() = _binding!!

    private var pulseAnimator: ObjectAnimator? = null
    private var sparkle1Animator: ObjectAnimator? = null
    private var sparkle2Animator: ObjectAnimator? = null
    private var sparkle3Animator: ObjectAnimator? = null
    private var sparkle4Animator: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAiProcessingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startAnimations()
    }

    private fun startAnimations() {
        // 1. Gentle breathing / pulse on the robot graphic
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.96f, 1.04f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.96f, 1.04f)
        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(binding.ivRobotGraphic, scaleX, scaleY).apply {
            duration = 1100
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        // 2. Sparkle twinkling animations with staggered intervals
        val sparkleAlpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.35f, 1.0f)
        val sparkleScaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.75f, 1.25f)
        val sparkleScaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.75f, 1.25f)

        sparkle1Animator = ObjectAnimator.ofPropertyValuesHolder(
            binding.ivSparkleTopRight,
            sparkleAlpha,
            sparkleScaleX,
            sparkleScaleY
        ).apply {
            duration = 900
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        sparkle2Animator = ObjectAnimator.ofPropertyValuesHolder(
            binding.ivSparkleLeft,
            sparkleAlpha,
            sparkleScaleX,
            sparkleScaleY
        ).apply {
            duration = 1300
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        sparkle3Animator = ObjectAnimator.ofPropertyValuesHolder(
            binding.ivSparkleTopRightSmall,
            sparkleAlpha,
            sparkleScaleX,
            sparkleScaleY
        ).apply {
            duration = 800
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        sparkle4Animator = ObjectAnimator.ofPropertyValuesHolder(
            binding.ivSparkleBottomLeft,
            sparkleAlpha,
            sparkleScaleX,
            sparkleScaleY
        ).apply {
            duration = 1150
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val density = resources.displayMetrics.density
            val maxWidthPx = (300 * density).toInt()
            val screenWidthPx = (resources.displayMetrics.widthPixels * 0.85f).toInt()
            val dialogWidth = screenWidthPx.coerceAtMost(maxWidthPx)
            window.setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
            window.setDimAmount(0.5f)
            DialogImmersiveHelper.hideWindowNavigationBar(window)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding?.viewRotatingRing?.stopAnimation()
        pulseAnimator?.cancel()
        sparkle1Animator?.cancel()
        sparkle2Animator?.cancel()
        sparkle3Animator?.cancel()
        sparkle4Animator?.cancel()
        _binding = null
    }

    fun dismissSafely() {
        try {
            dismissAllowingStateLoss()
        } catch (_: Exception) {}
    }

    companion object {
        const val TAG = "AiProcessingDialog"

        fun show(fragmentManager: FragmentManager): AiProcessingDialog {
            val existing = fragmentManager.findFragmentByTag(TAG) as? AiProcessingDialog
            if (existing != null) {
                return existing
            }
            val dialog = AiProcessingDialog()
            dialog.show(fragmentManager, TAG)
            return dialog
        }
    }
}
