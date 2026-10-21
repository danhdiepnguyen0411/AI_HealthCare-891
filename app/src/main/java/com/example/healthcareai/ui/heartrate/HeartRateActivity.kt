package com.example.healthcareai.ui.heartrate

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.example.healthcareai.R
import com.example.healthcareai.databinding.ActivityHeartRateBinding
import com.example.healthcareai.databinding.DialogHowToMeasureBinding
import com.example.healthcareai.ui.base.BaseActivity
import com.example.healthcareai.util.DialogImmersiveHelper
import com.example.healthcareai.util.PermissionReturnHelper

class HeartRateActivity : BaseActivity() {

    private lateinit var binding: ActivityHeartRateBinding
    private var guideDialog: Dialog? = null
    private var heartbeatAnimatorSet: AnimatorSet? = null

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startHeartRateMeasurement()
            } else {
                Toast.makeText(
                    this,
                    R.string.permission_desc,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHeartRateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupListeners()
        startHeartbeatAnimation()
    }

    override fun onResume() {
        super.onResume()
        if (heartbeatAnimatorSet?.isPaused == true) {
            heartbeatAnimatorSet?.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        heartbeatAnimatorSet?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissGuideDialog()
        heartbeatAnimatorSet?.cancel()
        heartbeatAnimatorSet = null
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootHeartRateLayout) { _, insets ->
            val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val navBarBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom

            if (statusBarTop > 0) {
                binding.topBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = statusBarTop + 8
                }
            }
            if (navBarBottom > 0) {
                binding.btnStartMeasure.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = navBarBottom + 24
                }
            }
            insets
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnStatistics.setOnClickListener {
            Toast.makeText(
                this,
                R.string.heart_rate_statistics_coming_soon,
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.layoutHowToMeasure.setOnClickListener {
            showHowToMeasureDialog()
        }

        binding.btnStartMeasure.setOnClickListener {
            onStartMeasureClicked()
        }
    }

    private fun onStartMeasureClicked() {
        val isCameraGranted = PermissionReturnHelper.isPermissionGranted(
            this,
            android.Manifest.permission.CAMERA
        )
        if (isCameraGranted) {
            startHeartRateMeasurement()
        } else {
            requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    private fun startHeartRateMeasurement() {
        Toast.makeText(
            this,
            "Place your finger on camera & flash to measure.",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showHowToMeasureDialog() {
        if (isFinishing || isDestroyed || guideDialog?.isShowing == true) return

        val dialog = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            val dialogBinding = DialogHowToMeasureBinding.inflate(layoutInflater)
            setContentView(dialogBinding.root)

            window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            setCancelable(true)
            setCanceledOnTouchOutside(true)

            dialogBinding.btnGotIt.setOnClickListener {
                dismiss()
            }

            setOnDismissListener {
                guideDialog = null
            }
        }

        guideDialog = dialog
        DialogImmersiveHelper.showImmersive(dialog)
    }

    private fun dismissGuideDialog() {
        guideDialog?.dismiss()
        guideDialog = null
    }

    private fun startHeartbeatAnimation() {
        val heart = binding.ivHeart
        val glow = binding.viewHeartGlow

        // Scale up
        val scaleUpX = ObjectAnimator.ofFloat(heart, "scaleX", 1f, 1.08f).apply {
            duration = 450
            interpolator = AccelerateDecelerateInterpolator()
        }
        val scaleUpY = ObjectAnimator.ofFloat(heart, "scaleY", 1f, 1.08f).apply {
            duration = 450
            interpolator = AccelerateDecelerateInterpolator()
        }

        val glowScaleUpX = ObjectAnimator.ofFloat(glow, "scaleX", 1f, 1.15f).apply {
            duration = 450
            interpolator = AccelerateDecelerateInterpolator()
        }
        val glowScaleUpY = ObjectAnimator.ofFloat(glow, "scaleY", 1f, 1.15f).apply {
            duration = 450
            interpolator = AccelerateDecelerateInterpolator()
        }
        val glowAlphaUp = ObjectAnimator.ofFloat(glow, "alpha", 0.7f, 1.0f).apply {
            duration = 450
            interpolator = AccelerateDecelerateInterpolator()
        }

        // Scale down
        val scaleDownX = ObjectAnimator.ofFloat(heart, "scaleX", 1.08f, 1f).apply {
            duration = 550
            interpolator = AccelerateDecelerateInterpolator()
        }
        val scaleDownY = ObjectAnimator.ofFloat(heart, "scaleY", 1.08f, 1f).apply {
            duration = 550
            interpolator = AccelerateDecelerateInterpolator()
        }

        val glowScaleDownX = ObjectAnimator.ofFloat(glow, "scaleX", 1.15f, 1f).apply {
            duration = 550
            interpolator = AccelerateDecelerateInterpolator()
        }
        val glowScaleDownY = ObjectAnimator.ofFloat(glow, "scaleY", 1.15f, 1f).apply {
            duration = 550
            interpolator = AccelerateDecelerateInterpolator()
        }
        val glowAlphaDown = ObjectAnimator.ofFloat(glow, "alpha", 1.0f, 0.7f).apply {
            duration = 550
            interpolator = AccelerateDecelerateInterpolator()
        }

        val beatUp = AnimatorSet().apply {
            playTogether(scaleUpX, scaleUpY, glowScaleUpX, glowScaleUpY, glowAlphaUp)
        }
        val beatDown = AnimatorSet().apply {
            playTogether(scaleDownX, scaleDownY, glowScaleDownX, glowScaleDownY, glowAlphaDown)
        }

        heartbeatAnimatorSet = AnimatorSet().apply {
            playSequentially(beatUp, beatDown)
            start()
        }

        // Loop gently
        heartbeatAnimatorSet?.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                if (!isFinishing && !isDestroyed) {
                    heartbeatAnimatorSet?.start()
                }
            }
        })
    }
}
