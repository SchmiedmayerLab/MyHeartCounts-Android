//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.firebase

import edu.stanford.myheartcounts.BuildConfig
import org.grovealliance.core.ConfigurationBuilder
import org.grovealliance.core.GroveDsl
import org.grovealliance.firebase.firebaseApp

/**
 * Registers Firebase and the components that decide which project it talks to.
 *
 * Register before every Firebase-backed module, so they can await initialization. On a first launch
 * no region has been picked yet, so Firebase stays uninitialized until [MHCFirebaseRegionInitializer]
 * supplies the options mid-onboarding.
 */
@GroveDsl
fun ConfigurationBuilder.mhcFirebase() {
    firebaseApp(
        options = { dependency<MHCFirebaseLoader>().persistedRegionOptions() },
        onInitialized = {
            dependency<MHCFirebaseEmulator>().redirectIfConfigured()
            dependency<MHCFirestoreSetup>().apply()
        },
    )

    singleton {
        MHCFirebaseLoader(
            context = appContext(),
            storageFactory = dependency(),
        )
    }

    singleton { MHCFirebaseEmulator(host = BuildConfig.FIREBASE_EMULATOR_HOST.takeIf { it.isNotEmpty() }) }

    singleton {
        MHCFirestoreSetup(
            firebaseLoader = dependency(),
            emulator = dependency(),
        )
    }

    singleton<MHCFirebaseRegionInitializer> {
        MHCFirebaseRegionInitializerImpl(
            firebaseLoader = dependency(),
            firebaseAppConfiguration = dependency(),
            studyManager = dependency(),
        )
    }
}
