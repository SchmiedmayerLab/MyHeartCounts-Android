//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.firebase

import com.google.firebase.FirebaseOptions
import kotlinx.serialization.Serializable

/**
 * The Firebase project options for a single study region, as stored in
 * `assets/firebase-config.json`.
 *
 * My Heart Counts runs one Firebase project per region, so the app ships the options for every
 * region and picks one at runtime once the participant has told us where they are. This mirrors
 * the combined, region-keyed `GoogleService-Info.plist` on iOS.
 *
 * @property projectId The Firebase project id.
 * @property applicationId The Firebase application (app) id for the Android client.
 * @property apiKey The Firebase API key.
 * @property gcmSenderId The Cloud Messaging sender id.
 * @property storageBucket The Cloud Storage bucket.
 */
@Serializable
data class MHCFirebaseProjectConfig(
    val projectId: String,
    val applicationId: String,
    val apiKey: String,
    val gcmSenderId: String,
    val storageBucket: String,
) {
    /**
     * Converts this configuration into the [FirebaseOptions] used to initialize the Firebase app.
     */
    fun toFirebaseOptions(): FirebaseOptions = FirebaseOptions.Builder()
        .setProjectId(projectId)
        .setApplicationId(applicationId)
        .setApiKey(apiKey)
        .setGcmSenderId(gcmSenderId)
        .setStorageBucket(storageBucket)
        .build()
}
