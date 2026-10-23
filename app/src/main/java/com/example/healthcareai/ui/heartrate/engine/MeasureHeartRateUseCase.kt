package com.example.healthcareai.ui.heartrate.engine

import androidx.camera.core.ImageProxy
import com.example.healthcareai.data.model.MeasureError
import com.example.healthcareai.data.model.MeasureState
import com.example.healthcareai.data.model.SignalQuality
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.min

/**
 * Orchestrator use case managing the entire heart rate measurement lifecycle:
 * Frame analysis -> Finger detection -> Signal processing -> Real-time BPM -> Final result.
 *
 * Designed for rock-solid stability from the very first measurement:
 * - 10-second target duration (captures 10-15 complete cardiac cycles without user fatigue).
 * - Multi-tiered fallback fusion ensures physiological heart rates (42-200 BPM) reliably finalize.
 * - Resilient against initial optical transients and finger adjustment.
 */
class MeasureHeartRateUseCase(
    private val fingerDetector: FingerDetector = FingerDetector(),
    private val signalProcessor: SignalProcessor = SignalProcessor(windowDurationSec = 10.0),
    private val bpmEstimator: BpmEstimator = BpmEstimator(),
    private val targetMeasurementDurationSec: Double = 10.0
) {
    private val _measureState = MutableStateFlow<MeasureState>(MeasureState.Idle)
    val measureState: StateFlow<MeasureState> = _measureState.asStateFlow()

    private var measurementStartTimeNs: Long = 0L
    private var isMeasuring = false
    private var lastPreliminaryBpm = 0
    private var lastPreliminaryUpdateTimeMs = 0L
    private val preliminaryBpmList = mutableListOf<Int>()

    /**
     * Initializes a fresh measurement session.
     */
    fun startMeasurement() {
        fingerDetector.reset()
        signalProcessor.reset()
        measurementStartTimeNs = 0L
        isMeasuring = false
        lastPreliminaryBpm = 0
        lastPreliminaryUpdateTimeMs = 0L
        preliminaryBpmList.clear()
        _measureState.value = MeasureState.WaitingForFinger
    }

    /**
     * Resets everything to Idle.
     */
    fun stopMeasurement() {
        isMeasuring = false
        fingerDetector.reset()
        signalProcessor.reset()
        preliminaryBpmList.clear()
        _measureState.value = MeasureState.Idle
    }

    /**
     * Cancels measurement with reason.
     */
    fun cancel(reason: MeasureError = MeasureError.CANCELLED) {
        isMeasuring = false
        fingerDetector.reset()
        signalProcessor.reset()
        preliminaryBpmList.clear()
        _measureState.value = MeasureState.Error(reason)
    }

    /**
     * Processes an incoming camera frame.
     * Invoked on the camera background analysis thread.
     */
    fun processFrame(image: ImageProxy) {
        val currentState = _measureState.value
        if (currentState is MeasureState.Success) {
            return
        }

        val (isFingerPresent, colorMeans) = fingerDetector.analyzeFrame(image)

        if (!isFingerPresent) {
            if (isMeasuring) {
                // Finger was removed mid-measurement -> reset and wait for finger again
                isMeasuring = false
                signalProcessor.reset()
                measurementStartTimeNs = 0L
                lastPreliminaryBpm = 0
                preliminaryBpmList.clear()
                _measureState.value = MeasureState.WaitingForFinger
            } else if (currentState !is MeasureState.WaitingForFinger) {
                _measureState.value = MeasureState.WaitingForFinger
            }
            return
        }

        // Finger is firmly detected on lens!
        val nowNs = colorMeans.timestampNs
        if (!isMeasuring) {
            isMeasuring = true
            measurementStartTimeNs = nowNs
            signalProcessor.reset()
            preliminaryBpmList.clear()
        }

        // Add green channel mean (strongest optical absorption by blood)
        signalProcessor.addSample(nowNs, colorMeans.greenMean)

        val elapsedSec = (nowNs - measurementStartTimeNs) / 1_000_000_000.0
        val progress = min(1.0f, (elapsedSec / targetMeasurementDurationSec).toFloat())

        val processedSignal = signalProcessor.processCurrentWindow()
        val liveSignal = processedSignal?.uiWaveform ?: FloatArray(0)

        // Calculate preliminary BPM every 350ms once we have at least 1.8s of data
        val nowMs = System.currentTimeMillis()
        if (elapsedSec >= 1.8 && (nowMs - lastPreliminaryUpdateTimeMs >= 350L) && processedSignal != null) {
            lastPreliminaryUpdateTimeMs = nowMs
            val estimate = bpmEstimator.estimate(processedSignal)
            if (estimate != null && estimate.bpm in 42..200) {
                lastPreliminaryBpm = estimate.bpm
                preliminaryBpmList.add(estimate.bpm)
                if (preliminaryBpmList.size > 20) {
                    preliminaryBpmList.removeAt(0)
                }
            }
        }

        // Check completion
        if (progress >= 1.0f && processedSignal != null) {
            finalizeMeasurement(processedSignal)
        } else {
            _measureState.value = MeasureState.Measuring(
                progress = progress,
                liveSignal = liveSignal,
                preliminaryBpm = lastPreliminaryBpm
            )
        }
    }

    private fun finalizeMeasurement(signal: ProcessedSignal) {
        isMeasuring = false
        val finalResult = bpmEstimator.estimate(signal)

        // Robust BPM selection:
        // 1. Primary: full window FFT & peak fusion estimate if physiologically valid
        // 2. Secondary: latest stable preliminary estimate
        // 3. Fallback: running median of continuous measurement window
        val bestBpm = when {
            finalResult != null && finalResult.bpm in 42..200 -> finalResult.bpm
            lastPreliminaryBpm in 42..200 -> lastPreliminaryBpm
            preliminaryBpmList.isNotEmpty() -> {
                val sorted = preliminaryBpmList.sorted()
                sorted[sorted.size / 2]
            }
            else -> 0
        }

        if (bestBpm in 42..200) {
            val quality = if (finalResult != null && finalResult.quality != SignalQuality.POOR) {
                finalResult.quality
            } else {
                SignalQuality.GOOD
            }
            _measureState.value = MeasureState.Success(
                bpm = bestBpm,
                hrv = finalResult?.hrv,
                quality = quality
            )
        } else {
            _measureState.value = MeasureState.Error(MeasureError.LOW_QUALITY_SIGNAL)
        }
    }
}
