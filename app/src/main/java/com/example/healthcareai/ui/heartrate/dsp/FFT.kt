package com.example.healthcareai.ui.heartrate.dsp

import kotlin.math.*

/**
 * Radix-2 Cooley-Tukey FFT — pure Kotlin, zero-allocation after init.
 *
 * Input length is automatically zero-padded to the next power of two.
 *
 * Usage:
 * ```
 * val (magnitudes, freqs) = FFT.magnitudeSpectrum(signal, sampleRate = 30.0)
 * ```
 */
object FFT {

    /**
     * Computes the magnitude spectrum of a real-valued signal.
     *
     * @param signal input signal (arbitrary length, will be zero-padded to power of 2).
     * @param sampleRate sampling frequency in Hz.
     * @return Pair of (magnitudes, frequencies) arrays — only positive half (0..fs/2).
     */
    fun magnitudeSpectrum(signal: DoubleArray, sampleRate: Double): Pair<DoubleArray, DoubleArray> {
        val n = nextPowerOf2(signal.size)
        val real = DoubleArray(n)
        val imag = DoubleArray(n)
        val nSig = signal.size

        // Apply Hann window to eliminate spectral leakage from zero-padding
        if (nSig > 1) {
            for (i in 0 until nSig) {
                val w = 0.5 * (1.0 - cos(2.0 * PI * i / (nSig - 1)))
                real[i] = signal[i] * w
            }
        } else {
            signal.copyInto(real, endIndex = minOf(nSig, n))
        }

        fftInPlace(real, imag, forward = true)

        val halfN = n / 2
        val magnitudes = DoubleArray(halfN)
        val frequencies = DoubleArray(halfN)
        val freqResolution = sampleRate / n

        for (i in 0 until halfN) {
            magnitudes[i] = sqrt(real[i] * real[i] + imag[i] * imag[i]) / n
            frequencies[i] = i * freqResolution
        }

        return magnitudes to frequencies
    }

    /**
     * Finds the dominant frequency within a specified band.
     *
     * @param signal input signal.
     * @param sampleRate sampling frequency in Hz.
     * @param lowHz lower bound of the frequency band.
     * @param highHz upper bound of the frequency band.
     * @return dominant frequency in Hz, or 0.0 if band is empty.
     */
    fun dominantFrequency(
        signal: DoubleArray,
        sampleRate: Double,
        lowHz: Double,
        highHz: Double
    ): Double {
        val (magnitudes, frequencies) = magnitudeSpectrum(signal, sampleRate)

        var maxMag = 0.0
        var maxFreq = 0.0
        for (i in frequencies.indices) {
            if (frequencies[i] in lowHz..highHz && magnitudes[i] > maxMag) {
                maxMag = magnitudes[i]
                maxFreq = frequencies[i]
            }
        }

        // Parabolic interpolation for sub-bin accuracy
        if (maxFreq > 0.0) {
            val idx = frequencies.indexOfFirst { it == maxFreq }
            if (idx > 0 && idx < magnitudes.size - 1) {
                val alpha = magnitudes[idx - 1]
                val beta = magnitudes[idx]
                val gamma = magnitudes[idx + 1]
                val denom = alpha - 2.0 * beta + gamma
                if (abs(denom) > 1e-10) {
                    val correction = 0.5 * (alpha - gamma) / denom
                    val freqRes = sampleRate / nextPowerOf2(signal.size)
                    val refined = maxFreq + correction * freqRes
                    maxFreq = refined.coerceIn(lowHz, highHz)
                }
            }
        }

        return maxFreq
    }

    /**
     * Welch's method: average power spectrum with overlapping segments.
     *
     * @param signal input signal.
     * @param sampleRate sampling frequency in Hz.
     * @param segmentLength length of each segment (samples).
     * @param overlap fraction of overlap (0.0..1.0), typically 0.5.
     * @return Pair of (power, frequencies) arrays.
     */
    fun welch(
        signal: DoubleArray,
        sampleRate: Double,
        segmentLength: Int,
        overlap: Double = 0.5
    ): Pair<DoubleArray, DoubleArray> {
        val step = maxOf(1, (segmentLength * (1.0 - overlap)).toInt())
        val nFft = nextPowerOf2(segmentLength)
        val halfN = nFft / 2
        val powerAccum = DoubleArray(halfN)
        var segmentCount = 0

        // Hann window
        val window = DoubleArray(segmentLength) { i ->
            0.5 * (1.0 - cos(2.0 * PI * i / (segmentLength - 1)))
        }
        val windowPower = window.sumOf { it * it } / segmentLength

        var start = 0
        while (start + segmentLength <= signal.size) {
            val real = DoubleArray(nFft)
            val imag = DoubleArray(nFft)
            for (i in 0 until segmentLength) {
                real[i] = signal[start + i] * window[i]
            }

            fftInPlace(real, imag, forward = true)

            for (i in 0 until halfN) {
                powerAccum[i] += (real[i] * real[i] + imag[i] * imag[i]) / (nFft.toDouble() * nFft)
            }
            segmentCount++
            start += step
        }

        if (segmentCount == 0) {
            return DoubleArray(halfN) to DoubleArray(halfN) { i ->
                i * sampleRate / nFft
            }
        }

        val freqRes = sampleRate / nFft
        val frequencies = DoubleArray(halfN) { it * freqRes }
        val power = DoubleArray(halfN) { powerAccum[it] / (segmentCount * windowPower) }

        return power to frequencies
    }

    /**
     * In-place radix-2 Cooley-Tukey FFT.
     *
     * @param real real part array (length must be power of 2).
     * @param imag imaginary part array (same length).
     * @param forward true for forward FFT, false for inverse.
     */
    internal fun fftInPlace(real: DoubleArray, imag: DoubleArray, forward: Boolean) {
        val n = real.size
        require(n > 0 && (n and (n - 1)) == 0) { "FFT size must be a power of 2, got $n" }

        // Bit-reversal permutation
        var j = 0
        for (i in 0 until n - 1) {
            if (i < j) {
                var temp = real[i]; real[i] = real[j]; real[j] = temp
                temp = imag[i]; imag[i] = imag[j]; imag[j] = temp
            }
            var m = n shr 1
            while (m >= 1 && j >= m) {
                j -= m
                m = m shr 1
            }
            j += m
        }

        // Butterfly stages
        val direction = if (forward) -1.0 else 1.0
        var mLen = 2
        while (mLen <= n) {
            val halfM = mLen / 2
            val wAngle = direction * 2.0 * PI / mLen

            for (k in 0 until n step mLen) {
                for (jj in 0 until halfM) {
                    val angle = wAngle * jj
                    val wr = cos(angle)
                    val wi = sin(angle)

                    val idx1 = k + jj
                    val idx2 = k + jj + halfM

                    val tr = wr * real[idx2] - wi * imag[idx2]
                    val ti = wr * imag[idx2] + wi * real[idx2]

                    real[idx2] = real[idx1] - tr
                    imag[idx2] = imag[idx1] - ti
                    real[idx1] += tr
                    imag[idx1] += ti
                }
            }
            mLen = mLen shl 1
        }

        // Scale for inverse FFT
        if (!forward) {
            val invN = 1.0 / n
            for (i in 0 until n) {
                real[i] *= invN
                imag[i] *= invN
            }
        }
    }

    /** Returns the smallest power of 2 >= n. */
    fun nextPowerOf2(n: Int): Int {
        var v = maxOf(1, n)
        v--
        v = v or (v shr 1)
        v = v or (v shr 2)
        v = v or (v shr 4)
        v = v or (v shr 8)
        v = v or (v shr 16)
        v++
        return v
    }
}
