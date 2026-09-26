//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import edu.stanford.myheartcounts.account.LanguageKey
import edu.stanford.myheartcounts.account.LastActiveDateKey
import edu.stanford.myheartcounts.account.PreferredMeasurementSystemKey
import edu.stanford.myheartcounts.account.TimeZoneKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.grovealliance.account.Account
import org.grovealliance.account.AccountDetails
import org.grovealliance.account.AccountModifications
import org.grovealliance.core.lifecycle.AppLifecycle
import org.grovealliance.core.logging.groveLogger
import org.grovealliance.core.time.TimeProvider
import java.util.Locale
import java.util.TimeZone

/**
 * Keeps the environment fields on the participant's account document current.
 *
 * The study uses these to interpret the data a participant sends: `timeZone` places their samples on
 * a local clock, `lastActiveDate` says when they last opened the app, and `language` and
 * `preferredMeasurementSystem` say how their answers were presented to them. Ports iOS's
 * `EnvironmentTracking`.
 */
class MHCEnvironmentTracker(
    private val context: Context,
    private val account: Account,
    private val appLifecycle: AppLifecycle,
    private val timeProvider: TimeProvider,
) {

    private val logger by groveLogger(tag = "MHCFirebase")

    /**
     * Starts tracking on [scope].
     *
     * Updates for a signed-out participant are dropped, so the caller has to call [pushAll] once
     * somebody signs in — see its documentation.
     */
    fun start(scope: CoroutineScope) {
        scope.launch {
            appLifecycle.state
                .filter { it == AppLifecycle.State.FOREGROUND }
                .collect { recordLastActive() }
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_TIMEZONE_CHANGED -> scope.launch { recordTimeZone() }
                    Intent.ACTION_LOCALE_CHANGED -> scope.launch { recordLocale() }
                    else -> Unit
                }
            }
        }
        context.applicationContext.registerReceiver(
            receiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
                addAction(Intent.ACTION_LOCALE_CHANGED)
            },
            Context.RECEIVER_NOT_EXPORTED,
        )

        scope.launch { pushAll() }
    }

    /**
     * Pushes every tracked field, whether or not it changed.
     *
     * The broadcasts and lifecycle events above only fire on a *change*, so without this a
     * participant whose device settings never change afterwards would have no `timeZone` or
     * `language` recorded at all. It matters most right after a sign-in: [start] runs once, early,
     * and everything it pushes then is dropped because nobody is signed in yet. iOS calls its
     * `triggerAll` for the same reason.
     */
    suspend fun pushAll() {
        recordLastActive()
        recordTimeZone()
        recordLocale()
    }

    private suspend fun recordLastActive() {
        update { it[LastActiveDateKey::class] = timeProvider.nowInstant() }
    }

    private suspend fun recordTimeZone() {
        update { it[TimeZoneKey::class] = TimeZone.getDefault().id }
    }

    private suspend fun recordLocale() {
        val locale = Locale.getDefault()
        update {
            it[LanguageKey::class] = locale.language.ifEmpty { FALLBACK_LANGUAGE }
            it[PreferredMeasurementSystemKey::class] = measurementSystem(locale = locale)
        }
    }

    /**
     * Applies [changes] to the account, doing nothing when nobody is signed in.
     */
    private suspend fun update(changes: (AccountDetails) -> Unit) {
        if (!account.isSignedIn) return
        val modifiedDetails = AccountDetails()
        changes(modifiedDetails)
        AccountModifications(modifiedDetails = modifiedDetails)
            .mapCatching { account.service.updateAccountDetails(it).getOrThrow() }
            .onFailure { logger.e(it) { "Failed to update the account's environment fields." } }
    }

    /**
     * The measurement system identifier iOS would report for [locale].
     *
     * Foundation derives this from the region, and both platforms write into the same field, so the
     * three identifiers it uses are reproduced here rather than invented.
     */
    private fun measurementSystem(locale: Locale): String = when (locale.country) {
        "US", "LR", "MM" -> "us"
        "GB" -> "uk"
        else -> "metric"
    }

    private companion object {
        /**
         * Matches iOS, which falls back to English when the locale carries no language code.
         */
        const val FALLBACK_LANGUAGE = "en"
    }
}
