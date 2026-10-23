package com.example.healthcareai.heartrate

import com.example.healthcareai.ui.heartrate.dsp.ButterworthFilter
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class ButterworthFilterTest {

    @Test
    fun testPassBandRetention() {
        val fs = 30.0
        val filter = ButterworthFilter(lowCutoffHz = 0.7, highCutoffHz = 4.0, sampleRateHz = fs)

        // 1.2 Hz sine wave (72 BPM) - squarely in the pass band
        val n = 300 // 10 seconds of 30 Hz
        val input = DoubleArray(n) { i ->
            val t = i / fs
            sin(2.0 * PI * 1.2 * t)
        }

        val output = filter.filterSignal(input)

        // Steady state (skip first 60 samples to avoid initial transient response)
        var maxIn = 0.0
        var maxOut = 0.0
        for (i in 60 until n) {
            val absIn = kotlin.math.abs(input[i])
            val absOut = kotlin.math.abs(output[i])
            if (absIn > maxIn) maxIn = absIn
            if (absOut > maxOut) maxOut = absOut
        }

        // Passband gain should be near 1.0 (at least > 0.6)
        val gain = maxOut / maxIn
        assertTrue("Pass band gain should be > 0.6, was $gain", gain > 0.6)
    }

    @Test
    fun testDcAndLowFrequencyAttenuation() {
        val fs = 30.0
        val filter = ButterworthFilter(lowCutoffHz = 0.7, highCutoffHz = 4.0, sampleRateHz = fs)

        // Very slow drift 0.1 Hz (6 BPM, far below 0.7 Hz cutoff)
        val n = 300
        val input = DoubleArray(n) { i ->
            val t = i / fs
            sin(2.0 * PI * 0.1 * t) + 10.0 // DC offset + slow drift
        }

        val output = filter.filterSignal(input)

        // After settling, output amplitude should be heavily attenuated
        var maxOut = 0.0
        for (i in 100 until n) {
            val absOut = kotlin.math.abs(output[i])
            if (absOut > maxOut) maxOut = absOut
        }

        // DC + 0.1Hz should be attenuated significantly compared to original 10.0 + 1.0
        assertTrue("Low-frequency drift & DC should be attenuated, was $maxOut", maxOut < 2.0)
    }

    @Test
    fun testHighFrequencyAttenuation() {
        val fs = 30.0
        val filter = ButterworthFilter(lowCutoffHz = 0.7, highCutoffHz = 4.0, sampleRateHz = fs)

        // 12.0 Hz high frequency noise (well above 4.0 Hz cutoff)
        val n = 300
        val input = DoubleArray(n) { i ->
            val t = i / fs
            sin(2.0 * PI * 12.0 * t)
        }

        val output = filter.filterSignal(input)

        var maxIn = 0.0
        var maxOut = 0.0
        for (i in 60 until n) {
            val absIn = kotlin.math.abs(input[i])
            val absOut = kotlin.math.abs(output[i])
            if (absIn > maxIn) maxIn = absIn
            if (absOut > maxOut) maxOut = absOut
        }

        val gain = maxOut / maxIn
        assertTrue("High-frequency gain should be < 0.35, was $gain", gain < 0.35)
    }
}
