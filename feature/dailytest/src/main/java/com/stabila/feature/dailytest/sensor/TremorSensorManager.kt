package com.stabila.feature.dailytest.sensor

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

/**
 * Wraps Android's SensorManager to provide a reactive stream of combined accelerometer
 * and gyroscope magnitude data at SENSOR_DELAY_GAME (~20ms / 50Hz).
 */
@Singleton
class TremorSensorManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    /**
     * Represents a single fused data point.
     */
    data class SensorDataPoint(
        val timestampMs: Long,
        val accelMagnitude: Float,
        val gyroMagnitude: Float
    )

    fun getSensorDataFlow(): Flow<SensorDataPoint> = callbackFlow {
        var lastAccel = FloatArray(3)
        var lastGyro = FloatArray(3)
        var accelReady = false
        var gyroReady = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event ?: return
                
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        System.arraycopy(event.values, 0, lastAccel, 0, event.values.size)
                        accelReady = true
                    }
                    Sensor.TYPE_GYROSCOPE -> {
                        System.arraycopy(event.values, 0, lastGyro, 0, event.values.size)
                        gyroReady = true
                    }
                }

                if (accelReady) {
                    // Compute Euclidean magnitude
                    val accelMag = sqrt(lastAccel[0] * lastAccel[0] + lastAccel[1] * lastAccel[1] + lastAccel[2] * lastAccel[2])
                    
                    // Use gyro magnitude if available, otherwise default to 0
                    val gyroMag = if (gyroReady) {
                        sqrt(lastGyro[0] * lastGyro[0] + lastGyro[1] * lastGyro[1] + lastGyro[2] * lastGyro[2])
                    } else {
                        0f
                    }

                    // Android's gravity vector magnitude is ~9.81 m/s^2. We subtract it approximately to get linear acceleration.
                    // (A high-pass filter in SignalProcessor will properly remove gravity DC bias).
                    trySend(
                        SensorDataPoint(
                            timestampMs = System.currentTimeMillis(),
                            accelMagnitude = accelMag,
                            gyroMagnitude = gyroMag
                        )
                    )
                    
                    // Reset flags to wait for next events
                    accelReady = false
                    gyroReady = false
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Not needed
            }
        }

        accelerometer?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gyroscope?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
