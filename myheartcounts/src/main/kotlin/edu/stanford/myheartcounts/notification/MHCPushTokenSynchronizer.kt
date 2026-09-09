//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.notification

import com.google.firebase.messaging.FirebaseMessaging
import edu.stanford.myheartcounts.account.FcmTokenKey
import edu.stanford.myheartcounts.account.fcmToken
import kotlinx.coroutines.tasks.await
import org.grovealliance.account.Account
import org.grovealliance.account.AccountDetails
import org.grovealliance.account.AccountModifications
import org.grovealliance.core.Module
import org.grovealliance.core.logging.groveLogger
import org.grovealliance.firebase.FirebaseAppConfiguration

/**
 * Keeps `fcmToken` on the participant's account document in step with the device's actual token.
 *
 * The backend's `sendNudges` reads the token straight off the user document, so a stale one silently
 * stops that participant's nudges. Ports iOS's `NotificationsManager.setFCMToken`.
 */
class MHCPushTokenSynchronizer(
    private val account: Account,
    private val firebaseAppConfiguration: FirebaseAppConfiguration,
) : Module {

    private val logger by groveLogger(tag = "MHCFirebase")

    /**
     * Fetches the device's current token and stores it, if it differs from what is stored.
     *
     * Called once per sign-in; token *changes* while signed in arrive through [onTokenRefreshed]
     * instead.
     */
    suspend fun synchronize() {
        runCatching {
            firebaseAppConfiguration.awaitConfigured()
            FirebaseMessaging.getInstance().token.await()
        }.onFailure { throwable ->
            logger.e(throwable) { "Failed to read the push token." }
        }.onSuccess { token ->
            store(token = token)
        }
    }

    /**
     * Stores a token Firebase rotated to.
     */
    suspend fun onTokenRefreshed(token: String) {
        store(token = token)
    }

    /**
     * Removes the token from the account document, so the backend stops pushing to this device.
     *
     * Called before logging out, while the participant is still signed in and can still write.
     */
    suspend fun clear() {
        val storedToken = account.details.value?.fcmToken ?: return
        val removedDetails = AccountDetails()
        removedDetails[FcmTokenKey::class] = storedToken
        apply(AccountModifications(removedAccountDetails = removedDetails))
    }

    private suspend fun store(token: String) {
        val details = account.details.value ?: return
        if (details.fcmToken == token) return
        val modifiedDetails = AccountDetails()
        modifiedDetails[FcmTokenKey::class] = token
        apply(AccountModifications(modifiedDetails = modifiedDetails))
    }

    private suspend fun apply(modifications: Result<AccountModifications>) {
        modifications
            .mapCatching { account.service.updateAccountDetails(it).getOrThrow() }
            .onFailure { logger.e(it) { "Failed to update the push token on the account." } }
    }
}
