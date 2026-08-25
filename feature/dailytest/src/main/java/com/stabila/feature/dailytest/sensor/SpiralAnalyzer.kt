package com.stabila.feature.dailytest.sensor

import javax.inject.Inject
import kotlin.math.sqrt

data class TouchPoint(val x: Float, val y: Float, val timestampMs: Long)

class SpiralAnalyzer @Inject constructor(
    private val signalProcessor: SignalProcessor
) {

    /**
     * Analyzes the drawn spiral for Tremor symptoms.
     * Extracts the instantaneous velocity between touch points and runs an FFT to find
     * high-frequency jitters typical of Essential Tremor (ET).
     */
    fun analyze(points: List<TouchPoint>): SignalProcessor.TremorAnalysisResult {
        if (points.size < 10) {
            return SignalProcessor.TremorAnalysisResult(0f, 0f, 0f)
        }

        val velocities = mutableListOf<Float>()
        val startTime = points.first().timestampMs
        val endTime = points.last().timestampMs
        val durationMs = endTime - startTime

        if (durationMs == 0L) {
            return SignalProcessor.TremorAnalysisResult(0f, 0f, 0f)
        }

        for (i in 1 until points.size) {
            val p1 = points[i - 1]
            val p2 = points[i]
            
            val dx = p2.x - p1.x
            val dy = p2.y - p1.y
            val dt = (p2.timestampMs - p1.timestampMs).toFloat()
            
            // Prevent division by zero if batch touch events have the same timestamp
            if (dt > 0f) {
                val distance = sqrt(dx * dx + dy * dy)
                val velocity = distance / dt
                velocities.add(velocity)
            }
        }

        // If all points arrived at the exact same millisecond (unlikely but possible with bad batching)
        if (velocities.isEmpty()) {
            return SignalProcessor.TremorAnalysisResult(0f, 0f, 0f)
        }

        // We pass the velocity magnitude stream into the standard FFT processor.
        // Jitters in drawing speed will show up as a frequency peak in the 4-12Hz band!
        val result = signalProcessor.process(velocities, durationMs)

        // Micrographia detection (PD tightens the spiral radius) can be added here in the future
        // by measuring the change in polar radius over time. For now, we rely on the kinematic FFT.
        
        // Boost the score slightly for velocity data since its baseline amplitude differs from accelerometer data
        val boostedScore = (result.overallScore * 1.5f).coerceIn(0f, 100f)
        
        return result.copy(overallScore = boostedScore)
    }
}
