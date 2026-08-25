package com.stabila.feature.history.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.stabila.core.domain.TremorReading
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun exportReadingsToPdf(readings: List<TremorReading>): File? = withContext(Dispatchers.IO) {
        if (readings.isEmpty()) return@withContext null

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 at 72 PPI
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        drawPdfContent(canvas, readings)

        document.finishPage(page)

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Stabila_Clinical_Report_$timestamp.pdf"
        
        // Save to cache dir so it can be shared via FileProvider
        val file = File(context.cacheDir, fileName)
        
        try {
            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        } finally {
            document.close()
        }

        file
    }

    private fun drawPdfContent(canvas: Canvas, readings: List<TremorReading>) {
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            isAntiAlias = true
        }

        // Title
        paint.textSize = 28f
        paint.isFakeBoldText = true
        canvas.drawText("Stabila Clinical Report", 50f, 60f, paint)

        // Subtitle / Date
        paint.textSize = 14f
        paint.isFakeBoldText = false
        paint.color = Color.DKGRAY
        val df = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault())
        canvas.drawText("Generated on: ${df.format(Date())}", 50f, 90f, paint)
        canvas.drawText("Total Readings: ${readings.size}", 50f, 110f, paint)

        // Basic stats
        val avgScore = readings.map { it.score }.average()
        canvas.drawText("Average Score: ${String.format("%.1f", avgScore)}/100", 50f, 130f, paint)

        // Draw a simple chart (mocking the axes for now)
        drawChart(canvas, readings, 50f, 180f, 495f, 200f)

        // Table Header
        var yPos = 420f
        paint.color = Color.BLACK
        paint.isFakeBoldText = true
        paint.textSize = 12f
        canvas.drawText("Date", 50f, yPos, paint)
        canvas.drawText("Type", 200f, yPos, paint)
        canvas.drawText("Freq (Hz)", 300f, yPos, paint)
        canvas.drawText("Amp", 400f, yPos, paint)
        canvas.drawText("Score", 500f, yPos, paint)

        // Table Rows (Draw up to 20 for one page)
        paint.isFakeBoldText = false
        val displayReadings = readings.take(20)
        
        yPos += 20f
        for (reading in displayReadings) {
            canvas.drawText(df.format(Date(reading.timestampEpochMs)), 50f, yPos, paint)
            canvas.drawText(reading.testType.name, 200f, yPos, paint)
            canvas.drawText(String.format("%.1f", reading.dominantFrequencyHz), 300f, yPos, paint)
            canvas.drawText(String.format("%.3f", reading.amplitude), 400f, yPos, paint)
            canvas.drawText(String.format("%.1f", reading.score), 500f, yPos, paint)
            yPos += 20f
        }
        
        if (readings.size > 20) {
            canvas.drawText("... and ${readings.size - 20} more readings.", 50f, yPos + 10f, paint)
        }
    }

    private fun drawChart(canvas: Canvas, readings: List<TremorReading>, x: Float, y: Float, w: Float, h: Float) {
        val paint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        // Draw axes
        canvas.drawRect(x, y, x + w, y + h, paint)

        // Need at least 2 points to draw a line graph
        if (readings.size < 2) return

        // Sort ascending for chart (left to right)
        val sorted = readings.sortedBy { it.timestampEpochMs }
        
        val maxScore = sorted.maxOfOrNull { it.score }?.coerceAtLeast(10f) ?: 100f
        
        paint.color = Color.parseColor("#6366F1") // Indigo500
        paint.strokeWidth = 3f
        paint.isAntiAlias = true

        val stepX = w / (sorted.size - 1)
        
        var prevX = x
        var prevY = y + h - ((sorted[0].score / maxScore) * h)

        for (i in 1 until sorted.size) {
            val currX = x + (i * stepX)
            val currY = y + h - ((sorted[i].score / maxScore) * h)
            
            canvas.drawLine(prevX, prevY, currX, currY, paint)
            
            prevX = currX
            prevY = currY
        }
    }
}
