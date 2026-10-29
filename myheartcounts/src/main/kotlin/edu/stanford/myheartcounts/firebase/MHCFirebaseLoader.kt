//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.firebase

import android.content.Context
import com.google.firebase.FirebaseOptions
import org.grovealliance.storage.local.KeyValueStorageFactory
import org.grovealliance.storage.local.KeyValueStorageType

/**
 * Resolves which Firebase project the app talks to, and remembers the choice across launches.
 *
 * My Heart Counts runs a separate Firebase project per region, and which one to talk to is only
 * known once the participant has picked their country during onboarding. Firebase is therefore
 * **not** initialized automatically at process start — `FirebaseInitProvider` is removed in the app
 * manifest — and there are exactly two ways it comes up:
 *
 * - **A returning participant**: [persistedRegionOptions] feeds `firebaseApp(options = …)`, so
 *   Firebase is up before any module awaiting it is configured.
 * - **A new participant**: [MHCFirebaseRegionInitializer] configures it mid-session, once onboarding
 *   knows the country.
 *
 * The region-agnostic half of the mechanism — owning the Firebase app and telling other modules
 * when it is ready — lives in Grove's [org.grovealliance.firebase.FirebaseAppConfiguration].
 *
 * The project options come from the standard `google-services.json`, which the
 * `com.google.gms.google-services` Gradle plugin turns into string resources at build time. Only
 * the automatic initialization is dropped, not the file itself.
 *
 * @param context Reads the project options from the app's resources.
 * @param storageFactory Creates the storage the region and the cache flag persist in.
 */
class MHCFirebaseLoader(
    private val context: Context,
    storageFactory: KeyValueStorageFactory,
) {

    private val logger by mhcFirebaseLogger()
    private val storage = storageFactory.create(fileName = STORAGE_FILE, type = KeyValueStorageType.UNENCRYPTED)

    /**
     * The [FirebaseOptions] for the region the participant previously chose, or `null` on a first
     * launch where they have not chosen one yet.
     */
    fun persistedRegionOptions(): FirebaseOptions? =
        persistedRegionCode()?.let { region ->
            optionsFor(regionCode = region) ?: run {
                logger.e { "No Firebase configuration shipped for the persisted region '$region'." }
                null
            }
        }

    /**
     * Records [regionCode] as the participant's region, so later launches initialize Firebase for
     * it from [persistedRegionOptions].
     *
     * Initializing Firebase itself goes through [MHCFirebaseRegionInitializer], which routes it via
     * Grove so dependent modules learn that Firebase became available.
     *
     * @param regionCode An ISO 3166-1 alpha-2 country code.
     */
    fun persistRegion(regionCode: String) {
        storage.putString(key = KEY_REGION, value = regionCode)
    }

    /**
     * Forgets the participant's region, so the next launch leaves Firebase uninitialized until
     * onboarding selects a region again.
     */
    fun clearRegion() {
        storage.delete(key = KEY_REGION)
    }

    /**
     * The region the participant previously chose, or `null` if they have not chosen one yet.
     */
    fun persistedRegionCode(): String? = storage.getString(key = KEY_REGION)

    /**
     * Returns the [FirebaseOptions] for [regionCode], or `null` when the build carries no Firebase
     * configuration.
     *
     * A build is configured from a single `google-services.json`, so every region currently
     * resolves to the same project. That covers the study as launched, where only the United
     * States is enabled and everyone else falls back to it. A second regional project needs its
     * own configuration source before it can be told apart here.
     */
    @Suppress("UnusedParameter")
    fun optionsFor(regionCode: String): FirebaseOptions? = FirebaseOptions.fromResource(context)

    /**
     * Whether the build carries a Firebase configuration for [regionCode].
     */
    fun hasConfigFor(regionCode: String): Boolean = optionsFor(regionCode = regionCode) != null

    /**
     * Whether the Firestore cache has to be cleared before Firestore is next used.
     *
     * Set when the participant logs out: their cached documents must not survive into the next
     * session, and `clearPersistence` only works while the Firestore client has not started, which
     * on Android means the next process launch.
     */
    fun shouldClearCacheOnNextLaunch(): Boolean = storage.getBoolean(key = KEY_CLEAR_CACHE, default = false)

    /**
     * Records whether the Firestore cache has to be cleared on the next launch. See
     * [shouldClearCacheOnNextLaunch].
     */
    fun setShouldClearCacheOnNextLaunch(shouldClear: Boolean) {
        storage.putBoolean(key = KEY_CLEAR_CACHE, value = shouldClear)
    }

    private companion object {
        const val STORAGE_FILE = "edu.stanford.myheartcounts.FIREBASE"
        const val KEY_REGION = "region"
        const val KEY_CLEAR_CACHE = "shouldClearFirestoreCacheOnNextLaunch"
    }
}
