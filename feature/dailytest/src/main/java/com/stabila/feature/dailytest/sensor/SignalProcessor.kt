package com.stabila.feature.dailytest.sensor

import org.apache.commons.math3.transform.DftNormalization
import org.apache.commons.math3.transform.FastFourierTransformer
import org.apache.commons.math3.transform.TransformType
import javax.inject.Inject
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.sqrt

class SignalProcessor @Inject constructor() {

    data class TremorAnalysisResult(
        val dominantFrequencyHz: Float,
        val amplitude: Float,
        val overallScore: Float,
        val classification: com.stabila.core.domain.TremorClassification? = null
    )

    /**
     * Processes a raw sequence of magnitudes to extract tremor features.
     * 1. Removes DC bias (mean subtraction).
     * 2. Zero-pads to the nearest power of 2 for FFT.
     * 3. Runs FFT and extracts the 4-12Hz (PD/ET band) peak.
     */
    fun process(magnitudes: List<Float>, durationMs: Long): TremorAnalysisResult {
        if (magnitudes.isEmpty() || durationMs == 0L) {
            return TremorAnalysisResult(0f, 0f, 0f)
        }

        val n = magnitudes.size
        val durationSec = durationMs / 1000.0
        val sampleRate = n / durationSec

        // 1. Remove DC offset (gravity or resting bias)
        val mean = magnitudes.average().toFloat()
        val zeroMeanData = magnitudes.map { (it - mean).toDouble() }.toDoubleArray()

        // 2. Pad to next power of 2 for Radix-2 FFT
        val power = kotlin.math.ceil(log2(n.toDouble())).toInt()
        val paddedSize = 2.0.pow(power).toInt()
        val paddedData = DoubleArray(paddedSize)
        System.arraycopy(zeroMeanData, 0, paddedData, 0, n)

        // 3. Perform FFT
        val transformer = FastFourierTransformer(DftNormalization.STANDARD)
        val complexResult = transformer.transform(paddedData, TransformType.FORWARD)

        // 4. Find peak in the 4-12Hz band
        var maxAmplitude = 0.0
        var dominantFreq = 0.0
        val freqResolution = sampleRate / paddedSize

        // We only care about positive frequencies up to Nyquist (paddedSize / 2)
        for (i in 0 until paddedSize / 2) {
            val freq = i * freqResolution
            // Real Parkinson's and Essential Tremor typically fall in 4-12Hz
            // But we widen it to 2-15Hz so that developers manually simulating tremor can test it
            if (freq in 2.0..15.0) {
                val real = complexResult[i].real
                val imag = complexResult[i].imaginary
                val magnitude = sqrt(real * real + imag * imag)

                // Normalize by padded size
                val normalizedMag = magnitude / paddedSize

                if (normalizedMag > maxAmplitude) {
                    maxAmplitude = normalizedMag
                    dominantFreq = freq
                }
            }
        }

        // Calculate a composite "score" (1-100 scale, higher = worse tremor)
        // Subtract a baseline noise floor (e.g. 0.02 m/s^2) so a perfectly stable phone reads 0
        val noiseFloor = 0.02f
        val rawScore = ((maxAmplitude.toFloat() - noiseFloor) * 1000f)
        val clampedScore = rawScore.coerceIn(0f, 100f)

        val finalFreq = if (clampedScore == 0f) 0f else dominantFreq.toFloat()
        val finalAmp = if (clampedScore == 0f) 0f else maxAmplitude.toFloat()

        return TremorAnalysisResult(
            dominantFrequencyHz = finalFreq,
            amplitude = finalAmp,
            overallScore = clampedScore
        )
    }
}
