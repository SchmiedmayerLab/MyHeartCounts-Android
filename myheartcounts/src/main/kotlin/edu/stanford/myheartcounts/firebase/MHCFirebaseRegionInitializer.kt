//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.firebase

import android.content.Context
import edu.stanford.myheartcounts.model.Country
import org.grovealliance.core.logging.groveLogger
import org.grovealliance.firebase.FirebaseAppConfiguration

/**
 * Initializes Firebase for the region a participant selected during onboarding.
 *
 * This is the runtime half of the deferred initialization described on [MHCFirebaseLoader]: it
 * resolves the Firebase project for a [Country] and hands it to Grove's [FirebaseAppConfiguration],
 * so every module waiting on Firebase learns that it became available.
 */
interface MHCFirebaseRegionInitializer {

    /**
     * Initializes Firebase for [country] and remembers the choice for later launches.
     *
     * Participants in a country the study has not launched in still need Firebase, because joining
     * the launch waitlist signs in anonymously and calls a Cloud Function. Those countries fall
     * back to the default region's Firebase project, matching iOS.
     *
     * Calling this repeatedly is safe; the first successful initialization wins.
     *
     * @param country The country the participant selected.
     * @return `true` when Firebase is initialized and ready to use.
     */
    fun initialize(country: Country): Boolean
}

/**
 * Default [MHCFirebaseRegionInitializer] implementation.
 */
class MHCFirebaseRegionInitializerImpl(
    private val context: Context,
    private val firebaseAppConfiguration: FirebaseAppConfiguration,
) : MHCFirebaseRegionInitializer {

    private val logger by groveLogger(tag = "MHCFirebase")

    override fun initialize(country: Country): Boolean {
        val region = regionFor(country)
        val options = MHCFirebaseLoader.optionsFor(context = context, regionCode = region) ?: run {
            logger.e { "No Firebase configuration shipped for region '$region'." }
            return false
        }
        val configured = firebaseAppConfiguration.configure(options = options).isSuccess
        if (configured) {
            MHCFirebaseLoader.persistRegion(context = context, regionCode = region)
        }
        return configured
    }

    /**
     * The region whose Firebase project serves [country].
     *
     * Only countries the study has actually launched in get their own project. Everyone else —
     * whether the study is coming soon there or not planned at all — falls back to
     * [FALLBACK_REGION], because the only thing they can do is join the launch waitlist and that
     * lives in the fallback project. Note that a coming-soon country may already have a Firebase
     * project shipped in the config: it must still not be used until the study launches there,
     * which is why this keys on [Country.isEnabled] rather than on whether a config exists.
     */
    private fun regionFor(country: Country): String =
        if (country.isEnabled && MHCFirebaseLoader.hasConfigFor(context = context, regionCode = country.code)) {
            country.code
        } else {
            FALLBACK_REGION
        }

    private companion object {
        /**
         * The region serving participants whose country the study has not launched in. Matches
         * iOS, which loads the United States project before calling `joinWaitlist`.
         */
        const val FALLBACK_REGION = "US"
    }
}
