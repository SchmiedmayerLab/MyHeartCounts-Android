//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.debug

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import org.grovealliance.core.lifecycle.AppLifecycle
import org.grovealliance.core.time.TimeProvider
import kotlin.math.sqrt

/**
 * Notices when the participant shakes the device while the app is in the foreground.
 */
interface ShakeDetector {

    /**
     * Emits once per shake, while the app is in the foreground and [isEnabled].
     */
    val shakes: Flow<Unit>

    /**
     * Whether shakes are detected. Kept in memory only, so it is `true` again once the app
     * restarts.
     */
    val isEnabled: StateFlow<Boolean>

    /**
     * Turns shake detection on or off for every collector of [shakes], until the app restarts.
     */
    fun setEnabled(enabled: Boolean)
}

/**
 * [ShakeDetector] reading the accelerometer. It listens only while the app is in the foreground, it
 * is enabled and something collects [shakes].
 */
class ShakeDetectorImpl(
    private val context: Context,
    appLifecycle: AppLifecycle,
    private val timeProvider: TimeProvider,
) : ShakeDetector {

    private val enabled = MutableStateFlow(true)

    override val isEnabled: StateFlow<Boolean> = enabled.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    override val shakes: Flow<Unit> = combine(appLifecycle.state, enabled) { state, isEnabled ->
        state == AppLifecycle.State.FOREGROUND && isEnabled
    }.flatMapLatest { isListening ->
        if (isListening) accelerometerShakes() else emptyFlow()
    }

    override fun setEnabled(enabled: Boolean) {
        this.enabled.value = enabled
    }

    private fun accelerometerShakes(): Flow<Unit> = callbackFlow {
        val sensorManager = context.getSystemService(SensorManager::class.java)
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensorManager == null || accelerometer == null) {
            close()
            return@callbackFlow
        }
        var lastShakeMillis = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val (x, y, z) = event.values
                val force = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
                val now = timeProvider.currentTimeMillis()
                if (force > SHAKE_THRESHOLD_G && now - lastShakeMillis > SHAKE_INTERVAL_MILLIS) {
                    lastShakeMillis = now
                    trySend(Unit)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        awaitClose { sensorManager.unregisterListener(listener) }
    }

    private companion object {
        /**
         * The acceleration, in multiples of gravity, above which a movement counts as a shake.
         */
        const val SHAKE_THRESHOLD_G = 2.5f

        /**
         * How long after a shake another one is ignored, so one shake opens the sheet once.
         */
        const val SHAKE_INTERVAL_MILLIS = 1_000L
    }
}
