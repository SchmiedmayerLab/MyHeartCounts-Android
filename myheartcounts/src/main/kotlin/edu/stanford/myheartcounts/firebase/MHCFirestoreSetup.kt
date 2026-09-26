//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.firebase

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import org.grovealliance.core.logging.groveLogger

/**
 * Prepares Firestore for use, immediately after the Firebase app is initialized.
 *
 * Both steps here have to happen before anything else touches Firestore: changing the settings after
 * the client has started throws, and `clearPersistence` fails with `FAILED_PRECONDITION` on a
 * running client. This is why it runs as `firebaseApp(onInitialized = …)` rather than off the
 * readiness gate — every coroutine awaiting the gate resumes on the same flip and their order is
 * undefined, so "right after Firebase became ready" is not early enough.
 *
 * Mirrors iOS's `DeferredConfigLoading.firestore` and `FirestoreCacheCleanup`.
 */
internal object MHCFirestoreSetup {

    /**
     * The on-device Firestore cache budget, matching iOS.
     */
    private const val CACHE_SIZE_BYTES = 100L * 1024 * 1024

    private val logger by groveLogger(tag = "MHCFirebase")

    /**
     * Applies the app's Firestore settings and, if a previous session logged out, clears the cache
     * left behind by the participant who was signed in then.
     *
     * @param context Any context; the application context is used internally.
     */
    fun apply(context: Context) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(
                PersistentCacheSettings.newBuilder()
                    .setSizeBytes(CACHE_SIZE_BYTES)
                    .build(),
            )
            .build()

        // Strictly after the settings above: `useEmulator` folds the host into whatever settings are
        // current, so redirecting first and replacing them second would quietly undo the redirect
        // and point a verification run at the real project.
        MHCFirebaseEmulator.host?.let { host ->
            firestore.useEmulator(host, MHCFirebaseEmulator.FIRESTORE_PORT)
        }

        if (!MHCFirebaseLoader.shouldClearCacheOnNextLaunch(context = context)) return

        // Only the enqueueing has to happen here, ahead of any other Firestore call; the SDK runs
        // the work itself on its own queue, ahead of everything queued after this point.
        firestore.clearPersistence()
            .addOnSuccessListener {
                logger.i { "Cleared the Firestore cache left behind by the previous session." }
                MHCFirebaseLoader.setShouldClearCacheOnNextLaunch(context = context, shouldClear = false)
            }
            .addOnFailureListener { throwable ->
                // Deliberately keeps the flag set, so the next launch tries again.
                logger.e(throwable) { "Failed to clear the Firestore cache." }
            }
    }
}
