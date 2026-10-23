package com.example.healthcareai.ui.heartrate.engine

import androidx.camera.core.ImageProxy
import com.example.healthcareai.data.model.FrameColorMeans
import java.nio.ByteBuffer

/**
 * Fast, zero-allocation finger detector that extracts RGB averages directly from
 * CameraX YUV_420_888 ImageProxy planes without Bitmap conversions.
 *
 * Blood-perfused fingertip under phone flash produces characteristic optical properties:
 * - High Red absorption/transmission contrast (Red >> Green and Red >> Blue).
 * - High overall brightness from flashlight back-scattering.
 *
 * Features asymmetric debouncing: detects finger contact quickly (2 frames ~60ms)
 * while requiring sustained absence (12 frames ~400ms) before declaring finger lost.
 * This prevents single-frame lighting or auto-exposure glitches from restarting measurements.
 */
class FingerDetector(
    /** Minimum average red level to consider finger present (0-255). */
    private val minRedThreshold: Double = 50.0,
    /** Minimum ratio of Red to Green (R / G) indicating vascular tissue transillumination. */
    private val minRedGreenRatio: Double = 1.10,
    /** Minimum ratio of Red to Blue (R / B). */
    private val minRedBlueRatio: Double = 1.15,
    /** Number of consecutive matching frames required to detect finger. */
    private val detectDebounceFrames: Int = 2,
    /** Number of consecutive lost frames required to declare finger lost (~400ms). */
    private val lostDebounceFrames: Int = 12
) {
    private var consecutiveDetectedCount = 0
    private var consecutiveLostCount = 0
    private var isFingerCurrentlyCovering = false

    /**
     * Resets the detection debounce state.
     */
    fun reset() {
        consecutiveDetectedCount = 0
        consecutiveLostCount = 0
        isFingerCurrentlyCovering = false
    }

    /**
     * Analyzes an [ImageProxy] frame in the central 35% ROI.
     * Computes R, G, B channel averages and evaluates finger placement.
     *
     * @param image CameraX ImageProxy in YUV_420_888 format.
     * @return Pair of (isFingerPresent: Boolean, frameMeans: FrameColorMeans).
     */
    fun analyzeFrame(image: ImageProxy): Pair<Boolean, FrameColorMeans> {
        val timestampNs = System.nanoTime()

        val planes = image.planes
        if (planes.size < 3) {
            return Pair(false, FrameColorMeans(0.0, 0.0, 0.0, 0.0, timestampNs))
        }

        val yPlane = planes[0]
        val uPlane = planes[1]
        val vPlane = planes[2]

        val yBuffer: ByteBuffer = yPlane.buffer
        val uBuffer: ByteBuffer = uPlane.buffer
        val vBuffer: ByteBuffer = vPlane.buffer

        val width = image.width
        val height = image.height

        val yRowStride = yPlane.rowStride
        val yPixelStride = yPlane.pixelStride
        val uRowStride = uPlane.rowStride
        val uPixelStride = uPlane.pixelStride
        val vRowStride = vPlane.rowStride
        val vPixelStride = vPlane.pixelStride

        // Central ROI: 35% to 65% of frame dimensions
        val roiStartX = (width * 0.35).toInt()
        val roiEndX = (width * 0.65).toInt()
        val roiStartY = (height * 0.35).toInt()
        val roiEndY = (height * 0.65).toInt()

        // Step by 2 pixels in X and Y for ultra-fast processing (< 1ms per frame)
        val step = 2

        var sumR = 0.0
        var sumG = 0.0
        var sumB = 0.0
        var sumY = 0.0
        var sampleCount = 0

        for (y in roiStartY until roiEndY step step) {
            val yOffset = y * yRowStride
            val uvY = y / 2
            val uOffset = uvY * uRowStride
            val vOffset = uvY * vRowStride

            for (x in roiStartX until roiEndX step step) {
                val yIndex = yOffset + x * yPixelStride
                val uvX = x / 2
                val uIndex = uOffset + uvX * uPixelStride
                val vIndex = vOffset + uvX * vPixelStride

                if (yIndex >= yBuffer.limit() || uIndex >= uBuffer.limit() || vIndex >= vBuffer.limit()) {
                    continue
                }

                val yVal = yBuffer.get(yIndex).toInt() and 0xFF
                val uVal = (uBuffer.get(uIndex).toInt() and 0xFF) - 128
                val vVal = (vBuffer.get(vIndex).toInt() and 0xFF) - 128

                // ITU-R BT.601 YUV to RGB standard conversion
                val r = (yVal + 1.402 * vVal).toInt().coerceIn(0, 255)
                val g = (yVal - 0.344136 * uVal - 0.714136 * vVal).toInt().coerceIn(0, 255)
                val b = (yVal + 1.772 * uVal).toInt().coerceIn(0, 255)

                sumR += r
                sumG += g
                sumB += b
                sumY += yVal
                sampleCount++
            }
        }

        if (sampleCount == 0) {
            return Pair(false, FrameColorMeans(0.0, 0.0, 0.0, 0.0, timestampNs))
        }

        val avgR = sumR / sampleCount
        val avgG = sumG / sampleCount
        val avgB = sumB / sampleCount
        val avgY = sumY / sampleCount

        val colorMeans = FrameColorMeans(
            redMean = avgR,
            greenMean = avgG,
            blueMean = avgB,
            brightness = avgY,
            timestampNs = timestampNs
        )

        // Validate finger placement criteria with robust thresholds
        val isRedDominant = avgR >= minRedThreshold &&
                avgR > (avgG * minRedGreenRatio) &&
                avgR > (avgB * minRedBlueRatio)

        // Asymmetric debounce to prevent momentary frame flickers from resetting measurement
        if (isRedDominant) {
            consecutiveDetectedCount++
            consecutiveLostCount = 0
            if (consecutiveDetectedCount >= detectDebounceFrames) {
                isFingerCurrentlyCovering = true
            }
        } else {
            consecutiveLostCount++
            consecutiveDetectedCount = 0
            if (consecutiveLostCount >= lostDebounceFrames) {
                isFingerCurrentlyCovering = false
            }
        }

        return Pair(isFingerCurrentlyCovering, colorMeans)
    }
}
