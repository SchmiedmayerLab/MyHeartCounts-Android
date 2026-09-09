//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.firebase.internal

import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import org.grovealliance.core.ApplicationModule
import org.grovealliance.core.dependency
import org.grovealliance.firebase.FirebaseAppConfiguration
import org.grovealliance.firebase.firebaseLogger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class FirebaseAppConfigurationImpl(
    private val options: FirebaseOptions?,
    private val onInitialized: (FirebaseApp) -> Unit,
) : FirebaseAppConfiguration {

    private val appModule by dependency<ApplicationModule>()
    private val logger by firebaseLogger()

    private val _isConfigured = MutableStateFlow(value = false)
    override val isConfigured: StateFlow<Boolean> = _isConfigured.asStateFlow()

    /**
     * Serializes [configure] so two callers racing on start-up cannot both attempt to initialize
     * the default app.
     */
    private val lock = ReentrantLock()

    override fun configure() {
        val options = options ?: run {
            logger.i { "No options supplied; deferring Firebase initialization until configure(options) is called." }
            // The default app may already exist: either `FirebaseInitProvider` was left in place, or
            // the application initialized it itself before Grove started. It was not this module
            // that initialized it, so `onInitialized` deliberately does not run.
            if (existingApp() != null) _isConfigured.value = true
            return
        }
        configure(options = options)
    }

    override fun configure(options: FirebaseOptions): Result<FirebaseApp> = lock.withLock {
        existingApp()?.let { app ->
            _isConfigured.value = true
            return Result.success(app)
        }
        runCatching {
            val app = FirebaseApp.initializeApp(appModule.requireContext(), options)
            // Deliberately before the flip below: a caller resuming from `awaitConfigured` must not
            // be able to touch a Firebase API ahead of the setup that has to precede it.
            onInitialized(app)
            app
        }.onSuccess { app ->
            logger.i { "Firebase configured for project '${app.options.projectId}'." }
            _isConfigured.value = true
        }.onFailure { throwable ->
            logger.e(throwable) { "Failed to configure Firebase for project '${options.projectId}'." }
        }
    }

    override suspend fun awaitConfigured() {
        isConfigured.first { it }
    }

    /**
     * Returns the already initialized default app, or `null` when none exists yet.
     */
    private fun existingApp(): FirebaseApp? =
        runCatching { FirebaseApp.getInstance() }.getOrNull()
}
