package com.example.healthcareai.ui.heartrate.dsp

import kotlin.math.*

/**
 * Second-order Butterworth band-pass filter implemented as a cascade of
 * two biquad sections (one high-pass, one low-pass).
 *
 * Pure Kotlin implementation — no external DSP library required.
 *
 * Usage:
 * ```
 * val bpf = ButterworthFilter.bandPass(sampleRate = 30.0, lowCut = 0.7, highCut = 4.0)
 * val filtered = bpf.filter(rawSignal)
 * ```
 *
 * @param sections ordered list of [BiquadSection]s applied in cascade.
 */
class ButterworthFilter(
    private val sections: List<BiquadSection>
) {
    constructor(lowCutoffHz: Double, highCutoffHz: Double, sampleRateHz: Double) : this(
        listOf(
            butterworthHighPass(sampleRateHz, lowCutoffHz),
            butterworthLowPass(sampleRateHz, highCutoffHz)
        )
    )

    fun filterSignal(input: DoubleArray): DoubleArray = filterZeroPhase(input)

    /**
     * Filters an entire array of samples through the cascade.
     * Always resets internal state before filtering to ensure idempotent batch processing.
     *
     * @param input raw signal array.
     * @return new array with the band-pass filtered signal.
     */
    fun filter(input: DoubleArray): DoubleArray {
        reset()
        val output = input.copyOf()
        for (section in sections) {
            for (i in output.indices) {
                output[i] = section.process(output[i])
            }
        }
        return output
    }

    /**
     * Zero-phase forward-backward filtering (equivalent to filtfilt).
     * Eliminates filter phase distortion so waveform peaks align precisely in time.
     */
    fun filterZeroPhase(input: DoubleArray): DoubleArray {
        if (input.size < 6) return filter(input)

        // Forward pass
        reset()
        val forward = input.copyOf()
        for (section in sections) {
            for (i in forward.indices) {
                forward[i] = section.process(forward[i])
            }
        }

        // Backward pass
        reset()
        for (section in sections) {
            for (i in forward.indices.reversed()) {
                forward[i] = section.process(forward[i])
            }
        }

        return forward
    }

    /**
     * Filters a single sample (for real-time per-frame usage).
     */
    fun filterSample(sample: Double): Double {
        var x = sample
        for (section in sections) {
            x = section.process(x)
        }
        return x
    }

    /** Resets all internal filter state (delay buffers). */
    fun reset() {
        sections.forEach { it.reset() }
    }

    companion object {
        /**
         * Creates a 2nd-order Butterworth band-pass filter.
         *
         * Internally builds a high-pass biquad at [lowCut] Hz and a
         * low-pass biquad at [highCut] Hz, both 2nd-order Butterworth.
         *
         * @param sampleRate sampling frequency in Hz.
         * @param lowCut lower cutoff frequency in Hz (high-pass).
         * @param highCut upper cutoff frequency in Hz (low-pass).
         * @return configured [ButterworthFilter].
         */
        fun bandPass(sampleRate: Double, lowCut: Double, highCut: Double): ButterworthFilter {
            require(lowCut > 0 && highCut > lowCut && highCut < sampleRate / 2) {
                "Invalid cutoff frequencies: lowCut=$lowCut, highCut=$highCut, fs=$sampleRate"
            }
            val hp = butterworthHighPass(sampleRate, lowCut)
            val lp = butterworthLowPass(sampleRate, highCut)
            return ButterworthFilter(listOf(hp, lp))
        }

        /**
         * 2nd-order Butterworth low-pass as a single biquad section.
         *
         * Transfer function derived via bilinear transform of the analog
         * 2nd-order Butterworth prototype H(s) = 1 / (s² + √2·s + 1).
         */
        private fun butterworthLowPass(fs: Double, fc: Double): BiquadSection {
            val wc = tan(PI * fc / fs)
            val wc2 = wc * wc
            val sqrt2 = sqrt(2.0)
            val norm = 1.0 / (1.0 + sqrt2 * wc + wc2)

            val b0 = wc2 * norm
            val b1 = 2.0 * b0
            val b2 = b0
            val a1 = 2.0 * (wc2 - 1.0) * norm
            val a2 = (1.0 - sqrt2 * wc + wc2) * norm

            return BiquadSection(b0, b1, b2, a1, a2)
        }

        /**
         * 2nd-order Butterworth high-pass as a single biquad section.
         *
         * Derived via bilinear transform with frequency pre-warping.
         */
        private fun butterworthHighPass(fs: Double, fc: Double): BiquadSection {
            val wc = tan(PI * fc / fs)
            val wc2 = wc * wc
            val sqrt2 = sqrt(2.0)
            val norm = 1.0 / (1.0 + sqrt2 * wc + wc2)

            val b0 = norm
            val b1 = -2.0 * norm
            val b2 = norm
            val a1 = 2.0 * (wc2 - 1.0) * norm
            val a2 = (1.0 - sqrt2 * wc + wc2) * norm

            return BiquadSection(b0, b1, b2, a1, a2)
        }
    }
}

/**
 * A single Direct-Form II Transposed biquad section.
 *
 * Implements the difference equation:
 *   y[n] = b0·x[n] + b1·x[n-1] + b2·x[n-2] - a1·y[n-1] - a2·y[n-2]
 *
 * Uses transposed form for better numerical stability.
 */
class BiquadSection(
    private val b0: Double,
    private val b1: Double,
    private val b2: Double,
    private val a1: Double,
    private val a2: Double
) {
    private var z1 = 0.0
    private var z2 = 0.0

    /**
     * Processes a single input sample and returns the filtered output.
     * Updates internal delay state.
     */
    fun process(input: Double): Double {
        val output = b0 * input + z1
        z1 = b1 * input - a1 * output + z2
        z2 = b2 * input - a2 * output
        return output
    }

    /** Clears the delay buffers. */
    fun reset() {
        z1 = 0.0
        z2 = 0.0
    }
}
