    package com.example.healthcareai.ui.heartrate.engine

import android.content.Context
import android.util.Size
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Manages the CameraX lifecycle, continuous flashlight torch, and high-frequency
 * ImageAnalysis stream (320x240, STRATEGY_KEEP_ONLY_LATEST) for pulse detection.
 */
class CameraFrameSource(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    interface FrameListener {
        fun onFrameAvailable(imageProxy: ImageProxy)
        fun onCameraReady(hasFlash: Boolean)
        fun onCameraError(exception: Exception)
    }

    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var cameraControl: CameraControl? = null
    private var cameraInfo: CameraInfo? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var isTorchEnabled = false

    fun start(listener: FrameListener) {
        start(null, listener)
    }

    @androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
    fun start(previewView: androidx.camera.view.PreviewView?, listener: FrameListener) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                if (!cameraProvider!!.hasCamera(cameraSelector)) {
                    listener.onCameraError(IllegalStateException("No back camera found on this device"))
                    return@addListener
                }

                // Low resolution ~320x240 for ultra-fast YUV memory access
                val analysisBuilder = ImageAnalysis.Builder()
                    .setTargetResolution(Size(320, 240))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)

                // Camera2 settings: prioritize frame rate stability & lock focus
                val camera2Extender = Camera2Interop.Extender(analysisBuilder)
                camera2Extender.setCaptureRequestOption(
                    android.hardware.camera2.CaptureRequest.CONTROL_MODE,
                    android.hardware.camera2.CaptureRequest.CONTROL_MODE_AUTO
                )
                camera2Extender.setCaptureRequestOption(
                    android.hardware.camera2.CaptureRequest.CONTROL_AF_MODE,
                    android.hardware.camera2.CaptureRequest.CONTROL_AF_MODE_OFF
                )
                camera2Extender.setCaptureRequestOption(
                    android.hardware.camera2.CaptureRequest.LENS_FOCUS_DISTANCE,
                    0.0f
                )
                camera2Extender.setCaptureRequestOption(
                    android.hardware.camera2.CaptureRequest.CONTROL_AE_ANTIBANDING_MODE,
                    android.hardware.camera2.CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_AUTO
                )

                imageAnalysis = analysisBuilder.build()
                imageAnalysis?.setAnalyzer(cameraExecutor) { imageProxy ->
                    try {
                        listener.onFrameAvailable(imageProxy)
                    } finally {
                        imageProxy.close()
                    }
                }

                val preview = previewView?.let { pv ->
                    androidx.camera.core.Preview.Builder()
                        .setTargetResolution(Size(320, 240))
                        .build().also {
                            it.setSurfaceProvider(pv.surfaceProvider)
                        }
                }

                cameraProvider?.unbindAll()
                camera = if (preview != null) {
                    cameraProvider?.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        imageAnalysis,
                        preview
                    )
                } else {
                    cameraProvider?.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        imageAnalysis
                    )
                }

                cameraControl = camera?.cameraControl
                cameraInfo = camera?.cameraInfo

                val hasFlash = cameraInfo?.hasFlashUnit() ?: false
                if (hasFlash) {
                    setTorch(true)
                }

                listener.onCameraReady(hasFlash)

            } catch (e: Exception) {
                listener.onCameraError(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Toggles camera flashlight torch.
     */
    fun setTorch(enable: Boolean) {
        if (cameraInfo?.hasFlashUnit() == true) {
            cameraControl?.enableTorch(enable)
            isTorchEnabled = enable
        }
    }

    /**
     * Stops camera stream and shuts down executor.
     */
    fun stop() {
        try {
            setTorch(false)
            imageAnalysis?.clearAnalyzer()
            cameraProvider?.unbindAll()
            camera = null
            cameraControl = null
            cameraInfo = null
        } catch (e: Exception) {
            // Ignore clean teardown errors
        }
    }

    /**
     * Releases executor completely when activity is destroyed.
     */
    fun destroy() {
        stop()
        if (!cameraExecutor.isShutdown) {
            cameraExecutor.shutdown()
        }
    }
}
