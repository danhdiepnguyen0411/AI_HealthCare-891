package com.example.healthcareai.ui.heartrate

import android.app.Application
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthcareai.data.model.HrvResult
import com.example.healthcareai.data.model.MeasureError
import com.example.healthcareai.data.model.MeasureState
import com.example.healthcareai.data.model.SignalQuality
import com.example.healthcareai.data.repository.HeartRateRecord
import com.example.healthcareai.data.repository.HeartRateRepository
import com.example.healthcareai.ui.heartrate.engine.MeasureHeartRateUseCase
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel orchestrating heart rate measurement state, camera frame delegation,
 * and saving records to repository.
 */
class HeartRateViewModel @JvmOverloads constructor(
    application: Application,
    private val useCase: MeasureHeartRateUseCase = MeasureHeartRateUseCase(),
    private val repository: HeartRateRepository = HeartRateRepository.getInstance(application)
) : AndroidViewModel(application) {

    val measureState: StateFlow<MeasureState> = useCase.measureState

    /**
     * Begins waiting for finger placement to measure.
     */
    fun startMeasurement() {
        useCase.startMeasurement()
    }

    /**
     * Feeds incoming camera frame to measurement use case pipeline.
     */
    fun processFrame(imageProxy: ImageProxy) {
        useCase.processFrame(imageProxy)
    }

    /**
     * User cancelled measurement.
     */
    fun cancelMeasurement() {
        useCase.cancel(MeasureError.CANCELLED)
    }

    /**
     * Reset measurement session to idle.
     */
    fun reset() {
        useCase.stopMeasurement()
    }

    /**
     * Saves measurement result to history.
     */
    fun saveMeasurement(bpm: Int, hrv: HrvResult?, quality: SignalQuality): HeartRateRecord {
        return repository.saveMeasurement(bpm, hrv, quality)
    }

    /**
     * Gets measurement history.
     */
    fun getHistory(): List<HeartRateRecord> {
        return repository.getAllRecords()
    }
}
