package com.stabila.feature.camera.sensor

import android.graphics.Bitmap

class SceneAnalyzer {

    // Current active state
    private var isCurrentlyBright = false
    
    // Counters for debouncing (Hysteresis)
    private var consecutiveBrightCount = 0
    private var consecutiveDarkCount = 0
    
    // We require 3 consecutive frames to confirm a lighting change to prevent flickering
    private val CONFIRMATION_THRESHOLD = 3 
    
    // Hysteresis thresholds (0-255 luminance scale)
    // Using two different thresholds creates a "dead zone" that stops flickering 
    // if the brightness hovers right on the edge.
    private val UPPER_THRESHOLD = 150f // Must be brighter than this to switch to Bright mode
    private val LOWER_THRESHOLD = 110f // Must be darker than this to switch to Normal/Dark mode

    /**
     * Analyzes the frame for Luminance (Brightness).
     * Returns true if we should switch to FAST SHUTTER (1/1000s) mode.
     */
    fun isBrightScene(bitmap: Bitmap): Boolean {
        val luminance = calculateLuminance(bitmap)
        
        if (luminance > UPPER_THRESHOLD) {
            consecutiveBrightCount++
            consecutiveDarkCount = 0 // Reset the other counter
            
            if (consecutiveBrightCount >= CONFIRMATION_THRESHOLD) {
                isCurrentlyBright = true
            }
        } else if (luminance < LOWER_THRESHOLD) {
            consecutiveDarkCount++
            consecutiveBrightCount = 0 // Reset the other counter
            
            if (consecutiveDarkCount >= CONFIRMATION_THRESHOLD) {
                isCurrentlyBright = false
            }
        } else {
            // In the "dead zone" between 110 and 150.
            // We do nothing and maintain the current state.
            consecutiveBrightCount = 0
            consecutiveDarkCount = 0
        }
        
        return isCurrentlyBright
    }

    private fun calculateLuminance(bitmap: Bitmap): Float {
        // Fast luminance check by sampling pixels across the image
        var totalLuminance = 0L
        var count = 0
        
        val step = 10 // Sample every 10th pixel for incredible speed (less than 1ms)
        for (y in 0 until bitmap.height step step) {
            for (x in 0 until bitmap.width step step) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xff
                val g = (pixel shr 8) and 0xff
                val b = (pixel and 0xff)
                
                // standard relative luminance formula
                val luminance = (0.299 * r + 0.587 * g + 0.114 * b)
                totalLuminance += luminance.toLong()
                count++
            }
        }
        return if (count > 0) (totalLuminance / count.toFloat()) else 0f
    }
}
