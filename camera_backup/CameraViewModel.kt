package com.stabila.feature.camera

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stabila.feature.camera.processing.ImageProcessor
import com.stabila.feature.camera.sensor.TremorTroughDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    application: Application,
    private val troughDetector: TremorTroughDetector,
    private val imageProcessor: ImageProcessor
) : AndroidViewModel(application) {

    enum class CameraState {
        IDLE,           // Ready to take a picture
        STABILIZING,    // Waiting for tremor trough
        PROCESSING,     // Deblurring the captured image
        DONE            // Showing the result
    }

    data class UiState(
        val cameraState: CameraState = CameraState.IDLE,
        val resultBitmap: Bitmap? = null,
        val savedToGallery: Boolean = false,
        val isCompareMode: Boolean = false,
        val originalBitmap: Bitmap? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun captureStarted() {
        _uiState.value = _uiState.value.copy(cameraState = CameraState.STABILIZING)
    }

    /**
     * Toggles the before/after comparison mode
     */
    fun toggleCompareMode() {
        _uiState.value = _uiState.value.copy(
            isCompareMode = !_uiState.value.isCompareMode
        )
    }

    fun calculateSharpnessBlocking(bitmap: Bitmap): Float {
        return imageProcessor.calculateSharpnessBlocking(bitmap)
    }

    /**
     * Process the dynamically captured Pro frames along with the Normal "Before" frame.
     */
    fun onBurstCaptured(bestProBitmap: Bitmap?, normalBitmap: Bitmap?) {
        if (bestProBitmap == null) {
            reset()
            return
        }

        _uiState.value = _uiState.value.copy(cameraState = CameraState.PROCESSING)

        viewModelScope.launch {
            // User requested the 'Before' image to be the normal (light mode) photo again
            val original = if (_uiState.value.isCompareMode) normalBitmap else null

            //  STEP 2 & 3: TFLite Zero-DCE Low-Light Enhancement 
            // This replaces both the manual brightness boost and the old super-resolution model,
            // providing natural lighting curves while preserving 100% of original detail!
            val zeroDceEnhanced = imageProcessor.applyZeroDCEEnhancement(bestProBitmap)

            //  Final: mild sharpening pass then save 
            val processed = imageProcessor.applyDeblurFilter(zeroDceEnhanced)
            val saved = saveToGallery(processed)

            _uiState.value = _uiState.value.copy(
                cameraState = CameraState.DONE,
                resultBitmap = processed,
                originalBitmap = original,
                savedToGallery = saved
            )

            // Clean up raw frames and intermediates
            if (normalBitmap != null && normalBitmap != original) normalBitmap.recycle()
            if (zeroDceEnhanced !== processed && zeroDceEnhanced !== original && zeroDceEnhanced !== bestProBitmap) zeroDceEnhanced.recycle()
            if (bestProBitmap !== processed && bestProBitmap !== original) bestProBitmap.recycle()

        }
    }

    /**
     * Saves a bitmap to the device's Pictures gallery using MediaStore.
     * Works on Android 10+ without needing WRITE_EXTERNAL_STORAGE permission.
     */
    private suspend fun saveToGallery(bitmap: Bitmap): Boolean = withContext(Dispatchers.IO) {
        val context = getApplication<Application>()
        val filename = "Stabila_${System.currentTimeMillis()}.jpg"

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Stabila")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: return@withContext false

        try {
            resolver.openOutputStream(uri)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
            } ?: return@withContext false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            resolver.delete(uri, null, null)
            false
        }
    }

    fun reset() {
        val currentCompareMode = _uiState.value.isCompareMode
        _uiState.value = UiState(isCompareMode = currentCompareMode)
    }
}
