package com.stabila.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.stabila.core.data.UserPreferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.sqrt

class TouchFilterManager(
    private val accessibilityService: AccessibilityService,
    private val userPrefs: UserPreferencesDataStore
) : View.OnTouchListener {

    private val windowManager = accessibilityService.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    
    private var isEnabled = false
    private var tremorRadius = 150f
    private var touchSlop = 24f
    private var isDispatching = false

    // We need to know if Auto-Scroll is currently injecting a gesture, to avoid intercepting it.
    var isAutoScrolling = false
        set(value) {
            field = value
            if (value) {
                // Auto-scroll started (State A) -> Step aside completely
                hideOverlay()
            } else {
                // Auto-scroll paused (State B) -> Resume stabilizer
                if (isEnabled) {
                    showOverlay()
                }
            }
        }

    init {
        touchSlop = android.view.ViewConfiguration.get(accessibilityService).scaledTouchSlop.toFloat()
        
        scope.launch {
            userPrefs.touchStabilizerEnabled.collect { enabled ->
                isEnabled = enabled
                if (enabled) {
                    showOverlay()
                } else {
                    hideOverlay()
                }
            }
        }
        
        scope.launch {
            userPrefs.touchTremorRadius.collect { radius ->
                tremorRadius = max(radius * 1.25f, touchSlop * 1.5f)
            }
        }
    }

    private fun showOverlay() {
        if (overlayView != null) return

        overlayView = View(accessibilityService).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setOnTouchListener(this@TouchFilterManager)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        windowManager.addView(overlayView, params)
    }

    private fun hideOverlay() {
        if (overlayView != null) {
            windowManager.removeView(overlayView)
            overlayView = null
        }
    }
    
    fun setOverlayTouchable(touchable: Boolean) {
        if (overlayView == null) return
        val params = overlayView!!.layoutParams as WindowManager.LayoutParams
        if (touchable) {
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        } else {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        windowManager.updateViewLayout(overlayView, params)
    }

    // Touch Tracking State
    private val currentPoints = mutableListOf<Pair<Float, Float>>()
    private var isCurrentlySwiping = false
    private var swipePath: Path? = null
    private var swipeStartTime = 0L

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if (event == null || !isEnabled || isDispatching || isAutoScrolling) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                currentPoints.clear()
                currentPoints.add(event.rawX to event.rawY)
                isCurrentlySwiping = false
                swipePath = Path().apply { moveTo(event.rawX, event.rawY) }
                swipeStartTime = System.currentTimeMillis()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                // If it's a multi-touch, we might want to just record the primary pointer
                currentPoints.add(event.rawX to event.rawY)
                swipePath?.lineTo(event.rawX, event.rawY)
                
                if (!isCurrentlySwiping) {
                    val startX = currentPoints.first().first
                    val startY = currentPoints.first().second
                    val dist = sqrt((event.rawX - startX) * (event.rawX - startX) + (event.rawY - startY) * (event.rawY - startY))
                    
                    if (dist > tremorRadius) {
                        isCurrentlySwiping = true
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (isCurrentlySwiping) {
                    dispatchRecordedSwipe()
                } else {
                    val avgX = currentPoints.map { it.first }.average().toFloat()
                    val avgY = currentPoints.map { it.second }.average().toFloat()
                    dispatchStabilizedTap(avgX, avgY)
                }
                currentPoints.clear()
                swipePath = null
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                currentPoints.clear()
                isCurrentlySwiping = false
                swipePath = null
                return true
            }
        }
        return false
    }

    private fun dispatchStabilizedTap(x: Float, y: Float) {
        val path = Path().apply { 
            moveTo(x, y) 
            // Add a tiny lineTo to prevent 0-length path crashes on some Android versions
            lineTo(x + 0.1f, y)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
            .build()

        isDispatching = true
        setOverlayTouchable(false)
        
        Handler(Looper.getMainLooper()).postDelayed({
            accessibilityService.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    if (isEnabled) setOverlayTouchable(true)
                    isDispatching = false
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    if (isEnabled) setOverlayTouchable(true)
                    isDispatching = false
                }
            }, null)
        }, 30L)
    }

    private fun dispatchRecordedSwipe() {
        if (swipePath == null) return
        
        val elapsedMs = System.currentTimeMillis() - swipeStartTime
        // Preserve actual duration up to 2 seconds so slow scrolls don't become violent flings
        val duration = elapsedMs.coerceIn(50L, 2000L)
        
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(swipePath!!, 0, duration))
            .build()

        isDispatching = true
        setOverlayTouchable(false)
        
        Handler(Looper.getMainLooper()).postDelayed({
            accessibilityService.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    if (isEnabled) setOverlayTouchable(true)
                    isDispatching = false
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    if (isEnabled) setOverlayTouchable(true)
                    isDispatching = false
                }
            }, null)
        }, 30L)
    }

    fun onDestroy() {
        hideOverlay()
    }
}
