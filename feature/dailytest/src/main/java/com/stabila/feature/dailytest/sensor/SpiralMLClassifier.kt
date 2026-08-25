package com.stabila.feature.dailytest.sensor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.stabila.core.domain.TremorClassification
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import javax.inject.Inject

class SpiralMLClassifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val spiralAnalyzer: SpiralAnalyzer
) {
    private var interpreter: Interpreter? = null

    init {
        try {
            val assetFileDescriptor = context.assets.openFd("parkinson_model.tflite")
            val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = fileInputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            val mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            
            val options = Interpreter.Options()
            options.setNumThreads(4)
            interpreter = Interpreter(mappedByteBuffer, options)
        } catch (e: Exception) {
            e.printStackTrace()
            // Model not found or invalid
        }
    }

    fun classify(points: List<TouchPoint>, bitmap: Bitmap?): ClassificationResult {
        // Run the math-based analysis for kinematic data tracking
        val kinematicResult = spiralAnalyzer.analyze(points)
        
        var classification = TremorClassification.NORMAL
        var confidence = 0f

        // PRE-FILTER: The CNN is trained on scanned paper and struggles with perfect digital ink.
        // It often flags perfectly smooth lines as "anomalies".
        // We use the Math Engine as a primary gate: if the physical vibration score is < 15 (very smooth),
        // we confidently classify it as Normal and bypass the CNN.
        if (kinematicResult.overallScore < 15f) {
            classification = TremorClassification.NORMAL
            confidence = 0.99f
        } else if (bitmap != null && interpreter != null) {
            // Convert Bitmap to ByteBuffer (1, 128, 128, 1) Grayscale Float32
            val inputBuffer = ByteBuffer.allocateDirect(1 * 128 * 128 * 1 * 4)
            inputBuffer.order(ByteOrder.nativeOrder())
            
            val pixels = IntArray(128 * 128)
            bitmap.getPixels(pixels, 0, 128, 0, 0, 128, 128)
            
            for (pixel in pixels) {
                // The bitmap is already drawn black on white, but we just extract intensity.
                // Standard grayscale conversion
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val grayscale = (0.299f * r + 0.587f * g + 0.114f * b)
                
                // Normalize to 0-1 as expected by the model
                val normalizedPixel = grayscale / 255.0f
                inputBuffer.putFloat(normalizedPixel)
            }

            // Output array: [1, 2] -> [P(Healthy), P(Parkinson)]
            val outputBuffer = Array(1) { FloatArray(2) }
            
            try {
                interpreter?.run(inputBuffer, outputBuffer)
                val pHealthy = outputBuffer[0][0]
                val pParkinson = outputBuffer[0][1]
                
                if (pParkinson > pHealthy) {
                    classification = TremorClassification.PARKINSONS
                    confidence = pParkinson
                } else {
                    classification = TremorClassification.NORMAL
                    confidence = pHealthy
                }
            } catch (e: Exception) {
                e.printStackTrace()
                classification = TremorClassification.UNCLASSIFIED
            }
        } else {
            // Fallback if model not loaded
            classification = TremorClassification.UNCLASSIFIED
        }
        
        return ClassificationResult(
            classification = classification,
            confidence = confidence,
            kinematicScore = kinematicResult.overallScore,
            kinematicFrequency = kinematicResult.dominantFrequencyHz,
            kinematicAmplitude = kinematicResult.amplitude
        )
    }
    
    data class ClassificationResult(
        val classification: TremorClassification,
        val confidence: Float,
        val kinematicScore: Float,
        val kinematicFrequency: Float,
        val kinematicAmplitude: Float
    )
}
