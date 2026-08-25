package com.stabila.feature.dailytest.sensor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import javax.inject.Inject

class SpiralImageGenerator @Inject constructor() {

    companion object {
        const val IMAGE_SIZE = 128
    }

    /**
     * Translates a list of raw touch coordinates into a standardized 256x256 
     * black-and-white bitmap image, perfectly formatted for a CNN AI model.
     */
    fun generateImage(points: List<TouchPoint>): Bitmap? {
        if (points.isEmpty()) return null

        // 1. Find bounding box to normalize the scale
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        for (p in points) {
            if (p.x < minX) minX = p.x
            if (p.y < minY) minY = p.y
            if (p.x > maxX) maxX = p.x
            if (p.y > maxY) maxY = p.y
        }

        val width = maxX - minX
        val height = maxY - minY
        
        // Prevent division by zero if it's a single point or flat line
        if (width <= 0 || height <= 0) return null

        val scale = (IMAGE_SIZE * 0.8f) / maxOf(width, height)
        
        val offsetX = (IMAGE_SIZE - (width * scale)) / 2f
        val offsetY = (IMAGE_SIZE - (height * scale)) / 2f

        // 2. Setup the Canvas
        val bitmap = Bitmap.createBitmap(IMAGE_SIZE, IMAGE_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE) // Model needs white paper

        val paint = Paint().apply {
            color = Color.BLACK // Model needs black ink
            style = Paint.Style.STROKE
            strokeWidth = 3f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }

        // 3. Draw the normalized path
        val path = Path()
        var isFirst = true
        
        for (p in points) {
            // Normalize coordinate to fit in the 256x256 box
            val normX = ((p.x - minX) * scale) + offsetX
            val normY = ((p.y - minY) * scale) + offsetY

            if (isFirst) {
                path.moveTo(normX, normY)
                isFirst = false
            } else {
                path.lineTo(normX, normY)
            }
        }
        
        canvas.drawPath(path, paint)

        return bitmap
    }
}
