package com.example.healthcareai.heartrate

import com.example.healthcareai.ui.heartrate.engine.SignalProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class SignalProcessorTest {

    @Test
    fun testSlidingWindowTrimming() {
        // Window = 5 seconds
        val processor = SignalProcessor(windowDurationSec = 5.0, targetSampleRate = 30.0)

        // Feed 10 seconds of data at 30 fps (~33.3ms per sample)
        val stepNs = (1_000_000_000L / 30)
        var t = 1_000_000_000L
        for (i in 0 until 300) {
            processor.addSample(t, 100.0)
            t += stepNs
        }

        // Buffer duration should be capped around 5.0s
        val duration = processor.getBufferedDurationSec()
        assertTrue("Buffer duration should be ~5.0s, was $duration", duration in 4.8..5.2)
    }

    @Test
    fun testProcessCurrentWindowProducesUniformSignal() {
        val processor = SignalProcessor(windowDurationSec = 8.0, targetSampleRate = 30.0)

        // Feed 6 seconds of a 1.2 Hz pulse with non-uniform timestamp jitter (+/- 3ms)
        var t = 10_000_000_000L
        val random = java.util.Random(42)
        for (i in 0 until 180) {
            val sec = (t - 10_000_000_000L) / 1_000_000_000.0
            val ppg = 120.0 + 15.0 * sin(2.0 * PI * 1.2 * sec)
            processor.addSample(t, ppg)
            val jitterNs = (random.nextInt(6) - 3) * 1_000_000L
            t += (33_333_333L + jitterNs)
        }

        val processed = processor.processCurrentWindow()
        assertNotNull("ProcessedSignal should not be null", processed)
        assertEquals(30.0, processed!!.sampleRateHz, 0.001)
        assertTrue("Filtered signal should have elements", processed.filteredSignal.isNotEmpty())
        assertTrue("Timestamps should match length", processed.timestampsNs.size == processed.filteredSignal.size)

        // Check UI waveform is bounded [-1.0, 1.0]
        for (v in processed.uiWaveform) {
            assertTrue("Waveform must be between -1.0 and 1.0, was $v", v in -1.0f..1.0f)
        }
    }

    @Test
    fun testResetClearsBuffer() {
        val processor = SignalProcessor()
        processor.addSample(1_000_000_000L, 50.0)
        processor.addSample(2_000_000_000L, 55.0)
        assertEquals(2, processor.getSampleCount())

        processor.reset()
        assertEquals(0, processor.getSampleCount())
        assertEquals(0.0, processor.getBufferedDurationSec(), 0.001)
    }
}
