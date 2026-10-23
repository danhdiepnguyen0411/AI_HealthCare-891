package com.example.healthcareai.ui.heartrate.engine

import com.example.healthcareai.data.model.TimestampedSample
import com.example.healthcareai.ui.heartrate.dsp.ButterworthFilter
import kotlin.math.max
import kotlin.math.min

/**
 * Handles buffering, uniform resampling, baseline detrending, and Butterworth band-pass
 * filtering of incoming PPG camera frame samples.
 *
 * @param windowDurationSec Maximum history length to retain in the sliding buffer (default 10s).
 * @param targetSampleRate Uniform sample rate (Hz) for resampling and filtering (default 30.0 Hz).
 * @param lowCutoffHz Lower cutoff frequency for band-pass filter (default 0.7 Hz = 42 BPM).
 * @param highCutoffHz Upper cutoff frequency for band-pass filter (default 4.0 Hz = 240 BPM).
 */
class SignalProcessor(
    private val windowDurationSec: Double = 10.0,
    val targetSampleRate: Double = 30.0,
    lowCutoffHz: Double = 0.7,
    highCutoffHz: Double = 4.0
) {
    private val windowDurationNs: Long = (windowDurationSec * 1_000_000_000L).toLong()

    /** Sliding window buffer of raw timestamped samples. */
    private val sampleBuffer = mutableListOf<TimestampedSample>()

    /** Butterworth band-pass filter tuned to target sample rate. */
    private val filter = ButterworthFilter(
        lowCutoffHz = lowCutoffHz,
        highCutoffHz = highCutoffHz,
        sampleRateHz = targetSampleRate
    )

    /**
     * Appends a new timestamped sample (green channel average value) to the buffer.
     * Automatically trims samples that fall outside the sliding window.
     */
    @Synchronized
    fun addSample(timestampNs: Long, value: Double) {
        sampleBuffer.add(TimestampedSample(timestampNs, value))

        // Trim old samples beyond windowDurationNs
        val cutoffTimestamp = timestampNs - windowDurationNs
        while (sampleBuffer.isNotEmpty() && sampleBuffer.first().timestampNs < cutoffTimestamp) {
            sampleBuffer.removeAt(0)
        }
    }

    /**
     * Clears all buffered samples and resets filter state (e.g. when finger is lifted).
     */
    @Synchronized
    fun reset() {
        sampleBuffer.clear()
        filter.reset()
    }

    /**
     * Returns the current duration of data accumulated in the buffer (in seconds).
     */
    @Synchronized
    fun getBufferedDurationSec(): Double {
        if (sampleBuffer.size < 2) return 0.0
        val durationNs = sampleBuffer.last().timestampNs - sampleBuffer.first().timestampNs
        return max(0.0, durationNs / 1_000_000_000.0)
    }

    /**
     * Returns the number of raw samples currently in the buffer.
     */
    @Synchronized
    fun getSampleCount(): Int = sampleBuffer.size

    /**
     * Processes the current buffer:
     * 1. Copies timestamped samples.
     * 2. Resamples non-uniform timestamps to uniform grid at [targetSampleRate].
     * 3. Detrends (removes linear DC drift).
     * 4. Applies Butterworth band-pass filter (0.7 - 4.0 Hz).
     *
     * @return [ProcessedSignal] containing uniform signal, timestamps, and normalized points for UI.
     */
    @Synchronized
    fun processCurrentWindow(): ProcessedSignal? {
        if (sampleBuffer.size < 10) return null

        val startNs = sampleBuffer.first().timestampNs
        val endNs = sampleBuffer.last().timestampNs
        val durationSec = (endNs - startNs) / 1_000_000_000.0
        if (durationSec < 1.0) return null // Need at least 1s of data

        // Exclude initial 500ms contact transient if we have accumulated at least 2.0s of data
        val effectiveStartNs = if (durationSec >= 2.0) {
            startNs + 500_000_000L
        } else {
            startNs
        }
        val effectiveDurationSec = (endNs - effectiveStartNs) / 1_000_000_000.0
        if (effectiveDurationSec < 1.0) return null

        // 1. Resample to uniform time series at targetSampleRate
        val stepSec = 1.0 / targetSampleRate
        val totalPoints = (effectiveDurationSec * targetSampleRate).toInt()
        if (totalPoints < 8) return null

        val uniformTimestampsNs = LongArray(totalPoints)
        val uniformValues = DoubleArray(totalPoints)

        var srcIdx = 0
        for (i in 0 until totalPoints) {
            val targetTimeNs = effectiveStartNs + (i * stepSec * 1_000_000_000L).toLong()
            uniformTimestampsNs[i] = targetTimeNs

            // Linear interpolation
            while (srcIdx < sampleBuffer.size - 2 && sampleBuffer[srcIdx + 1].timestampNs < targetTimeNs) {
                srcIdx++
            }

            val p1 = sampleBuffer[srcIdx]
            val p2 = if (srcIdx + 1 < sampleBuffer.size) sampleBuffer[srcIdx + 1] else p1

            val tDiff = (p2.timestampNs - p1.timestampNs).toDouble()
            if (tDiff > 0) {
                val ratio = (targetTimeNs - p1.timestampNs) / tDiff
                uniformValues[i] = p1.value + ratio * (p2.value - p1.value)
            } else {
                uniformValues[i] = p1.value
            }
        }

        // 2. Linear detrending (subtract best-fit line to eliminate DC drift)
        detrendInPlace(uniformValues)

        // 3. Apply Butterworth band-pass filter (0.7 Hz - 4.0 Hz)
        val filtered = filter.filterSignal(uniformValues)

        // 4. Generate normalized values (-1.0 to +1.0) for real-time waveform UI rendering
        val normalizedForUi = normalizeForUi(filtered)

        return ProcessedSignal(
            filteredSignal = filtered,
            timestampsNs = uniformTimestampsNs,
            sampleRateHz = targetSampleRate,
            uiWaveform = normalizedForUi
        )
    }

    /**
     * Linear regression detrending in-place.
     */
    private fun detrendInPlace(data: DoubleArray) {
        val n = data.size
        if (n < 2) return

        var sumX = 0.0
        var sumY = 0.0
        var sumXY = 0.0
        var sumX2 = 0.0

        for (i in 0 until n) {
            val x = i.toDouble()
            val y = data[i]
            sumX += x
            sumY += y
            sumXY += x * y
            sumX2 += x * x
        }

        val denominator = n * sumX2 - sumX * sumX
        if (abs(denominator) < 1e-12) return

        val slope = (n * sumXY - sumX * sumY) / denominator
        val intercept = (sumY - slope * sumX) / n

        for (i in 0 until n) {
            data[i] = data[i] - (slope * i + intercept)
        }
    }

    private fun abs(v: Double): Double = kotlin.math.abs(v)

    /**
     * Normalizes signal to range [-1.0, 1.0] with clipping to prevent erratic spikes in UI.
     */
    private fun normalizeForUi(signal: DoubleArray): FloatArray {
        if (signal.isEmpty()) return FloatArray(0)

        var minVal = Double.MAX_VALUE
        var maxVal = -Double.MAX_VALUE
        for (v in signal) {
            if (v < minVal) minVal = v
            if (v > maxVal) maxVal = v
        }

        val range = maxVal - minVal
        val result = FloatArray(signal.size)
        if (range < 1e-6) {
            return result
        }

        for (i in signal.indices) {
            // Map [minVal, maxVal] -> [-1.0, 1.0]
            val normalized = ((signal[i] - minVal) / range * 2.0 - 1.0).toFloat()
            result[i] = min(1.0f, max(-1.0f, normalized))
        }
        return result
    }
}

/**
 * Clean bundle of processed signal data ready for peak detection and FFT estimation.
 */
data class ProcessedSignal(
    val filteredSignal: DoubleArray,
    val timestampsNs: LongArray,
    val sampleRateHz: Double,
    val uiWaveform: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProcessedSignal) return false
        return filteredSignal.contentEquals(other.filteredSignal) &&
                timestampsNs.contentEquals(other.timestampsNs) &&
                sampleRateHz == other.sampleRateHz &&
                uiWaveform.contentEquals(other.uiWaveform)
    }

    override fun hashCode(): Int {
        var result = filteredSignal.contentHashCode()
        result = 31 * result + timestampsNs.contentHashCode()
        result = 31 * result + sampleRateHz.hashCode()
        result = 31 * result + uiWaveform.contentHashCode()
        return result
    }
}
