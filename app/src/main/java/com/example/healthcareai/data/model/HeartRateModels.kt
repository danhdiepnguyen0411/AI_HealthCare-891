package com.example.healthcareai.data.model

/**
 * All domain models for the Heart Rate measurement feature.
 */

/** Represents the current state of a heart rate measurement session. */
sealed class MeasureState {
    /** Camera not started, waiting for user action. */
    data object Idle : MeasureState()

    /** Camera is running; waiting for finger to cover the lens. */
    data object WaitingForFinger : MeasureState()

    /** Actively measuring. */
    data class Measuring(
        /** Progress 0.0‥1.0 */
        val progress: Float,
        /** Latest filtered signal values for waveform drawing. */
        val liveSignal: FloatArray,
        /** Preliminary BPM estimate (may be 0 if not yet available). */
        val preliminaryBpm: Int = 0
    ) : MeasureState() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Measuring) return false
            return progress == other.progress &&
                    liveSignal.contentEquals(other.liveSignal) &&
                    preliminaryBpm == other.preliminaryBpm
        }
        override fun hashCode(): Int {
            var result = progress.hashCode()
            result = 31 * result + liveSignal.contentHashCode()
            result = 31 * result + preliminaryBpm
            return result
        }
    }

    /** Measurement finished successfully. */
    data class Success(
        val bpm: Int,
        val hrv: HrvResult?,
        val quality: SignalQuality
    ) : MeasureState()

    /** Measurement failed or was cancelled. */
    data class Error(val reason: MeasureError) : MeasureState()
}

/** Reason for measurement failure. */
enum class MeasureError {
    FINGER_LOST,
    LOW_QUALITY_SIGNAL,
    NO_FLASH_AVAILABLE,
    CAMERA_ERROR,
    PERMISSION_DENIED,
    CANCELLED
}

/** Signal quality classification based on SQI score. */
enum class SignalQuality(val label: String) {
    EXCELLENT("Excellent"),
    GOOD("Good"),
    FAIR("Fair"),
    POOR("Poor")
}

/** Heart Rate Variability metrics. */
data class HrvResult(
    /** Root Mean Square of Successive Differences (ms) */
    val rmssd: Double,
    /** Standard Deviation of NN intervals (ms) */
    val sdnn: Double,
    /** Number of valid inter-beat intervals used */
    val validIbiCount: Int
)

/** Result from BPM estimation algorithms. */
data class BpmResult(
    /** Final BPM (weighted average of peak & FFT methods). */
    val bpm: Int,
    /** BPM from peak-detection method. */
    val peakBpm: Double,
    /** BPM from FFT/Welch method. */
    val fftBpm: Double,
    /** Signal Quality Index 0.0‥1.0 */
    val sqi: Double,
    /** HRV metrics, null if insufficient valid IBIs. */
    val hrv: HrvResult?,
    /** Signal quality classification. */
    val quality: SignalQuality
)

/** A single timestamped sample from the camera frame analysis. */
data class TimestampedSample(
    /** System.nanoTime() of the frame. */
    val timestampNs: Long,
    /** Filtered or raw signal value (typically green channel mean). */
    val value: Double
)

/** RGB mean values extracted from a camera frame. */
data class FrameColorMeans(
    val redMean: Double,
    val greenMean: Double,
    val blueMean: Double,
    val brightness: Double,
    val timestampNs: Long
)
