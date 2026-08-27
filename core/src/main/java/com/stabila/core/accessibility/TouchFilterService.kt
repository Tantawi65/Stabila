package com.stabila.core.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.stabila.core.data.UserPreferencesDataStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

@AndroidEntryPoint
class TouchFilterService : AccessibilityService(), View.OnTouchListener {

    @Inject
    lateinit var userPrefs: UserPreferencesDataStore

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var isEnabled = false
    private var tremorRadius = 150f
    private var touchSlop = 24f
    private var isDispatching = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        touchSlop = android.view.ViewConfiguration.get(this).scaledTouchSlop.toFloat()
        
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
                // Use a tighter bound to allow small scrolls without them registering as taps
                tremorRadius = max(radius * 1.25f, touchSlop * 1.5f)
            }
        }

        scope.launch {
            AutoScrollState.isScrollingFlow.collect { isAutoScrolling ->
                if (isEnabled) {
                    setOverlayTouchable(!isAutoScrolling)
                }
            }
        }
    }

    private fun showOverlay() {
        if (overlayView != null) return

        overlayView = View(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setOnTouchListener(this@TouchFilterService)
        }

        val initialFlags = if (AutoScrollState.isScrolling) {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        } else {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            initialFlags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        windowManager?.addView(overlayView, params)
    }

    private fun hideOverlay() {
        if (overlayView != null) {
            windowManager?.removeView(overlayView)
            overlayView = null
        }
    }
    
    private fun setOverlayTouchable(touchable: Boolean) {
        if (overlayView == null) return
        val params = overlayView!!.layoutParams as WindowManager.LayoutParams
        if (touchable) {
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        } else {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        windowManager?.updateViewLayout(overlayView, params)
    }

    // Touch Tracking State
    private val currentPoints = mutableListOf<Pair<Float, Float>>()
    private var isCurrentlySwiping = false
    private var swipePath: Path? = null
    private var swipeStartTime = 0L

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if (event == null || !isEnabled || isDispatching || AutoScrollState.isScrolling) return false

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
                currentPoints.add(event.rawX to event.rawY)
                swipePath?.lineTo(event.rawX, event.rawY)
                
                if (!isCurrentlySwiping) {
                    val startX = currentPoints.first().first
                    val startY = currentPoints.first().second
                    val dist = sqrt((event.rawX - startX) * (event.rawX - startX) + (event.rawY - startY) * (event.rawY - startY))
                    
                    // Smart Auto-Detect: If they exceed the tremor radius, they are swiping!
                    if (dist > tremorRadius) {
                        isCurrentlySwiping = true
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (isCurrentlySwiping) {
                    // Dispatch a straight line swipe to trigger normal scroll/fling
                    dispatchRecordedSwipe(event.rawX, event.rawY)
                } else {
                    // It stayed within the tremor radius -> It's a Jittery Tap!
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
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 80)) // 80ms tap
            .build()

        isDispatching = true
        setOverlayTouchable(false)
        
        // Wait for WindowManager to asynchronously apply the FLAG_NOT_TOUCHABLE before dispatching
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    if (isEnabled) setOverlayTouchable(true)
                    isDispatching = false
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    if (isEnabled) setOverlayTouchable(true)
                    isDispatching = false
                }
            }, null)
        }, 50L)
    }

    private fun dispatchRecordedSwipe(endX: Float, endY: Float) {
        val startX = currentPoints.first().first
        val startY = currentPoints.first().second
        
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        
        // Dynamic duration based on their physical swipe speed to retain native fling momentum
        val elapsedMs = System.currentTimeMillis() - swipeStartTime
        val duration = elapsedMs.coerceIn(80L, 400L)
        
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()

        isDispatching = true
        setOverlayTouchable(false)
        
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    if (isEnabled) setOverlayTouchable(true)
                    isDispatching = false
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    if (isEnabled) setOverlayTouchable(true)
                    isDispatching = false
                }
            }, null)
        }, 50L)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    
    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
    }
}
