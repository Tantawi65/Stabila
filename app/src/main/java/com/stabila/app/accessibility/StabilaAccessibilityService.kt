package com.stabila.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.stabila.core.data.UserPreferencesDataStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max

@AndroidEntryPoint
class StabilaAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var userPreferences: UserPreferencesDataStore

    @Inject
    lateinit var tremorReadingDao: com.stabila.core.data.db.TremorReadingDao

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    private lateinit var overlayManager: OverlayManager

    private var currentPackageName: String = ""
    private var isScrolling = false
    private var scrollJob: Job? = null
    
    // We fetch these from DataStore when entering a new app or on resume
    private var currentScrollSpeed = 3f
    private var enabledApps = setOf<String>()
    
    // Store the latest score
    private var latestTremorScore = 0f

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlayManager = OverlayManager(this)
        
        scope.launch {
            currentScrollSpeed = userPreferences.autoScrollSpeed.first()
            enabledApps = userPreferences.enabledScrollApps.first()
        }
        
        scope.launch {
            tremorReadingDao.getRecentReadings().collect { readings ->
                if (readings.isNotEmpty()) {
                    latestTremorScore = readings.first().score
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            
            // EMERGENCY BRAKE ON INFINITE LOOP:
            // When our own transparent shields or start buttons appear, Android fires this event.
            // If we process it, we hide the button, which fires another event, creating a loop that glitches the button.
            // We MUST completely ignore events from our own app and the system UI.
            if (packageName == "com.android.systemui" || packageName == this.packageName) {
                return
            }

            if (packageName != currentPackageName) {
                currentPackageName = packageName
                onAppSwitched(packageName)
            }
        }
    }

    private fun onAppSwitched(packageName: String) {
        scope.launch {
            // Refresh preferences
            enabledApps = userPreferences.enabledScrollApps.first()
            currentScrollSpeed = userPreferences.autoScrollSpeed.first()

            val isAppEnabled = enabledApps.contains(packageName) || enabledApps.contains("all")

            if (isAppEnabled) {
                // Feature automatically disabled (State B) when entering any app for the first time
                enterStateB()
            } else {
                stopScrollingAndHideUI()
            }
        }
    }

    private fun enterStateB() {
        isScrolling = false
        scrollJob?.cancel()
        
        overlayManager.hideInvisibleShield()
        
        // Get real tremor score fetched from database
        val tremorScore = latestTremorScore 
        
        overlayManager.showResumePill(tremorScore) {
            // On pill clicked, enter State A
            enterStateA()
        }
    }

    private fun enterStateA() {
        isScrolling = true
        overlayManager.hideResumePill()
        
        // Show the invisible shield that acts as the emergency brake
        overlayManager.showInvisibleShield {
            // If the user taps anywhere, stop scrolling and return to State B
            enterStateB()
        }

        startScrollingLoop()
    }

    private fun stopScrollingAndHideUI() {
        isScrolling = false
        scrollJob?.cancel()
        overlayManager.hideAll()
    }

    private fun startScrollingLoop() {
        scrollJob?.cancel()
        scrollJob = scope.launch {
            while (isScrolling) {
                dispatchScrollGesture()
                
                // Delay based on speed. 
                // Speed 1 (slow) -> 3000ms delay
                // Speed 5 (fast) -> 600ms delay
                val baseDelay = 3600L
                val delayMs = max(200L, baseDelay - (currentScrollSpeed * 600L).toLong())
                delay(delayMs)
            }
        }
    }

    private fun dispatchScrollGesture() {
        val displayMetrics = resources.displayMetrics
        val middleX = displayMetrics.widthPixels / 2f
        val startY = displayMetrics.heightPixels * 0.7f
        val endY = displayMetrics.heightPixels * 0.3f // Scroll up by swiping up

        val path = Path().apply {
            moveTo(middleX, startY)
            lineTo(middleX, endY)
        }

        // Duration of swipe is also tied to speed for smoothness
        // Speed 1 -> 2000ms duration (very slow)
        // Speed 5 -> 400ms duration (fast)
        val durationMs = max(200L, 2400L - (currentScrollSpeed * 400L).toLong())

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()

        dispatchGesture(gesture, null, null)
    }

    override fun onInterrupt() {
        stopScrollingAndHideUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        overlayManager.hideAll()
    }
}
