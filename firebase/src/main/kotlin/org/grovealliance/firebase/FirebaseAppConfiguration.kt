//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.firebase

import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.coroutines.flow.StateFlow
import org.grovealliance.core.Module
import org.grovealliance.firebase.internal.FirebaseAppConfigurationImpl

/**
 * Initializes the default [FirebaseApp] and reports when it is ready to use.
 *
 * Every other Firebase-backed Grove module depends on this one, so that `FirebaseApp` is
 * initialized exactly once and before any Firebase API is touched.
 *
 * ## Eager configuration
 *
 * When the [FirebaseOptions] are known at configuration time, pass them in and the app is
 * initialized during Grove's start-up:
 *
 * ```kotlin
 * override val configuration = Configuration {
 *     firebaseApp(options = myOptions)
 * }
 * ```
 *
 * ## Deferred configuration
 *
 * When the options only become known later — for example because the participant first has to
 * pick a region that determines which Firebase project to talk to — register the module without
 * options and call [configure] once they are available:
 *
 * ```kotlin
 * override val configuration = Configuration {
 *     firebaseApp()
 * }
 *
 * // later, e.g. after region selection
 * val firebase by dependency<FirebaseAppConfiguration>()
 * firebase.configure(options = optionsForSelectedRegion)
 * ```
 *
 * Until [configure] succeeds, [isConfigured] stays `false` and [awaitConfigured] suspends.
 * Modules that must not touch Firebase before then await it:
 *
 * ```kotlin
 * override fun configure() {
 *     ioScope.launch {
 *         firebaseAppConfiguration.awaitConfigured()
 *         // safe to use FirebaseAuth / Firestore / Storage from here on
 *     }
 * }
 * ```
 *
 * ## Setup that must precede every Firebase API call
 *
 * Some Firebase APIs only accept configuration before their client has started —
 * `FirebaseFirestore.setFirestoreSettings` and `FirebaseFirestore.clearPersistence` both throw once
 * anything else has touched Firestore. Awaiting [isConfigured] and doing the setup afterwards is not
 * enough: every awaiting coroutine resumes on the same flip, in an undefined order.
 *
 * Pass such setup as `onInitialized` instead. It runs on the thread that initialized the app, after
 * the default [FirebaseApp] exists and *before* [isConfigured] flips, so no awaiting caller can get
 * in ahead of it. It only runs when this module performed the initialization itself, and a throw
 * from it fails [configure] and leaves the app unconfigured.
 *
 * Note that Android's `FirebaseInitProvider` initializes the default app automatically from a
 * `google-services.json` before any application code runs. Deferred configuration therefore
 * requires removing that provider via the manifest merger:
 *
 * ```xml
 * <provider
 *     android:name="com.google.firebase.provider.FirebaseInitProvider"
 *     android:authorities="${applicationId}.firebaseinitprovider"
 *     tools:node="remove" />
 * ```
 */
interface FirebaseAppConfiguration : Module {

    /**
     * Whether the default [FirebaseApp] has been initialized and Firebase APIs may be used.
     */
    val isConfigured: StateFlow<Boolean>

    /**
     * Initializes the default [FirebaseApp] with the given [options].
     *
     * Calling this more than once is safe: the already initialized app is returned unchanged and
     * [options] are ignored. To point at a different Firebase project, restart the process.
     *
     * @param options The options identifying the Firebase project to connect to.
     * @return A [Result] carrying the initialized [FirebaseApp].
     */
    fun configure(options: FirebaseOptions): Result<FirebaseApp>

    /**
     * Suspends until the default [FirebaseApp] is initialized, returning immediately if it
     * already is.
     */
    suspend fun awaitConfigured()

    companion object {

        /**
         * Creates a [FirebaseAppConfiguration].
         *
         * @param options The options to initialize the default [FirebaseApp] with during Grove's
         * start-up, or `null` to defer initialization until [configure] is called.
         * @param onInitialized Setup that has to run before any other Firebase API is used; see the
         * type-level documentation.
         */
        operator fun invoke(
            options: FirebaseOptions? = null,
            onInitialized: (FirebaseApp) -> Unit = {},
        ): FirebaseAppConfiguration = FirebaseAppConfigurationImpl(
            options = options,
            onInitialized = onInitialized,
        )
    }
}
