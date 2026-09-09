//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.firebase

import android.content.Context
import com.google.firebase.FirebaseOptions
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import org.grovealliance.core.logging.groveLogger
import org.grovealliance.foundation.JsonSerializer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves which Firebase project the app talks to, and remembers the choice across launches.
 *
 * My Heart Counts runs a separate Firebase project per region, and which one to talk to is only
 * known once the participant has picked their country during onboarding. Firebase is therefore
 * **not** initialized automatically at process start — `FirebaseInitProvider` is removed in the app
 * manifest — and there are exactly two ways it comes up:
 *
 * - **A returning participant**: [persistedRegionOptions] feeds `firebaseApp(options = …)` when the
 *   app builds its Grove configuration, so Firebase is up before any module is configured.
 * - **A new participant**: [MHCFirebaseRegionInitializer] configures it mid-session, once onboarding
 *   knows the country.
 *
 * This mirrors `DeferredConfigLoading` on iOS. The region-agnostic half of the mechanism — owning
 * the Firebase app and telling other modules when it is ready — lives in Grove's
 * [org.grovealliance.firebase.FirebaseAppConfiguration].
 *
 * The choice is persisted in a dedicated [android.content.SharedPreferences] file rather than in
 * Grove's `KeyValueStorage`, because it is read while the configuration is being built and Grove's
 * dependency graph does not exist yet.
 */
object MHCFirebaseLoader {

    private const val PREFERENCES_NAME = "edu.stanford.myheartcounts.FIREBASE"
    private const val KEY_REGION = "region"
    private const val KEY_CLEAR_CACHE = "shouldClearFirestoreCacheOnNextLaunch"
    private const val CONFIG_ASSET = "firebase-config.json"

    private val logger by groveLogger(tag = "MHCFirebase")

    /**
     * The parsed contents of [CONFIG_ASSET], keyed by ISO 3166-1 alpha-2 region code matching
     * [edu.stanford.myheartcounts.model.Country].
     *
     * Note that iOS keys the equivalent plist on `UK` rather than the ISO code `GB`; each platform
     * keys on its own internal model, so the two config files intentionally differ in shape.
     */
    private val configCache = ConcurrentHashMap<String, MHCFirebaseProjectConfig>()

    /**
     * The [FirebaseOptions] for the region the participant previously chose, or `null` on a first
     * launch where they have not chosen one yet.
     *
     * The app passes this straight into `firebaseApp(options = …)`, so that a returning participant
     * has Firebase up before Grove configures any module, while a new one has no Firebase at all
     * until onboarding reaches region selection.
     */
    fun persistedRegionOptions(context: Context): FirebaseOptions? =
        persistedRegionCode(context)?.let { region ->
            optionsFor(context = context, regionCode = region) ?: run {
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
     * @param context Any context; the application context is used internally.
     * @param regionCode An ISO 3166-1 alpha-2 country code.
     */
    fun persistRegion(context: Context, regionCode: String) {
        preferences(context).edit().putString(KEY_REGION, regionCode).apply()
    }

    /**
     * The region the participant previously chose, or `null` if they have not chosen one yet.
     */
    fun persistedRegionCode(context: Context): String? =
        preferences(context).getString(KEY_REGION, null)

    /**
     * The locale used to resolve localized study-bundle resources, derived from the persisted
     * region, or `null` when no region has been chosen yet.
     *
     * Grove's `studyManager(preferredLocale = …)` captures its locale when the configuration is
     * built, so on the very first launch — where the region is only chosen part-way through the
     * session — the study bundle stays on the default locale until the next process start.
     */
    fun persistedRegionLocale(context: Context): Locale? =
        persistedRegionCode(context)?.let { region ->
            Locale.Builder().setLanguage(Locale.getDefault().language).setRegion(region).build()
        }

    /**
     * Returns the [FirebaseOptions] shipped for [regionCode], or `null` when the app carries no
     * configuration for that region.
     */
    fun optionsFor(context: Context, regionCode: String): FirebaseOptions? =
        projectConfig(context = context, regionCode = regionCode)?.toFirebaseOptions()

    /**
     * Whether the app ships a Firebase configuration for [regionCode].
     */
    fun hasConfigFor(context: Context, regionCode: String): Boolean =
        projectConfig(context = context, regionCode = regionCode) != null

    /**
     * Whether the Firestore cache has to be cleared before Firestore is next used.
     *
     * Set when the participant logs out: their cached documents must not survive into the next
     * session, and `clearPersistence` only works while the Firestore client has not started, which
     * on Android means the next process launch. Mirrors iOS's `FirestoreCacheCleanup`.
     */
    fun shouldClearCacheOnNextLaunch(context: Context): Boolean =
        preferences(context).getBoolean(KEY_CLEAR_CACHE, false)

    /**
     * Records whether the Firestore cache has to be cleared on the next launch. See
     * [shouldClearCacheOnNextLaunch].
     */
    fun setShouldClearCacheOnNextLaunch(context: Context, shouldClear: Boolean) {
        preferences(context).edit().putBoolean(KEY_CLEAR_CACHE, shouldClear).apply()
    }

    private fun projectConfig(context: Context, regionCode: String): MHCFirebaseProjectConfig? {
        configCache[regionCode]?.let { return it }
        val configs = runCatching {
            val text = context.applicationContext.assets.open(CONFIG_ASSET)
                .bufferedReader()
                .use { it.readText() }
            JsonSerializer.decode(
                text = text,
                deserializer = MapSerializer(String.serializer(), MHCFirebaseProjectConfig.serializer()),
            )
        }.onFailure { throwable ->
            logger.e(throwable) { "Failed to read '$CONFIG_ASSET'." }
        }.getOrNull() ?: return null

        configCache.putAll(configs)
        return configs[regionCode]
    }

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
}
