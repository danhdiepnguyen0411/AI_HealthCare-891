package com.example.healthcareai.ui.heartrate

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.ImageProxy
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.healthcareai.R
import com.example.healthcareai.data.model.HrvResult
import com.example.healthcareai.data.model.MeasureError
import com.example.healthcareai.data.local.PreferenceHelper
import com.example.healthcareai.data.model.MeasureState
import com.example.healthcareai.data.model.SignalQuality
import com.example.healthcareai.databinding.ActivityHeartRateBinding
import com.example.healthcareai.ui.base.BaseActivity
import com.example.healthcareai.ui.dialog.CustomCameraPermissionDialog
import com.example.healthcareai.ui.heartrate.engine.CameraFrameSource
import com.example.healthcareai.util.PermissionReturnHelper
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class HeartRateActivity : BaseActivity() {

    private lateinit var binding: ActivityHeartRateBinding
    private lateinit var preferenceHelper: PreferenceHelper
    private val viewModel: HeartRateViewModel by viewModels()

    private var heartbeatAnimatorSet: AnimatorSet? = null
    private var handGuideAnimatorSet: AnimatorSet? = null

    private var cameraFrameSource: CameraFrameSource? = null
    private var isCameraActive = false
    private var hasMeasuredSuccessfully = false
    private var permissionDialog: Dialog? = null

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                preferenceHelper.cameraDenialCount = 0
                startHeartRateMeasurement()
            } else {
                preferenceHelper.cameraDenialCount++
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHeartRateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceHelper = PreferenceHelper.getInstance(this)
        cameraFrameSource = CameraFrameSource(this, this)

        setupWindowInsets()
        setupListeners()
        setupHeartShadowEffect()
        startHeartbeatAnimation()
        startHandGuideAnimation()
        binding.ecgPulseLineView.startFlowAnimation()
        observeMeasureState()

        // Automatically start camera and measurement right away
        checkPermissionAndStartMeasurement()
    }

    private fun setupHeartShadowEffect() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            try {
                binding.ivHeartShadow.setRenderEffect(
                    android.graphics.RenderEffect.createBlurEffect(
                        22f,
                        22f,
                        android.graphics.Shader.TileMode.CLAMP
                    )
                )
            } catch (_: Exception) {
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (PermissionReturnHelper.isPermissionGranted(this, android.Manifest.permission.CAMERA)) {
            preferenceHelper.cameraDenialCount = 0
            dismissPermissionDialog()
            if (hasMeasuredSuccessfully) {
                hasMeasuredSuccessfully = false
                binding.layoutCheckmarkBadge.visibility = View.GONE
                binding.tvProgressPill.text = "--"
                binding.tvProgressPill.setTextColor(Color.parseColor("#242E49"))
                binding.viewProgressFill.updateLayoutParams { width = 0 }
                binding.tvLiveBpm.text = "--"
                startHeartRateMeasurement()
            } else if (!isCameraActive) {
                startHeartRateMeasurement()
            }
        }
        if (heartbeatAnimatorSet?.isPaused == true) {
            heartbeatAnimatorSet?.resume()
        }
        if (handGuideAnimatorSet?.isPaused == true) {
            handGuideAnimatorSet?.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        stopCamera()
        heartbeatAnimatorSet?.pause()
        handGuideAnimatorSet?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        PermissionReturnHelper.stopPolling()
        dismissPermissionDialog()
        stopCamera()
        cameraFrameSource?.destroy()
        cameraFrameSource = null
        heartbeatAnimatorSet?.cancel()
        heartbeatAnimatorSet = null
        handGuideAnimatorSet?.cancel()
        handGuideAnimatorSet = null
        binding.ecgPulseLineView.stopFlowAnimation()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        stopCamera()
        viewModel.cancelMeasurement()
        super.onBackPressed()
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
                binding.cardProgressPill.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = navBarBottom + 28
                }
            }
            insets
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            stopCamera()
            viewModel.cancelMeasurement()
            finish()
        }

        binding.btnStatistics.setOnClickListener {
            HeartRateStatisticsActivity.start(this)
        }

        binding.tvInstruction.setOnClickListener {
            showHowToMeasureBottomSheet()
        }

        binding.phoneHandContainer.setOnClickListener {
            showHowToMeasureBottomSheet()
        }

        binding.layoutMeasuringHeader.setOnClickListener {
            showHowToMeasureBottomSheet()
        }
    }

    private fun showHowToMeasureBottomSheet() {
        HowToMeasureBottomSheetDialog.show(supportFragmentManager)
    }

    private fun checkPermissionAndStartMeasurement() {
        val isCameraGranted = PermissionReturnHelper.isPermissionGranted(
            this,
            android.Manifest.permission.CAMERA
        )
        if (isCameraGranted) {
            preferenceHelper.cameraDenialCount = 0
            startHeartRateMeasurement()
            return
        }

        val denialCount = preferenceHelper.cameraDenialCount
        when {
            denialCount < 2 -> {
                requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
            else -> {
                showCustomCameraPermissionDialog()
            }
        }
    }

    private fun showCustomCameraPermissionDialog() {
        if (isFinishing || isDestroyed || permissionDialog?.isShowing == true) return

        permissionDialog = CustomCameraPermissionDialog.show(
            activity = this,
            onGranted = {
                preferenceHelper.cameraDenialCount = 0
                startHeartRateMeasurement()
            },
            onDenied = {
                // User dismissed
            }
        )
    }

    private fun dismissPermissionDialog() {
        permissionDialog?.dismiss()
        permissionDialog = null
    }

    /**
     * Starts CameraX stream and measurement use case pipeline.
     */
    private fun startHeartRateMeasurement() {
        isCameraActive = true
        viewModel.startMeasurement()

        binding.tvProgressPill.text = "--"
        binding.viewProgressFill.updateLayoutParams { width = 0 }
        binding.layoutCheckmarkBadge.visibility = View.GONE

        cameraFrameSource?.start(binding.previewView, object : CameraFrameSource.FrameListener {
            override fun onFrameAvailable(imageProxy: ImageProxy) {
                viewModel.processFrame(imageProxy)
            }

            override fun onCameraReady(hasFlash: Boolean) {
                runOnUiThread {
                    if (!hasFlash) {
                        Toast.makeText(
                            this@HeartRateActivity,
                            R.string.heart_rate_error_no_flash,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

            override fun onCameraError(exception: Exception) {
                isCameraActive = false
                runOnUiThread {
                    Toast.makeText(
                        this@HeartRateActivity,
                        exception.localizedMessage ?: "Camera error",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }

    private fun stopCamera() {
        isCameraActive = false
        cameraFrameSource?.stop()
    }

    private fun observeMeasureState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.measureState.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(state: MeasureState) {
        when (state) {
            is MeasureState.Idle -> {
                // Initial
            }

            is MeasureState.WaitingForFinger -> {
                binding.layoutCheckmarkBadge.visibility = View.GONE
                binding.tvProgressPill.text = "--"
                binding.tvProgressPill.setTextColor(Color.parseColor("#242E49"))
                binding.viewProgressFill.updateLayoutParams { width = 0 }
                binding.tvLiveBpm.text = "--"
                if (handGuideAnimatorSet?.isPaused == true) {
                    handGuideAnimatorSet?.resume()
                }
            }

            is MeasureState.Measuring -> {
                // Finger is placed firmly on camera!
                binding.layoutCheckmarkBadge.visibility = View.VISIBLE

                val pct = (state.progress * 100).toInt()
                binding.tvProgressPill.text = "$pct%"
                if (state.progress >= 0.5f) {
                    binding.tvProgressPill.setTextColor(Color.WHITE)
                } else {
                    binding.tvProgressPill.setTextColor(Color.parseColor("#242E49"))
                }

                val pillWidth = binding.cardProgressPill.width
                if (pillWidth > 0) {
                    val targetW = (pillWidth * state.progress).toInt()
                    binding.viewProgressFill.updateLayoutParams { width = targetW }
                }

                // Real-time BPM display directly on the heart
                if (state.preliminaryBpm > 0) {
                    binding.tvLiveBpm.text = state.preliminaryBpm.toString()
                } else {
                    binding.tvLiveBpm.text = "--"
                }
            }

            is MeasureState.Success -> {
                stopCamera()
                binding.layoutCheckmarkBadge.visibility = View.VISIBLE
                binding.tvProgressPill.text = "100%"
                binding.tvProgressPill.setTextColor(Color.WHITE)
                val pillWidth = binding.cardProgressPill.width
                if (pillWidth > 0) {
                    binding.viewProgressFill.updateLayoutParams { width = pillWidth }
                }
                binding.tvLiveBpm.text = state.bpm.toString()

                hasMeasuredSuccessfully = true
                val record = viewModel.saveMeasurement(state.bpm, state.hrv, state.quality)
                viewModel.reset()

                val intent = HeartRateResultActivity.createIntent(
                    context = this,
                    recordId = record.id,
                    bpm = state.bpm,
                    timestamp = record.timestamp
                )
                startActivity(intent)
                finish()
            }

            is MeasureState.Error -> {
                binding.tvLiveBpm.text = "--"
                if (state.reason == MeasureError.FINGER_LOST) {
                    binding.layoutCheckmarkBadge.visibility = View.GONE
                    binding.tvProgressPill.text = "--"
                    binding.tvProgressPill.setTextColor(Color.parseColor("#242E49"))
                    binding.viewProgressFill.updateLayoutParams { width = 0 }
                } else if (state.reason == MeasureError.LOW_QUALITY_SIGNAL) {
                    Toast.makeText(
                        this,
                        R.string.heart_rate_error_low_quality,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun startHeartbeatAnimation() {
        val heart = binding.ivHeart
        val shadow = binding.ivHeartShadow
        val overlay = binding.layoutHeartBpmOverlay

        // Scale up
        val scaleUpX = ObjectAnimator.ofFloat(heart, "scaleX", 1f, 1.07f).apply {
            duration = 450
            interpolator = AccelerateDecelerateInterpolator()
        }
        val scaleUpY = ObjectAnimator.ofFloat(heart, "scaleY", 1f, 1.07f).apply {
            duration = 450
            interpolator = AccelerateDecelerateInterpolator()
        }
        val shadowUpX = ObjectAnimator.ofFloat(shadow, "scaleX", 1f, 1.07f).apply {
            duration = 450
            interpolator = AccelerateDecelerateInterpolator()
        }
        val shadowUpY = ObjectAnimator.ofFloat(shadow, "scaleY", 1f, 1.07f).apply {
            duration = 450
            interpolator = AccelerateDecelerateInterpolator()
        }
        val overlayUpX = ObjectAnimator.ofFloat(overlay, "scaleX", 1f, 1.07f).apply {
            duration = 450
            interpolator = AccelerateDecelerateInterpolator()
        }
        val overlayUpY = ObjectAnimator.ofFloat(overlay, "scaleY", 1f, 1.07f).apply {
            duration = 450
            interpolator = AccelerateDecelerateInterpolator()
        }

        // Scale down
        val scaleDownX = ObjectAnimator.ofFloat(heart, "scaleX", 1.07f, 1f).apply {
            duration = 550
            interpolator = AccelerateDecelerateInterpolator()
        }
        val scaleDownY = ObjectAnimator.ofFloat(heart, "scaleY", 1.07f, 1f).apply {
            duration = 550
            interpolator = AccelerateDecelerateInterpolator()
        }
        val shadowDownX = ObjectAnimator.ofFloat(shadow, "scaleX", 1.07f, 1f).apply {
            duration = 550
            interpolator = AccelerateDecelerateInterpolator()
        }
        val shadowDownY = ObjectAnimator.ofFloat(shadow, "scaleY", 1.07f, 1f).apply {
            duration = 550
            interpolator = AccelerateDecelerateInterpolator()
        }
        val overlayDownX = ObjectAnimator.ofFloat(overlay, "scaleX", 1.07f, 1f).apply {
            duration = 550
            interpolator = AccelerateDecelerateInterpolator()
        }
        val overlayDownY = ObjectAnimator.ofFloat(overlay, "scaleY", 1.07f, 1f).apply {
            duration = 550
            interpolator = AccelerateDecelerateInterpolator()
        }

        val beatUp = AnimatorSet().apply {
            playTogether(scaleUpX, scaleUpY, shadowUpX, shadowUpY, overlayUpX, overlayUpY)
        }
        val beatDown = AnimatorSet().apply {
            playTogether(scaleDownX, scaleDownY, shadowDownX, shadowDownY, overlayDownX, overlayDownY)
        }

        heartbeatAnimatorSet = AnimatorSet().apply {
            playSequentially(beatUp, beatDown)
            start()
        }

        heartbeatAnimatorSet?.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (!isFinishing && !isDestroyed) {
                    heartbeatAnimatorSet?.start()
                }
            }
        })
    }

    private fun startHandGuideAnimation() {
        val hand = binding.ivHand
        val pulse = binding.viewTouchPulse
        val density = resources.displayMetrics.density

        // Hand starting position: offset down and to the right
        val startOffsetX = 45f * density
        val startOffsetY = 60f * density

        hand.translationX = startOffsetX
        hand.translationY = startOffsetY
        hand.scaleX = 0.94f
        hand.scaleY = 0.94f
        hand.alpha = 0f
        pulse.alpha = 0f
        pulse.scaleX = 0.6f
        pulse.scaleY = 0.6f

        // Phase 1: Move in
        val moveInX = ObjectAnimator.ofFloat(hand, "translationX", startOffsetX, 0f).apply {
            duration = 1100
            interpolator = DecelerateInterpolator(1.4f)
        }
        val moveInY = ObjectAnimator.ofFloat(hand, "translationY", startOffsetY, 0f).apply {
            duration = 1100
            interpolator = DecelerateInterpolator(1.4f)
        }
        val scaleInX = ObjectAnimator.ofFloat(hand, "scaleX", 0.94f, 1f).apply {
            duration = 1100
            interpolator = DecelerateInterpolator(1.4f)
        }
        val scaleInY = ObjectAnimator.ofFloat(hand, "scaleY", 0.94f, 1f).apply {
            duration = 1100
            interpolator = DecelerateInterpolator(1.4f)
        }
        val fadeIn = ObjectAnimator.ofFloat(hand, "alpha", 0f, 1f).apply {
            duration = 800
            interpolator = DecelerateInterpolator()
        }

        val moveInSet = AnimatorSet().apply {
            playTogether(moveInX, moveInY, scaleInX, scaleInY, fadeIn)
        }

        // Phase 2: Tap contact & pulse
        val tapSettleX = ObjectAnimator.ofFloat(hand, "scaleX", 1f, 0.985f, 1f).apply {
            duration = 260
            interpolator = AccelerateDecelerateInterpolator()
        }
        val tapSettleY = ObjectAnimator.ofFloat(hand, "scaleY", 1f, 0.985f, 1f).apply {
            duration = 260
            interpolator = AccelerateDecelerateInterpolator()
        }

        val pulseAlpha1 = ObjectAnimator.ofFloat(pulse, "alpha", 0f, 0.85f, 0.4f).apply {
            duration = 550
            interpolator = DecelerateInterpolator()
        }
        val pulseScaleX1 = ObjectAnimator.ofFloat(pulse, "scaleX", 0.6f, 1.35f).apply {
            duration = 550
            interpolator = DecelerateInterpolator()
        }
        val pulseScaleY1 = ObjectAnimator.ofFloat(pulse, "scaleY", 0.6f, 1.35f).apply {
            duration = 550
            interpolator = DecelerateInterpolator()
        }

        val pulseAlpha2 = ObjectAnimator.ofFloat(pulse, "alpha", 0.4f, 0.95f, 0.35f, 0.9f, 0f).apply {
            duration = 1200
            interpolator = AccelerateDecelerateInterpolator()
        }
        val pulseScaleX2 = ObjectAnimator.ofFloat(pulse, "scaleX", 1.1f, 1.4f, 1.15f, 1.45f, 1.1f).apply {
            duration = 1200
            interpolator = AccelerateDecelerateInterpolator()
        }
        val pulseScaleY2 = ObjectAnimator.ofFloat(pulse, "scaleY", 1.1f, 1.4f, 1.15f, 1.45f, 1.1f).apply {
            duration = 1200
            interpolator = AccelerateDecelerateInterpolator()
        }

        val pulseCycle = AnimatorSet().apply {
            val p1 = AnimatorSet().apply {
                playTogether(pulseAlpha1, pulseScaleX1, pulseScaleY1)
            }
            val p2 = AnimatorSet().apply {
                playTogether(pulseAlpha2, pulseScaleX2, pulseScaleY2)
            }
            playSequentially(p1, p2)
        }

        val holdAndMeasureSet = AnimatorSet().apply {
            playTogether(tapSettleX, tapSettleY, pulseCycle)
        }

        // Phase 3: Lift off and retract
        val retractX = ObjectAnimator.ofFloat(hand, "translationX", 0f, startOffsetX).apply {
            duration = 750
            interpolator = AccelerateDecelerateInterpolator()
        }
        val retractY = ObjectAnimator.ofFloat(hand, "translationY", 0f, startOffsetY).apply {
            duration = 750
            interpolator = AccelerateDecelerateInterpolator()
        }
        val scaleOutX = ObjectAnimator.ofFloat(hand, "scaleX", 1f, 0.94f).apply {
            duration = 750
            interpolator = AccelerateDecelerateInterpolator()
        }
        val scaleOutY = ObjectAnimator.ofFloat(hand, "scaleY", 1f, 0.94f).apply {
            duration = 750
            interpolator = AccelerateDecelerateInterpolator()
        }
        val fadeOut = ObjectAnimator.ofFloat(hand, "alpha", 1f, 0f).apply {
            duration = 700
            interpolator = AccelerateDecelerateInterpolator()
        }
        val pulseOff = ObjectAnimator.ofFloat(pulse, "alpha", pulse.alpha, 0f).apply {
            duration = 300
        }

        val retractSet = AnimatorSet().apply {
            playTogether(retractX, retractY, scaleOutX, scaleOutY, fadeOut, pulseOff)
        }

        // Phase 4: Pause
        val pauseAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 350
        }

        handGuideAnimatorSet = AnimatorSet().apply {
            playSequentially(moveInSet, holdAndMeasureSet, retractSet, pauseAnim)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (!isFinishing && !isDestroyed) {
                        handGuideAnimatorSet?.start()
                    }
                }
            })
            start()
        }
    }
}
