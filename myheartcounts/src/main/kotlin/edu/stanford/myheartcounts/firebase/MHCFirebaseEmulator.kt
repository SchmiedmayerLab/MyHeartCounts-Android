//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.firebase

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import edu.stanford.myheartcounts.BuildConfig
import org.grovealliance.account.firebase.FirebaseEmulatorSettings
import org.grovealliance.core.logging.groveLogger

/**
 * Points the app at a locally running Firebase emulator suite instead of a real project.
 *
 * This is how the app's Firebase paths are exercised without creating real participants in
 * `myheart-counts-development`, and it runs against the study's actual `firestore.rules` and
 * `firebasestorage.rules`, so a rule that would reject a write in production rejects it here too.
 *
 * Off unless the build supplies a host:
 *
 * ```
 * ./gradlew :myheartcounts:installDebug -Pmhc.firebaseEmulatorHost=10.0.2.2
 * ```
 *
 * The host is a build input rather than a debug-build default on purpose — a debug build is also
 * how the app is run against the real development project, and silently redirecting that would make
 * data appear to vanish. `10.0.2.2` is the address an Android emulator reaches its host machine on.
 *
 * The ports are those in `MyHeartCounts-Firebase/firebase.json`.
 */
internal object MHCFirebaseEmulator {

    private val logger by groveLogger(tag = "MHCFirebase")

    /**
     * The emulator host this build targets, or `null` when it targets a real Firebase project.
     */
    val host: String? get() = BuildConfig.FIREBASE_EMULATOR_HOST.takeIf { it.isNotEmpty() }

    /**
     * The Firestore emulator's port; see [redirectIfConfigured] for why Firestore is not redirected
     * here along with the rest.
     */
    const val FIRESTORE_PORT = 8080

    /**
     * Redirects Cloud Storage and Cloud Functions, if this build targets an emulator.
     *
     * Firestore is deliberately absent: `FirebaseFirestore.useEmulator` folds the host into the
     * *current* settings, so replacing those settings afterwards would silently drop the redirect.
     * [MHCFirestoreSetup] therefore redirects Firestore itself, immediately after applying them.
     *
     * Must run before anything touches Storage or Functions — `useEmulator` throws once their
     * clients have started.
     */
    fun redirectIfConfigured() {
        val host = host ?: return
        logger.i { "Targeting the Firebase emulator suite on '$host'." }
        FirebaseStorage.getInstance().useEmulator(host, STORAGE_PORT)
        FirebaseFunctions.getInstance().useEmulator(host, FUNCTIONS_PORT)
    }

    /**
     * The Auth emulator settings for Grove's `FirebaseAccountService`, or `null` when this build
     * targets a real Firebase project.
     */
    fun authSettings(): FirebaseEmulatorSettings? =
        host?.let { FirebaseEmulatorSettings(host = it, port = AUTH_PORT) }

    private const val AUTH_PORT = 9099
    private const val FUNCTIONS_PORT = 5001
    private const val STORAGE_PORT = 9199
}
