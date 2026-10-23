package com.example.healthcareai.heartrate

import com.example.healthcareai.ui.heartrate.engine.BpmEstimator
import com.example.healthcareai.ui.heartrate.engine.ProcessedSignal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class BpmEstimatorTest {

    private val estimator = BpmEstimator()

    @Test
    fun testSynthetic72BpmSignal() {
        val fs = 30.0
        val targetBpm = 72.0
        val targetFreqHz = targetBpm / 60.0 // 1.2 Hz
        val durationSec = 10.0
        val n = (durationSec * fs).toInt()

        val timestampsNs = LongArray(n)
        val signal = DoubleArray(n)
        val rand = java.util.Random(123)

        for (i in 0 until n) {
            val tSec = i / fs
            timestampsNs[i] = (tSec * 1_000_000_000L).toLong()
            // Fundamental + 2nd harmonic + small noise
            signal[i] = sin(2.0 * PI * targetFreqHz * tSec) +
                    0.25 * sin(2.0 * PI * 2 * targetFreqHz * tSec) +
                    0.05 * rand.nextGaussian()
        }

        val processed = ProcessedSignal(
            filteredSignal = signal,
            timestampsNs = timestampsNs,
            sampleRateHz = fs,
            uiWaveform = FloatArray(n)
        )

        val result = estimator.estimate(processed)
        assertNotNull("Should estimate BPM for clear 72 BPM signal", result)
        assertTrue("Estimated BPM should be around 72 (was ${result!!.bpm})", result.bpm in 69..75)
        assertTrue("SQI should be high (was ${result.sqi})", result.sqi >= 0.50)
    }

    @Test
    fun testBradycardia48BpmSignal() {
        val fs = 30.0
        val targetBpm = 48.0
        val targetFreqHz = targetBpm / 60.0 // 0.8 Hz
        val durationSec = 10.0
        val n = (durationSec * fs).toInt()

        val timestampsNs = LongArray(n)
        val signal = DoubleArray(n)

        for (i in 0 until n) {
            val tSec = i / fs
            timestampsNs[i] = (tSec * 1_000_000_000L).toLong()
            signal[i] = sin(2.0 * PI * targetFreqHz * tSec)
        }

        val processed = ProcessedSignal(
            filteredSignal = signal,
            timestampsNs = timestampsNs,
            sampleRateHz = fs,
            uiWaveform = FloatArray(n)
        )

        val result = estimator.estimate(processed)
        assertNotNull(result)
        assertTrue("Estimated BPM should be near 48 (was ${result!!.bpm})", result.bpm in 46..51)
    }

    @Test
    fun testTachycardia150BpmSignal() {
        val fs = 30.0
        val targetBpm = 150.0
        val targetFreqHz = targetBpm / 60.0 // 2.5 Hz
        val durationSec = 8.0
        val n = (durationSec * fs).toInt()

        val timestampsNs = LongArray(n)
        val signal = DoubleArray(n)

        for (i in 0 until n) {
            val tSec = i / fs
            timestampsNs[i] = (tSec * 1_000_000_000L).toLong()
            signal[i] = sin(2.0 * PI * targetFreqHz * tSec)
        }

        val processed = ProcessedSignal(
            filteredSignal = signal,
            timestampsNs = timestampsNs,
            sampleRateHz = fs,
            uiWaveform = FloatArray(n)
        )

        val result = estimator.estimate(processed)
        assertNotNull(result)
        assertTrue("Estimated BPM should be near 150 (was ${result!!.bpm})", result.bpm in 146..154)
    }

    @Test
    fun testFlatSignalYieldsNullOrZero() {
        val fs = 30.0
        val n = 300
        val timestampsNs = LongArray(n) { (it / fs * 1e9).toLong() }
        val signal = DoubleArray(n) { 0.0 } // Flat line

        val processed = ProcessedSignal(
            filteredSignal = signal,
            timestampsNs = timestampsNs,
            sampleRateHz = fs,
            uiWaveform = FloatArray(n)
        )

        val result = estimator.estimate(processed)
        assertTrue("Flat signal should yield null or zero BPM", result == null || result.bpm == 0)
    }
}
