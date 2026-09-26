//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account

import edu.stanford.myheartcounts.standard.MHCStandard
import org.grovealliance.account.AccountService
import org.grovealliance.core.ApplicationModule
import org.grovealliance.core.logging.groveLogger
import org.grovealliance.core.requireDependency

/**
 * Wraps an [AccountService] to run the app's teardown while the participant is still signed in.
 *
 * Grove only reports that a participant *has* signed out, and by then their Firebase credentials are
 * gone and the backend rejects every write. Some teardown has to happen before that — most of all
 * removing `fcmToken`, which the backend reads straight off the user document and would otherwise
 * keep pushing nudges to a device nobody is signed in on. Wrapping the service is the only place
 * that sees the intent to log out rather than the aftermath, and it is the counterpart of iOS's
 * `willLogOut` account event.
 *
 * @param wrapped The service that actually performs the operations, delegated to unchanged.
 * @param willSignOut Teardown to run before logging out or deleting; failures are logged, never
 * propagated, because they must not leave a participant unable to sign out.
 */
class MHCAccountService(
    private val wrapped: AccountService,
    private val willSignOut: suspend () -> Unit,
) : AccountService by wrapped {

    private val logger by groveLogger(tag = "MHCFirebase")

    override suspend fun logout(): Result<Unit> {
        runWillSignOut()
        return wrapped.logout()
    }

    override suspend fun delete(): Result<Unit> {
        runWillSignOut()
        return wrapped.delete()
    }

    private suspend fun runWillSignOut() {
        runCatching { willSignOut() }
            .onFailure { logger.e(it) { "Sign-out teardown failed; signing out anyway." } }
    }

    companion object {

        /**
         * Wraps [wrapped] so that [MHCStandard] gets to tear down before a sign-out.
         *
         * The standard is reached through [ApplicationModule] rather than resolved by type, because
         * Grove holds it on the configuration rather than in the dependency graph — the same route
         * Grove's own `HealthClient` takes. It is resolved only when a sign-out actually happens,
         * since this runs while the configuration is still being built.
         */
        fun wrapping(wrapped: AccountService): AccountService = MHCAccountService(
            wrapped = wrapped,
            willSignOut = {
                (requireDependency<ApplicationModule>().standard as? MHCStandard)?.willSignOut()
            },
        )
    }
}
