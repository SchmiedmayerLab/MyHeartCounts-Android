//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.firebase

import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.grovealliance.core.ConfigurationBuilder
import org.grovealliance.core.GroveDsl

/**
 * Convenience for registering the [FirebaseAppConfiguration] module to the Grove configuration.
 *
 * Register this before any other Firebase-backed module. Pass [options] to initialize the default
 * `FirebaseApp` during start-up, or omit them to defer initialization until
 * [FirebaseAppConfiguration.configure] is called.
 *
 * @param options The options to initialize the default `FirebaseApp` with, or `null` to defer.
 * @param onInitialized Setup that has to run before any other Firebase API is used, such as
 * `FirebaseFirestore.setFirestoreSettings`. See [FirebaseAppConfiguration].
 */
@GroveDsl
fun ConfigurationBuilder.firebaseApp(
    options: FirebaseOptions? = null,
    onInitialized: (FirebaseApp) -> Unit = {},
) {
    module<FirebaseAppConfiguration> {
        FirebaseAppConfiguration(options = options, onInitialized = onInitialized)
    }
}
