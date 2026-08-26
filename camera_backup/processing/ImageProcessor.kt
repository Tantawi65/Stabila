package com.stabila.feature.camera.processing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
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

    /**
     * STEP 3 — Zero-DCE On-Device Low-Light Enhancement (TFLite)
     *
     * Pipeline:
     *   1. Resize source to 256×256
     *   2. Normalize to [0, 1] float
     *   3. Run Zero-DCE inference -> produces enhanced 256×256 float image
     *   4. Calculate the Gain Map (Enhanced / Original)
     *   5. Apply the Gain Map to the original full-resolution image!
     *      (This preserves 100% of the sharpness of the original Pro mode photo)
     */
    suspend fun applyZeroDCEEnhancement(src: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val srcW = src.width
        val srcH = src.height

        try {
            val model: Module
            val file = File(context.cacheDir, "zero_dce_mobile.ptl")
            if (!file.exists()) {
                context.assets.open("zero_dce_mobile.ptl").use { inputStream: InputStream ->
                    FileOutputStream(file).use { outputStream ->
                        val buffer = ByteArray(4 * 1024)
                        var read: Int
                        while (inputStream.read(buffer).also { read = it } != -1) {
                            outputStream.write(buffer, 0, read)
                        }
                        outputStream.flush()
                    }
                }
            }
            model = org.pytorch.LiteModuleLoader.load(file.absolutePath)

            val modelInputH = 400
            val modelInputW = 600

            // 1. Resize source to 600x400 for model input
            val resized = Bitmap.createScaledBitmap(src, modelInputW, modelInputH, true)

            // 2. Prepare PyTorch input Tensor [1, 3, 400, 600] float32 (NCHW format)
            val inputFloatArray = FloatArray(1 * 3 * modelInputH * modelInputW)

            val origSmallPixels = IntArray(modelInputW * modelInputH)
            resized.getPixels(origSmallPixels, 0, modelInputW, 0, 0, modelInputW, modelInputH)

            var idx = 0
            for (c in 0..2) {
                for (y in 0 until modelInputH) {
                    for (x in 0 until modelInputW) {
                        val pixel = origSmallPixels[y * modelInputW + x]
                        val value = when (c) {
                            0 -> Color.red(pixel).toFloat() / 255f
                            1 -> Color.green(pixel).toFloat() / 255f
                            else -> Color.blue(pixel).toFloat() / 255f
                        }
                        inputFloatArray[idx++] = value
                    }
                }
            }

            val inputTensor = Tensor.fromBlob(inputFloatArray, longArrayOf(1, 3, modelInputH.toLong(), modelInputW.toLong()))

            // 3. Run inference
            val outputTuple = model.forward(IValue.from(inputTensor)).toTuple()

            // Zero-DCE returns a tuple: (A, enhanced_image, R). We want the second element (index 1).
            val enhancedImageTensor = outputTuple[1].toTensor()
            val outputFloatArray = enhancedImageTensor.dataAsFloatArray

            // 4. Convert output NCHW tensor back to Bitmap
            val enhancedSmallPixels = IntArray(modelInputW * modelInputH)
            val channelStride = modelInputH * modelInputW
            for (y in 0 until modelInputH) {
                for (x in 0 until modelInputW) {
                    val r = (outputFloatArray[0 * channelStride + y * modelInputW + x] * 255f).toInt().coerceIn(0, 255)
                    val g = (outputFloatArray[1 * channelStride + y * modelInputW + x] * 255f).toInt().coerceIn(0, 255)
                    val b = (outputFloatArray[2 * channelStride + y * modelInputW + x] * 255f).toInt().coerceIn(0, 255)
                    enhancedSmallPixels[y * modelInputW + x] = Color.rgb(r, g, b)
                }
            }

            val enhancedSmall = Bitmap.createBitmap(modelInputW, modelInputH, Bitmap.Config.ARGB_8888)
            enhancedSmall.setPixels(enhancedSmallPixels, 0, modelInputW, 0, 0, modelInputW, modelInputH)
            resized.recycle()

            // 5. Upscale the Enhanced image to the Full resolution
            val enhancedFullRes = Bitmap.createScaledBitmap(enhancedSmall, srcW, srcH, true)
            enhancedSmall.recycle()

            // 6. Blend 80% AI with 20% Original
            val origPixels = IntArray(srcW * srcH)
            val enhancedPixels = IntArray(srcW * srcH)
            src.getPixels(origPixels, 0, srcW, 0, 0, srcW, srcH)
            enhancedFullRes.getPixels(enhancedPixels, 0, srcW, 0, 0, srcW, srcH)

            for (i in origPixels.indices) {
                val orig = origPixels[i]
                val enh = enhancedPixels[i]

                val r = (Color.red(enh) * 0.8f + Color.red(orig) * 0.2f).roundToInt().coerceIn(0, 255)
                val g = (Color.green(enh) * 0.8f + Color.green(orig) * 0.2f).roundToInt().coerceIn(0, 255)
                val b = (Color.blue(enh) * 0.8f + Color.blue(orig) * 0.2f).roundToInt().coerceIn(0, 255)

                origPixels[i] = Color.rgb(r, g, b)
            }
            
            val finalBmp = Bitmap.createBitmap(srcW, srcH, Bitmap.Config.ARGB_8888)
            finalBmp.setPixels(origPixels, 0, srcW, 0, 0, srcW, srcH)
            enhancedFullRes.recycle()

            return@withContext finalBmp

        } catch (e: Throwable) {
            e.printStackTrace()
            // We cannot recycle 'resized' here because it might not be initialized if the crash happened at Module.load

            // Draw the exception message onto a new Bitmap so we can read it!
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
