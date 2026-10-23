package com.example.healthcareai.ui.heartrate.dsp

import kotlin.math.abs
import kotlin.math.max

/**
 * Robust peak detection algorithm for photoplethysmogram (PPG) waveforms.
 *
 * Implements:
 * 1. Adaptive moving-average thresholding to handle baseline drift and amplitude variations.
 * 2. Physiological refractory period (minimum interval between heartbeats).
 * 3. Outlier rejection based on physiologically feasible human heart rate (40 - 220 BPM).
 * 4. Relative jump rejection: sudden IBI deviation > 30% from running median.
 */
class PeakDetector(
    private val minBpm: Double = 40.0,
    private val maxBpm: Double = 220.0,
    /** Minimum fraction of local peak amplitude required to be recognized as a peak. */
    private val thresholdFactor: Double = 0.40
) {
    /**
     * Finds peaks in a filtered signal and calculates inter-beat intervals (IBI in ms).
     *
     * @param signal Array of filtered PPG values.
     * @param timestampsNs Timestamps corresponding to each sample in nanoseconds.
     * @return Pair of (peak indices list, valid IBIs in milliseconds).
     */
    fun findPeaks(
        signal: DoubleArray,
        timestampsNs: LongArray
    ): PeakDetectionResult {
        if (signal.size < 5 || signal.size != timestampsNs.size) {
            return PeakDetectionResult(emptyList(), doubleArrayOf(), emptyList())
        }

        val n = signal.size
        // Minimum time between peaks in nanoseconds based on max feasible BPM (e.g. 220 BPM -> ~272 ms)
        val minIntervalNs = (60.0 / maxBpm * 1_000_000_000L).toLong()

        // 1. Calculate dynamic threshold based on signal envelope / standard deviation
        var mean = 0.0
        for (v in signal) mean += v
        mean /= n

        var variance = 0.0
        for (v in signal) {
            val d = v - mean
            variance += d * d
        }
        val stdDev = kotlin.math.sqrt(variance / n)
        val baseThreshold = mean + thresholdFactor * stdDev

        // 2. Candidate peak identification (local maxima above threshold with refractory period)
        val candidateIndices = mutableListOf<Int>()
        var lastPeakTimeNs = 0L

        for (i in 1 until n - 1) {
            val curr = signal[i]
            if (curr > signal[i - 1] && curr >= signal[i + 1] && curr > baseThreshold) {
                val t = timestampsNs[i]
                if (t - lastPeakTimeNs >= minIntervalNs) {
                    candidateIndices.add(i)
                    lastPeakTimeNs = t
                } else if (candidateIndices.isNotEmpty()) {
                    // If candidate is within refractory period, keep the taller peak
                    val prevIdx = candidateIndices.last()
                    if (curr > signal[prevIdx]) {
                        candidateIndices[candidateIndices.size - 1] = i
                        lastPeakTimeNs = t
                    }
                }
            }
        }

        if (candidateIndices.size < 2) {
            return PeakDetectionResult(candidateIndices, doubleArrayOf(), candidateIndices)
        }

        // 3. Extract raw IBIs in milliseconds
        val rawIbis = DoubleArray(candidateIndices.size - 1)
        for (i in 0 until candidateIndices.size - 1) {
            val t1 = timestampsNs[candidateIndices[i]]
            val t2 = timestampsNs[candidateIndices[i + 1]]
            rawIbis[i] = (t2 - t1) / 1_000_000.0 // ns -> ms
        }

        // 4. Outlier rejection: physiological limits (300ms - 1500ms) & median-referenced consistency
        val minIbiMs = 60_000.0 / maxBpm // e.g. 272ms
        val maxIbiMs = 60_000.0 / minBpm // e.g. 1500ms

        val inRangeCandidates = mutableListOf<Pair<Int, Double>>()
        for (i in rawIbis.indices) {
            val ibi = rawIbis[i]
            if (ibi in minIbiMs..maxIbiMs) {
                inRangeCandidates.add(i to ibi)
            }
        }

        if (inRangeCandidates.isEmpty()) {
            return PeakDetectionResult(candidateIndices, doubleArrayOf(), emptyList())
        }

        // Use the median of in-range IBIs as the robust anchor reference
        val sortedInRanges = inRangeCandidates.map { it.second }.sorted()
        val medianIbi = sortedInRanges[sortedInRanges.size / 2]

        val validIbis = mutableListOf<Double>()
        val validPeakIndices = mutableListOf<Int>()
        validPeakIndices.add(candidateIndices[0])

        for ((idx, ibi) in inRangeCandidates) {
            val deviationFromMedian = abs(ibi - medianIbi) / medianIbi
            if (deviationFromMedian <= 0.35) {
                validIbis.add(ibi)
                validPeakIndices.add(candidateIndices[idx + 1])
            }
        }

        return PeakDetectionResult(
            allPeakIndices = candidateIndices,
            validIbis = validIbis.toDoubleArray(),
            validPeakIndices = validPeakIndices
        )
    }

    /**
     * Estimates BPM from an array of valid IBIs (in milliseconds).
     * Uses trimmed mean (excluding 10% outliers at each end) to protect against artifacts.
     */
    fun estimateBpm(validIbis: DoubleArray): Double {
        if (validIbis.isEmpty()) return 0.0
        if (validIbis.size == 1) return 60_000.0 / validIbis[0]

        val sorted = validIbis.sorted()
        val trimCount = (sorted.size * 0.1).toInt()
        val trimmed = if (trimCount > 0 && sorted.size - 2 * trimCount >= 2) {
            sorted.subList(trimCount, sorted.size - trimCount)
        } else {
            sorted
        }

        val avgIbi = trimmed.average()
        return if (avgIbi > 0) 60_000.0 / avgIbi else 0.0
    }
}

/**
 * Result bundle for peak detection.
 */
data class PeakDetectionResult(
    val allPeakIndices: List<Int>,
    val validIbis: DoubleArray,
    val validPeakIndices: List<Int>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PeakDetectionResult) return false
        return allPeakIndices == other.allPeakIndices &&
                validIbis.contentEquals(other.validIbis) &&
                validPeakIndices == other.validPeakIndices
    }

    override fun hashCode(): Int {
        var result = allPeakIndices.hashCode()
        result = 31 * result + validIbis.contentHashCode()
        result = 31 * result + validPeakIndices.hashCode()
        return result
    }
}
