//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT
//

package edu.stanford.myheartcounts.standard.health

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.StateFlow
import org.grovealliance.core.Module
import org.grovealliance.core.logging.groveLogger
import org.grovealliance.health.Health
import org.grovealliance.health.HealthConnectAvailability
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicReference

/**
 * The participant's access to Health Connect: whether it can be used, and asking for the permissions
 * the study collects with.
 */
interface HealthAccess : Module {
    /**
     * Whether Health Connect can be used on this device.
     */
    val availability: StateFlow<HealthConnectAvailability>

    /**
     * Requests every permission the study needs that isn't granted yet, and suspends until the
     * participant closes the Health Connect permission screen.
     *
     * @return `true` if every permission is granted afterwards, `false` otherwise, including when no
     * screen of the app is in the foreground to ask from.
     */
    suspend fun requestPermissions(): Boolean

    /**
     * Opens the store listing where the participant can install or update Health Connect.
     *
     * @return `true` if a store listing was opened.
     */
    fun installHealthConnect(): Boolean
}

/**
 * The [HealthAccess] backed by Grove's [Health], asking from whichever activity of the app is in the
 * foreground.
 */
class HealthAccessImpl(
    private val health: Health,
    private val application: Application,
) : HealthAccess {
    private val logger by groveLogger()
    private val foregroundActivity = AtomicReference<WeakReference<FragmentActivity>?>(null)

    override val availability: StateFlow<HealthConnectAvailability>
        get() = health.availability

    override fun configure() {
        application.registerActivityLifecycleCallbacks(ForegroundActivityTracker())
    }

    override suspend fun requestPermissions(): Boolean {
        val activity = foregroundActivity.get()?.get() ?: run {
            logger.w { "No activity in the foreground to request Health Connect permissions from" }
            return false
        }
        return health.requestPermissionsIfNeeded(activity = activity)
    }

    override fun installHealthConnect(): Boolean = health.installOrUpdateHealthConnect()

    private inner class ForegroundActivityTracker : Application.ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity) {
            if (activity is FragmentActivity) foregroundActivity.set(WeakReference(activity))
        }

        override fun onActivityDestroyed(activity: Activity) {
            val current = foregroundActivity.get()
            if (current?.get() === activity) foregroundActivity.compareAndSet(current, null)
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityStarted(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    }
}
