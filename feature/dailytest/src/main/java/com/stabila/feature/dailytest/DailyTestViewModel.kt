package com.stabila.feature.dailytest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stabila.core.data.db.TremorReadingDao
import com.stabila.core.domain.TestType
import com.stabila.core.domain.TremorReading
import com.stabila.feature.dailytest.sensor.SignalProcessor
import com.stabila.feature.dailytest.sensor.TremorSensorManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DailyTestViewModel @Inject constructor(
    private val sensorManager: TremorSensorManager,
    private val signalProcessor: SignalProcessor,
    private val tremorReadingDao: TremorReadingDao,
    private val spiralAnalyzer: com.stabila.feature.dailytest.sensor.SpiralAnalyzer,
    private val spiralImageGenerator: com.stabila.feature.dailytest.sensor.SpiralImageGenerator,
    private val spiralMLClassifier: com.stabila.feature.dailytest.sensor.SpiralMLClassifier
) : ViewModel() {

    enum class TestState {
        IDLE,
        COUNTDOWN,
        RECORDING,
        PROCESSING,
        COMPLETE
    }

    data class UiState(
        val testState: TestState = TestState.IDLE,
        val testType: TestType = TestType.ACTION,
        val countdownSeconds: Int = 3,
        val recordingProgress: Float = 0f,
        val currentMagnitude: Float = 0f,
        val result: SignalProcessor.TremorAnalysisResult? = null,
        val medicationTag: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val recordedMagnitudes = mutableListOf<Float>()
    private val recordedTouchPoints = mutableListOf<com.stabila.feature.dailytest.sensor.TouchPoint>()
    private var testJob: Job? = null
    private var sensorJob: Job? = null
    private var recordingStartTime = 0L

    fun startTest(type: TestType = TestType.ACTION) {
        if (_uiState.value.testState != TestState.IDLE && _uiState.value.testState != TestState.COMPLETE) return

        testJob?.cancel()
        sensorJob?.cancel()
        testJob = viewModelScope.launch {
            _uiState.update { it.copy(testType = type) }

            // 1. Countdown (Skip for Spiral)
            if (type != TestType.SPIRAL) {
                _uiState.update { it.copy(testState = TestState.COUNTDOWN, countdownSeconds = 3, result = null) }
                var c = 3
                while (c > 0) {
                    _uiState.update { it.copy(countdownSeconds = c) }
                    c--
                    delay(1000)
                }
            } else {
                // Clear UI state for spiral
                _uiState.update { it.copy(result = null) }
            }

            // 2. Recording
            _uiState.update { it.copy(testState = TestState.RECORDING, recordingProgress = 0f) }
            recordedMagnitudes.clear()
            recordedTouchPoints.clear()
            recordingStartTime = System.currentTimeMillis()
            val totalDurationMs = 20_000L
            var elapsed = 0L

            sensorJob = launch {
                if (type == TestType.ACTION) {
                    sensorManager.getSensorDataFlow().collect { dataPoint ->
                        val combinedMag = dataPoint.accelMagnitude + dataPoint.gyroMagnitude
                        recordedMagnitudes.add(combinedMag)
                        _uiState.update { it.copy(currentMagnitude = combinedMag) }
                    }
                }
            }

            if (type != TestType.SPIRAL) {
                // Update progress taking pauses into account
                while (elapsed < totalDurationMs) {
                    elapsed += 50
                    _uiState.update { it.copy(recordingProgress = elapsed.toFloat() / totalDurationMs) }
                    delay(50) // 20fps UI updates
                }
                sensorJob?.cancel()
                processTestResults(type)
            } else {
                // For spiral, we wait here indefinitely. `finishSpiralTest` will trigger processing.
            }
        }
    }

    fun finishSpiralTest() {
        if (_uiState.value.testState == TestState.RECORDING && _uiState.value.testType == TestType.SPIRAL) {
            viewModelScope.launch {
                processTestResults(TestType.SPIRAL)
            }
        }
    }

    private suspend fun processTestResults(type: TestType) {
        // 3. Processing
        _uiState.update { it.copy(testState = TestState.PROCESSING) }
        
        delay(500) // slight UI pause

        val result = if (type == TestType.SPIRAL) {
            val bitmap = spiralImageGenerator.generateImage(recordedTouchPoints)
            val mlResult = spiralMLClassifier.classify(recordedTouchPoints, bitmap)
            SignalProcessor.TremorAnalysisResult(
                dominantFrequencyHz = mlResult.kinematicFrequency,
                amplitude = mlResult.kinematicAmplitude,
                overallScore = mlResult.kinematicScore,
                classification = mlResult.classification
            )
        } else {
            signalProcessor.process(
                magnitudes = recordedMagnitudes,
                durationMs = 20_000L
            )
        }

        // Save to database
        val reading = TremorReading(
            timestampEpochMs = System.currentTimeMillis(),
            score = result.overallScore,
            amplitude = result.amplitude,
            dominantFrequencyHz = result.dominantFrequencyHz,
            testType = type,
            medicationTag = _uiState.value.medicationTag,
            classification = result.classification
        )
        tremorReadingDao.insertReading(reading)

        // 4. Complete
        _uiState.update { 
            it.copy(
                testState = TestState.COMPLETE,
                result = result
            )
        }
    }


    fun addSpiralPoint(x: Float, y: Float, timestampMs: Long) {
        if (_uiState.value.testState == TestState.RECORDING && _uiState.value.testType == TestType.SPIRAL) {
            recordedTouchPoints.add(com.stabila.feature.dailytest.sensor.TouchPoint(x, y, timestampMs))
        }
    }
    
    fun setMedicationTag(tag: String?) {
        _uiState.update { it.copy(medicationTag = tag) }
    }
    
    fun resetTest() {
        _uiState.update { UiState() }
    }
}
