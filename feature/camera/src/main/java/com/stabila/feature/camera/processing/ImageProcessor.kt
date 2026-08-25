package com.stabila.feature.camera.processing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import org.opencv.photo.Photo
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Singleton
class ImageProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // -------------------------------------------------------------------------
    // STEP 2: Frame alignment + stacking
    // -------------------------------------------------------------------------

    /**
     * Aligns all frames to the reference (sharpest) frame using brute-force
     * normalized cross-correlation on a 128x128 thumbnail to estimate a 2D
     * translation offset. Then averages all aligned frames pixel-by-pixel.
     *
     * This is the "Lucky Imaging + Stacking" technique used in astronomy.
     *
     * @param reference The sharpest frame chosen in Step 1.
     * @param frames    All frames from the live buffer (including the reference).
     * @return          A stacked Bitmap with less noise and sharper edges.
     */
    suspend fun alignAndStack(reference: Bitmap, frames: List<Bitmap>): Bitmap =
        withContext(Dispatchers.Default) {

        val w = reference.width
        val h = reference.height

        val sumR = FloatArray(w * h)
        val sumG = FloatArray(w * h)
        val sumB = FloatArray(w * h)
        var stackedCount = 0

        val refPixels = IntArray(w * h)
        reference.getPixels(refPixels, 0, w, 0, 0, w, h)

        val thumbW = min(w, 128)
        val thumbH = min(h, 128)
        val refThumb = resizePixels(refPixels, w, h, thumbW, thumbH)

        for (frame in frames) {
            val frameScaled: Bitmap = if (frame.width != w || frame.height != h) {
                Bitmap.createScaledBitmap(frame, w, h, true)
            } else {
                frame
            }

            val framePixels = IntArray(w * h)
            frameScaled.getPixels(framePixels, 0, w, 0, 0, w, h)

            val frameThumb = resizePixels(framePixels, w, h, thumbW, thumbH)

            val (dx, dy) = estimateTranslation(refThumb, frameThumb, thumbW, thumbH)

            // Scale offset back to full resolution
            val fullDx = (dx * w.toFloat() / thumbW).roundToInt()
            val fullDy = (dy * h.toFloat() / thumbH).roundToInt()

            // Clamp to ±20% (tremors don't move the camera this far)
            val clampedDx = fullDx.coerceIn(-w / 5, w / 5)
            val clampedDy = fullDy.coerceIn(-h / 5, h / 5)

            for (y in 0 until h) {
                for (x in 0 until w) {
                    val srcX = x - clampedDx
                    val srcY = y - clampedDy
                    if (srcX < 0 || srcX >= w || srcY < 0 || srcY >= h) continue
                    val pixel = framePixels[srcY * w + srcX]
                    sumR[y * w + x] = sumR[y * w + x] + Color.red(pixel)
                    sumG[y * w + x] = sumG[y * w + x] + Color.green(pixel)
                    sumB[y * w + x] = sumB[y * w + x] + Color.blue(pixel)
                }
            }
            stackedCount++

            if (frameScaled !== frame) frameScaled.recycle()
        }

        val n = stackedCount.toFloat()
        val resultPixels = IntArray(w * h)
        for (i in resultPixels.indices) {
            resultPixels[i] = Color.rgb(
                (sumR[i] / n).roundToInt().coerceIn(0, 255),
                (sumG[i] / n).roundToInt().coerceIn(0, 255),
                (sumB[i] / n).roundToInt().coerceIn(0, 255)
            )
        }

        val stacked = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        stacked.setPixels(resultPixels, 0, w, 0, 0, w, h)
        stacked
    }

    /** Estimates (dx,dy) translation using cross-correlation on thumbnails. */
    private fun estimateTranslation(
        ref: IntArray, frame: IntArray,
        w: Int, h: Int,
        searchRadius: Int = 20
    ): Pair<Int, Int> {
        var bestDx = 0
        var bestDy = 0
        var bestScore = Double.NEGATIVE_INFINITY

        val refLuma   = IntArray(w * h) { i -> luminance(ref[i]) }
        val frameLuma = IntArray(w * h) { i -> luminance(frame[i]) }

        for (dy in -searchRadius..searchRadius) {
            for (dx in -searchRadius..searchRadius) {
                var score = 0.0
                var count = 0
                for (y in searchRadius until h - searchRadius) {
                    for (x in searchRadius until w - searchRadius) {
                        val sy = y + dy; val sx = x + dx
                        if (sx < 0 || sx >= w || sy < 0 || sy >= h) continue
                        score += refLuma[y * w + x].toDouble() * frameLuma[sy * w + sx]
                        count++
                    }
                }
                if (count > 0) score /= count
                if (score > bestScore) { bestScore = score; bestDx = dx; bestDy = dy }
            }
        }
        return Pair(bestDx, bestDy)
    }

    private fun resizePixels(src: IntArray, srcW: Int, srcH: Int, dstW: Int, dstH: Int): IntArray {
        val dst = IntArray(dstW * dstH)
        for (y in 0 until dstH) {
            for (x in 0 until dstW) {
                val sx = (x * srcW.toFloat() / dstW).roundToInt().coerceIn(0, srcW - 1)
                val sy = (y * srcH.toFloat() / dstH).roundToInt().coerceIn(0, srcH - 1)
                dst[y * dstW + x] = src[sy * srcW + sx]
            }
        }
        return dst
    }

    private fun luminance(pixel: Int): Int =
        (0.299 * Color.red(pixel) + 0.587 * Color.green(pixel) + 0.114 * Color.blue(pixel)).toInt()

    // -------------------------------------------------------------------------
    // STEP 2: Brightness Correction
    // -------------------------------------------------------------------------
    
    suspend fun boostBrightness(src: Bitmap, factor: Float): Bitmap = withContext(Dispatchers.Default) {
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (Color.red(p) * factor).roundToInt().coerceIn(0, 255)
            val g = (Color.green(p) * factor).roundToInt().coerceIn(0, 255)
            val b = (Color.blue(p) * factor).roundToInt().coerceIn(0, 255)
            pixels[i] = Color.rgb(r, g, b)
        }
        
        val dest = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        dest.setPixels(pixels, 0, w, 0, 0, w, h)
        dest
    }

    // -------------------------------------------------------------------------
    // STEP 3: Zero-DCE On-Device Low-Light Enhancement
    // -------------------------------------------------------------------------

    suspend fun applyOpenCVDenoising(src: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val srcW = src.width
        val srcH = src.height

        try {
            // 1. Convert Android Bitmap to OpenCV Mat (RGBA)
            val matRgba = Mat()
            Utils.bitmapToMat(src, matRgba)

            // 2. OpenCV's fastNlMeansDenoisingColored requires 3 channels.
            // Convert RGBA -> RGB
            val matRgb = Mat()
            Imgproc.cvtColor(matRgba, matRgb, Imgproc.COLOR_RGBA2RGB)
            matRgba.release()

            // 3. Apply the exact Goldilocks OpenCV Non-Local Means algorithm
            val denoisedRgb = Mat()
            Photo.fastNlMeansDenoisingColored(
                matRgb,          // src
                denoisedRgb,     // dst
                17f,             // h (Luminance filter strength)
                20f,             // hColor (Color filter strength)
                7,               // templateWindowSize
                21               // searchWindowSize
            )
            matRgb.release()

            // 4. Convert back to RGBA for Android Bitmap
            val finalRgba = Mat()
            Imgproc.cvtColor(denoisedRgb, finalRgba, Imgproc.COLOR_RGB2RGBA)
            denoisedRgb.release()

            val finalBmp = Bitmap.createBitmap(srcW, srcH, Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(finalRgba, finalBmp)
            finalRgba.release()

            return@withContext finalBmp

        } catch (e: Throwable) {
            e.printStackTrace()

            // Draw the exception message onto a new Bitmap so we can read it if OpenCV crashes!
            val errorBmp = Bitmap.createBitmap(srcW, srcH, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(errorBmp)
            canvas.drawColor(android.graphics.Color.RED)
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 40f
                isAntiAlias = true
            }
            val msg = e.toString()
            var y = 100f
            msg.chunked(50).forEach { line ->
                canvas.drawText(line, 50f, y, paint)
                y += 50f
            }

            return@withContext errorBmp
        }
    }

    // -------------------------------------------------------------------------
    // Legacy: mild final sharpening pass applied after stacking.
    // -------------------------------------------------------------------------

    /** Applies a mild final sharpening convolution after stacking. */
    suspend fun applyDeblurFilter(src: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val width = src.width
        val height = src.height
        val dest = Bitmap.createBitmap(width, height, src.config ?: Bitmap.Config.ARGB_8888)

        // Mild unsharp-mask kernel
        val matrix = floatArrayOf(
            -0.5f, -0.5f, -0.5f,
            -0.5f,  5f,  -0.5f,
            -0.5f, -0.5f, -0.5f
        )

        val pixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)
        val resultPixels = IntArray(width * height)

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var r = 0f; var g = 0f; var b = 0f
                var k = 0
                for (dy in -1..1) for (dx in -1..1) {
                    val pixel = pixels[(y + dy) * width + (x + dx)]
                    val weight = matrix[k++]
                    r += Color.red(pixel) * weight
                    g += Color.green(pixel) * weight
                    b += Color.blue(pixel) * weight
                }
                resultPixels[y * width + x] = Color.rgb(
                    min(max(r.toInt(), 0), 255),
                    min(max(g.toInt(), 0), 255),
                    min(max(b.toInt(), 0), 255)
                )
            }
        }
        for (x in 0 until width) {
            resultPixels[x] = pixels[x]
            resultPixels[(height - 1) * width + x] = pixels[(height - 1) * width + x]
        }
        for (y in 0 until height) {
            resultPixels[y * width] = pixels[y * width]
            resultPixels[y * width + (width - 1)] = pixels[y * width + (width - 1)]
        }
        dest.setPixels(resultPixels, 0, width, 0, 0, width, height)
        dest
    }
    /**
     * Calculates a sharpness score for the image using the Variance of Laplacian method.
     * Higher score means a sharper image with more pronounced edges.
     */
    suspend fun calculateSharpness(src: Bitmap): Float = withContext(Dispatchers.Default) {
        calculateSharpnessBlocking(src)
    }

    fun calculateSharpnessBlocking(src: Bitmap): Float {
        val width = src.width
        val height = src.height
        val pixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        var sum = 0.0
        var sumSq = 0.0
        var count = 0

        // Subsample for performance (evaluate every 4th pixel)
        val step = 4

        for (y in 1 until height - 1 step step) {
            for (x in 1 until width - 1 step step) {
                // Using the Green channel as a proxy for luminance (fast and effective)
                val pCenter = Color.green(pixels[y * width + x])
                val pTop = Color.green(pixels[(y - 1) * width + x])
                val pBottom = Color.green(pixels[(y + 1) * width + x])
                val pLeft = Color.green(pixels[y * width + (x - 1)])
                val pRight = Color.green(pixels[y * width + (x + 1)])

                val laplacian = pTop + pBottom + pLeft + pRight - 4 * pCenter

                sum += laplacian
                sumSq += laplacian * laplacian
                count++
            }
        }

        if (count == 0) return 0f

        val mean = sum / count
        val variance = (sumSq / count) - (mean * mean)

        return variance.toFloat()
    }
}
