package com.stabila.feature.camera.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class TremorTroughDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    /**
     * Emits true when a "trough" (stable moment) is detected.
     * We define a trough as a moment when the acceleration magnitude is very close to 9.81 m/s^2.
     */
    fun detectTrough(timeoutMs: Long = 2000L): Flow<Boolean> = callbackFlow {
        val startTime = System.currentTimeMillis()
        var troughFound = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (troughFound) return

                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

                val deviation = Math.abs(magnitude - 9.81f)
                
                // If deviation is very small (< 0.5 m/s^2), we are at a trough
                // Or if we exceed timeout, we just fire anyway
                if (deviation < 0.5f || System.currentTimeMillis() - startTime > timeoutMs) {
                    troughFound = true
                    trySend(true)
                    close()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(
            listener,
            accelerometer,
            SensorManager.SENSOR_DELAY_FASTEST
        )

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
