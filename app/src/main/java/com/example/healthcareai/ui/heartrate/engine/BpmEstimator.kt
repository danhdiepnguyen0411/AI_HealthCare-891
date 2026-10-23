package com.example.healthcareai.ui.heartrate.engine

import com.example.healthcareai.data.model.BpmResult
import com.example.healthcareai.data.model.HrvResult
import com.example.healthcareai.data.model.SignalQuality
import com.example.healthcareai.ui.heartrate.dsp.FFT
import com.example.healthcareai.ui.heartrate.dsp.PeakDetector
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Combines Time-Domain (Peak Detection) and Frequency-Domain (FFT Power Spectral Density)
 * to estimate heart rate (BPM), signal quality index (SQI), and HRV metrics (SDNN, RMSSD).
 */
class BpmEstimator(
    private val peakDetector: PeakDetector = PeakDetector(minBpm = 42.0, maxBpm = 220.0),
    private val minValidBpm: Double = 42.0,
    private val maxValidBpm: Double = 220.0
) {
    /**
     * Estimates BPM, SQI, and HRV from processed PPG signal.
     */
    fun estimate(signal: ProcessedSignal): BpmResult? {
        val filtered = signal.filteredSignal
        val timestampsNs = signal.timestampsNs
        val sampleRate = signal.sampleRateHz

        if (filtered.size < 30) return null

        // 1. Time-Domain: Peak Detection
        val peakResult = peakDetector.findPeaks(filtered, timestampsNs)
        val validIbis = peakResult.validIbis
        val peakBpm = if (validIbis.isNotEmpty()) peakDetector.estimateBpm(validIbis) else 0.0

        // 2. Frequency-Domain: FFT Dominant Frequency
        val dominantFreqHz = FFT.dominantFrequency(
            signal = filtered,
            sampleRate = sampleRate,
            lowHz = 0.70, // 42 BPM
            highHz = 3.67  // 220 BPM
        )
        val fftBpm = dominantFreqHz * 60.0

        // 3. Compute Signal Quality Index (SQI)
        val sqi = calculateSqi(filtered, sampleRate, dominantFreqHz, peakBpm, fftBpm, validIbis)
        val quality = when {
            sqi >= 0.65 -> SignalQuality.EXCELLENT
            sqi >= 0.45 -> SignalQuality.GOOD
            sqi >= 0.20 -> SignalQuality.FAIR
            else -> SignalQuality.POOR
        }

        // 4. Fusion of Peak & FFT estimates
        val finalBpmDouble = fuseBpm(peakBpm, fftBpm, validIbis.size, sqi)
        if (finalBpmDouble < minValidBpm || finalBpmDouble > maxValidBpm) {
            return null
        }

        // 5. HRV (Heart Rate Variability) computation
        val hrv = computeHrv(validIbis)

        return BpmResult(
            bpm = finalBpmDouble.roundToInt(),
            peakBpm = peakBpm,
            fftBpm = fftBpm,
            sqi = sqi,
            hrv = hrv,
            quality = quality
        )
    }

    /**
     * Intelligently fuses time-domain (Peak) and frequency-domain (FFT) estimates.
     */
    private fun fuseBpm(
        peakBpm: Double,
        fftBpm: Double,
        validIbiCount: Int,
        sqi: Double
    ): Double {
        val peakValid = peakBpm in minValidBpm..maxValidBpm
        val fftValid = fftBpm in minValidBpm..maxValidBpm

        return when {
            peakValid && fftValid -> {
                val bpmDiff = abs(peakBpm - fftBpm)
                if (bpmDiff <= 18.0) {
                    0.60 * peakBpm + 0.40 * fftBpm
                } else if (validIbiCount >= 4 && sqi >= 0.40) {
                    peakBpm
                } else {
                    fftBpm
                }
            }
            peakValid -> peakBpm
            fftValid -> fftBpm
            else -> 0.0
        }
    }

    /**
     * Calculates Signal Quality Index (SQI) in range [0.0, 1.0] by evaluating:
     * 1. Agreement between Peak-detection BPM and FFT BPM.
     * 2. Spectral energy concentration around the dominant cardiac peak vs background noise.
     * 3. Consistency of inter-beat intervals.
     */
    private fun calculateSqi(
        signal: DoubleArray,
        sampleRate: Double,
        dominantFreqHz: Double,
        peakBpm: Double,
        fftBpm: Double,
        validIbis: DoubleArray
    ): Double {
        var score = 0.0

        // Factor 1: Agreement between Peak and FFT (up to 0.40 score)
        if (peakBpm > 0 && fftBpm > 0) {
            val diff = abs(peakBpm - fftBpm)
            val agreementScore = max(0.0, 1.0 - diff / 25.0)
            score += agreementScore * 0.40
        }

        // Factor 2: Spectral energy ratio around cardiac peak (up to 0.35 score)
        if (dominantFreqHz > 0.6) {
            val energyScore = calculateSpectralEnergyRatio(signal, sampleRate, dominantFreqHz)
            score += energyScore * 0.35
        }

        // Factor 3: IBI consistency (low coefficient of variation of IBIs, up to 0.25 score)
        if (validIbis.size >= 3) {
            val ibiMean = validIbis.average()
            var ibiVar = 0.0
            for (v in validIbis) {
                val d = v - ibiMean
                ibiVar += d * d
            }
            val ibiStd = sqrt(ibiVar / validIbis.size)
            val cv = if (ibiMean > 0) ibiStd / ibiMean else 1.0
            val consistencyScore = max(0.0, 1.0 - cv * 3.0)
            score += consistencyScore * 0.25
        }

        return score.coerceIn(0.0, 1.0)
    }

    private fun calculateSpectralEnergyRatio(
        signal: DoubleArray,
        sampleRate: Double,
        dominantFreqHz: Double
    ): Double {
        val segLen = min(signal.size, 128)
        if (segLen < 16) return 0.5
        val (psd, freqs) = FFT.welch(signal, sampleRate, segmentLength = segLen)
        if (freqs.isEmpty() || psd.isEmpty()) return 0.5

        val halfBand = 0.20 // +/- 0.20 Hz around fundamental peak
        var cardiacPeakEnergy = 0.0
        var totalBandEnergy = 0.0

        for (i in freqs.indices) {
            val f = freqs[i]
            if (f in 0.7..4.0) {
                val p = psd[i]
                totalBandEnergy += p
                if (abs(f - dominantFreqHz) <= halfBand || abs(f - 2 * dominantFreqHz) <= halfBand) {
                    cardiacPeakEnergy += p
                }
            }
        }

        return if (totalBandEnergy > 1e-12) {
            (cardiacPeakEnergy / totalBandEnergy).coerceIn(0.0, 1.0)
        } else {
            0.0
        }
    }

    /**
     * Computes HRV metrics (SDNN, RMSSD in milliseconds).
     */
    private fun computeHrv(validIbis: DoubleArray): HrvResult? {
        if (validIbis.size < 4) return null

        // SDNN: Standard deviation of NN intervals
        val meanIbi = validIbis.average()
        var sumSquaredDev = 0.0
        for (ibi in validIbis) {
            val d = ibi - meanIbi
            sumSquaredDev += d * d
        }
        val sdnn = sqrt(sumSquaredDev / (validIbis.size - 1))

        // RMSSD: Root mean square of successive differences
        var sumSquaredDiff = 0.0
        val diffCount = validIbis.size - 1
        for (i in 0 until diffCount) {
            val diff = validIbis[i + 1] - validIbis[i]
            sumSquaredDiff += diff * diff
        }
        val rmssd = sqrt(sumSquaredDiff / diffCount)

        return HrvResult(
            rmssd = rmssd,
            sdnn = sdnn,
            validIbiCount = validIbis.size
        )
    }
}
