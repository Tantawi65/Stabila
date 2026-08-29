package com.stabila.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent

import com.stabila.core.data.UserPreferencesDataStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.math.max

@AndroidEntryPoint
class StabilaAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var isInjectingGesture = false
    }

    @Inject
    lateinit var userPreferences: UserPreferencesDataStore

    @Inject
    lateinit var tremorReadingDao: com.stabila.core.data.db.TremorReadingDao

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    private lateinit var overlayManager: OverlayManager
    private lateinit var touchFilterManager: TouchFilterManager

    private var currentPackageName: String = ""
    private var isScrolling = false
    private var scrollJob: Job? = null
    
    // We fetch these from DataStore when entering a new app or on resume
    private var currentScrollSpeed = 3f
    private var enabledApps = setOf<String>()
    private var isMasterEnabled = true
    
    private var latestTremorScore = 0f

    private var screenStateReceiver: android.content.BroadcastReceiver? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlayManager = OverlayManager(this)
        touchFilterManager = TouchFilterManager(this, userPreferences)

        val filter = android.content.IntentFilter().apply {
            addAction(android.content.Intent.ACTION_SCREEN_OFF)
            addAction(android.content.Intent.ACTION_USER_PRESENT)
        }
        screenStateReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                when (intent?.action) {
                    android.content.Intent.ACTION_SCREEN_OFF -> {
                        stopScrollingAndHideUI()
                        currentPackageName = ""
                    }
                    android.content.Intent.ACTION_USER_PRESENT -> {
                        // When unlocked, the foreground app will trigger a WINDOW_STATE_CHANGED event,
                        // and since we reset currentPackageName to empty, it will successfully trigger onAppSwitched.
                    }
                }
            }
        }
        registerReceiver(screenStateReceiver, filter)
        
        scope.launch {
            userPreferences.isAutoScrollMasterEnabled.collect { enabled ->
                isMasterEnabled = enabled
                if (currentPackageName.isNotEmpty()) {
                    onAppSwitched(currentPackageName)
                }
            }
        }

        scope.launch {
            userPreferences.enabledScrollApps.collect { apps ->
                enabledApps = apps
                if (currentPackageName.isNotEmpty()) {
                    onAppSwitched(currentPackageName)
                }
            }
        }

        scope.launch {
            userPreferences.autoScrollSpeed.collect { speed ->
                currentScrollSpeed = speed
            }
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
        val keyguardManager = getSystemService(android.content.Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
        if (keyguardManager.isKeyguardLocked) {
            stopScrollingAndHideUI()
            return
        }

        scope.launch {
            val isAppEnabled = isMasterEnabled && (enabledApps.contains(packageName) || enabledApps.contains("all"))

            if (isAppEnabled) {
                // If auto-scroll is already running, keep scrolling!
                // Only enter paused state (State B) if auto-scroll was NOT actively running.
                if (!isScrolling) {
                    enterStateB()
                }
            } else {
                stopScrollingAndHideUI()
            }
        }
    }

    private fun enterStateB() {
        isScrolling = false
        touchFilterManager.isAutoScrolling = false
        
        scrollJob?.cancel()
        
        overlayManager.hideInvisibleShield()
        
        scope.launch {
            val savedX = userPreferences.autoScrollButtonX.first()
            val savedY = userPreferences.autoScrollButtonY.first()
            val tremorScore = latestTremorScore

            overlayManager.showFloatingButton(
                tremorScore = tremorScore,
                savedX = savedX,
                savedY = savedY,
                isScrolling = false,
                onToggleScroll = { toggleScrollingState() },
                onPositionSaved = { x, y ->
                    scope.launch {
                        userPreferences.setAutoScrollButtonPosition(x, y)
                    }
                }
            )
        }
    }

    private fun enterStateA() {
        isScrolling = true
        touchFilterManager.isAutoScrolling = true
        
        
        scope.launch {
            val savedX = userPreferences.autoScrollButtonX.first()
            val savedY = userPreferences.autoScrollButtonY.first()
            val tremorScore = latestTremorScore

            // Disappear floating button when playing
            overlayManager.hideFloatingButton()

            // Show the invisible shield that acts as the emergency brake
            overlayManager.showInvisibleShield {
                // If the user taps anywhere on screen, stop scrolling and return to State B
                enterStateB()
            }

            startScrollingLoop()
        }
    }

    private fun toggleScrollingState() {
        if (isScrolling) {
            enterStateB()
        } else {
            enterStateA()
        }
    }

    private fun stopScrollingAndHideUI() {
        isScrolling = false
        touchFilterManager.isAutoScrolling = false
        
        scrollJob?.cancel()
        overlayManager.hideAll()
    }

    private fun startScrollingLoop() {
        scrollJob?.cancel()
        scrollJob = scope.launch {
            while (isScrolling) {
                dispatchScrollGesture()
                
                // Fixed minimal delay to allow system to process gesture callbacks cleanly
                delay(10L)
            }
        }
    }

    private suspend fun dispatchScrollGesture() {
        if (!isScrolling) return

        val displayMetrics = resources.displayMetrics
        val middleX = displayMetrics.widthPixels / 2f
        val centerY = displayMetrics.heightPixels / 2f

        // Map current setting level to the exact px/s requested by the user
        val targetSpeedPxPerSec = when (currentScrollSpeed.toInt()) {
            1 -> 150f
            2 -> 300f
            3 -> 400f
            4 -> 750f
            5 -> 1000f
            else -> 1000f
        }

        // We use a fixed duration for each stroke to ensure smooth continuous increments.
        val durationMs = 200L
        val interStepDelayMs = 10L // Matching the delay(10L) in startScrollingLoop
        val cycleMs = durationMs + interStepDelayMs

        // Math for target speed (px/s)
        val calculatedDistance = (targetSpeedPxPerSec * cycleMs) / 1000f
        val minDistancePx = 40f * displayMetrics.density
        val strokeDistance = max(calculatedDistance, minDistancePx)

        val startY = centerY + (strokeDistance / 2f)
        val endY = centerY - (strokeDistance / 2f)

        val path = Path().apply {
            moveTo(middleX, startY)
            lineTo(middleX, endY)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()

        val stepCompleted = CompletableDeferred<Unit>()

        isInjectingGesture = true

        dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                isInjectingGesture = false
                stepCompleted.complete(Unit)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                isInjectingGesture = false
                stepCompleted.complete(Unit)
            }
        }, null)

        // Wait for current micro-step stroke to finish executing before initiating next increment
        withTimeoutOrNull(durationMs + 100L) {
            stepCompleted.await()
        }
    }

    override fun onInterrupt() {
        stopScrollingAndHideUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        
        screenStateReceiver?.let { unregisterReceiver(it) }
        
        job.cancel()
        overlayManager.hideAll()
    }
}
