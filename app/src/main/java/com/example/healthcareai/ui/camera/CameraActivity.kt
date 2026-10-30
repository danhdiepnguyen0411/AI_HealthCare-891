package com.example.healthcareai.ui.camera

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import com.example.healthcareai.R
import com.example.healthcareai.data.local.PreferenceHelper
import com.example.healthcareai.databinding.ActivityCameraBinding
import com.example.healthcareai.databinding.DialogPhotoGuideBinding
import com.example.healthcareai.ui.base.BaseActivity
import com.example.healthcareai.ui.dialog.CustomCameraPermissionDialog
import com.example.healthcareai.util.DialogImmersiveHelper
import com.example.healthcareai.util.PermissionReturnHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

class CameraActivity : BaseActivity() {

    private lateinit var binding: ActivityCameraBinding
    private lateinit var preferenceHelper: PreferenceHelper

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK

    private var permissionDialog: Dialog? = null
    private var photoGuideDialog: Dialog? = null

    // Native Camera Permission Launcher
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d(TAG, "Native camera permission GRANTED")
            preferenceHelper.cameraDenialCount = 0
            startCamera()
        } else {
            preferenceHelper.cameraDenialCount++
            Log.d(TAG, "Native camera permission DENIED. Total denials: ${preferenceHelper.cameraDenialCount}")
        }
    }

    // Gallery Picker Launcher
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                val bitmap = CapturedImageHolder.decodeScreenScaledBitmap(this@CameraActivity, uri)
                CapturedImageHolder.bitmap = bitmap
                CapturedImageHolder.imageUri = uri
                withContext(Dispatchers.Main) {
                    val intent = Intent(this@CameraActivity, ConfirmPhotoActivity::class.java).apply {
                        putExtra(ConfirmPhotoActivity.EXTRA_IMAGE_URI, uri.toString())
                    }
                    startActivity(intent)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceHelper = PreferenceHelper.getInstance(this)

        setupWindowInsets()
        setupViewfinderOverlay()
        setupListeners()
        handleCameraPermissionFlow()
    }

    override fun onResume() {
        super.onResume()
        binding.btnCapture.isEnabled = true
        // Synchronous check if permission was granted via Settings while paused
        if (PermissionReturnHelper.isPermissionGranted(this, Manifest.permission.CAMERA)) {
            preferenceHelper.cameraDenialCount = 0
            dismissPermissionDialog()
            if (cameraProvider == null) {
                startCamera()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        PermissionReturnHelper.stopPolling()
        dismissPermissionDialog()
        dismissPhotoGuideDialog()
    }

    // =========================================================================
    // Permission Management: 2 Native Attempts Threshold -> Custom Pop-up
    // =========================================================================

    private fun handleCameraPermissionFlow() {
        val isGranted = PermissionReturnHelper.isPermissionGranted(this, Manifest.permission.CAMERA)

        if (isGranted) {
            preferenceHelper.cameraDenialCount = 0
            startCamera()
            return
        }

        val denialCount = preferenceHelper.cameraDenialCount
        Log.d(TAG, "Handling camera permission flow. Current denial count: $denialCount")

        when {
            // Attempt 1 & Attempt 2: Show OS native permission dialog (Bottom Sheet style on Android 11+)
            denialCount < 2 -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            // Attempt 3 onwards: Show custom rationale pop-up with auto-return
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
                startCamera()
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

    // =========================================================================
    // Photo Guide Dialog
    // =========================================================================

    private fun showPhotoGuideDialog() {
        if (isFinishing || isDestroyed || photoGuideDialog?.isShowing == true) return

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogBinding = DialogPhotoGuideBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        dialogBinding.btnGotIt.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            photoGuideDialog = null
        }

        photoGuideDialog = dialog
        DialogImmersiveHelper.showImmersive(dialog)
    }

    private fun dismissPhotoGuideDialog() {
        photoGuideDialog?.dismiss()
        photoGuideDialog = null
    }

    // =========================================================================
    // CameraX Implementation
    // =========================================================================

    private fun startCamera() {
        if (!PermissionReturnHelper.isPermissionGranted(this, Manifest.permission.CAMERA)) return

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start camera", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val provider = cameraProvider ?: return

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(binding.previewView.surfaceProvider)
        }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        try {
            provider.unbindAll()
            provider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            Log.d(TAG, "Camera bound successfully with lensFacing: $lensFacing")
        } catch (e: Exception) {
            Log.e(TAG, "Camera binding error", e)
        }
    }

    private fun toggleCameraFacing() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        bindCameraUseCases()
    }

    // =========================================================================
    // UI Setup & Listeners
    // =========================================================================

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootCameraLayout) { _, insets ->
            val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            if (statusBarTop > 0) {
                binding.topBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = statusBarTop + 8
                }
            }
            insets
        }
    }

    private fun setupViewfinderOverlay() {
        binding.flViewfinder.addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
            binding.overlayView.setHoleRect(
                left.toFloat(),
                top.toFloat(),
                right.toFloat(),
                bottom.toFloat()
            )
        }
    }

    private fun setupListeners() {
        // Back Button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Photo Guide Button (?)
        binding.btnPhotoGuide.setOnClickListener {
            showPhotoGuideDialog()
        }

        // Album Button
        binding.btnAlbum.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // Flip Camera Button
        binding.btnFlipCamera.setOnClickListener {
            toggleCameraFacing()
        }

        // Shutter / Capture Button
        binding.btnCapture.setOnClickListener {
            if (!PermissionReturnHelper.isPermissionGranted(this, Manifest.permission.CAMERA)) {
                handleCameraPermissionFlow()
                return@setOnClickListener
            }

            // Snappy shutter feedback animation
            binding.btnCapture.animate()
                .scaleX(0.88f)
                .scaleY(0.88f)
                .setDuration(70)
                .withEndAction {
                    binding.btnCapture.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(100)
                        .start()
                }
                .start()

            // Instant white shutter flash effect
            binding.shutterFlash.apply {
                visibility = View.VISIBLE
                alpha = 0.8f
                animate()
                    .alpha(0f)
                    .setDuration(160)
                    .withEndAction { visibility = View.GONE }
                    .start()
            }

            binding.btnCapture.isEnabled = false

            val capture = imageCapture
            if (capture != null) {
                val photoFile = File(cacheDir, "scan_${System.currentTimeMillis()}.jpg")
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                val cameraIoExecutor = Executors.newSingleThreadExecutor()

                capture.takePicture(
                    outputOptions,
                    cameraIoExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            // Decode scaled bitmap immediately on background executor (<20ms)
                            val scaledBitmap = CapturedImageHolder.decodeScreenScaledBitmap(photoFile.absolutePath)
                            CapturedImageHolder.bitmap = scaledBitmap ?: binding.previewView.bitmap
                            CapturedImageHolder.imagePath = photoFile.absolutePath

                            runOnUiThread {
                                binding.btnCapture.isEnabled = true
                                val intent = Intent(this@CameraActivity, ConfirmPhotoActivity::class.java).apply {
                                    putExtra(ConfirmPhotoActivity.EXTRA_IMAGE_PATH, photoFile.absolutePath)
                                }
                                startActivity(intent)
                            }
                        }

                        override fun onError(exception: androidx.camera.core.ImageCaptureException) {
                            Log.e(TAG, "Photo capture error", exception)
                            val fallbackBmp = binding.previewView.bitmap
                            CapturedImageHolder.bitmap = fallbackBmp
                            runOnUiThread {
                                binding.btnCapture.isEnabled = true
                                val intent = Intent(this@CameraActivity, ConfirmPhotoActivity::class.java)
                                startActivity(intent)
                            }
                        }
                    }
                )
            } else {
                val fallbackBmp = binding.previewView.bitmap
                CapturedImageHolder.bitmap = fallbackBmp
                binding.btnCapture.isEnabled = true
                val intent = Intent(this, ConfirmPhotoActivity::class.java)
                startActivity(intent)
            }
        }
    }

    companion object {
        private const val TAG = "CameraActivity"
        const val EXTRA_CATEGORY = "extra_scan_category"
    }
}
