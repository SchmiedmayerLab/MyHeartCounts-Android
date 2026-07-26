//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.account

import edu.stanford.spezi.core.Module
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/**
 * The central [Account] module component.
 */
interface Account : Module {

    /**
     * The [AccountService] of this [Account] as configured in Spezi configuration.
     */
    val service: AccountService

    /**
     * Configured account keys with their requirement configuration
     */
    val configuration: AccountValueConfiguration

    /**
     * The current [AccountDetails] of the user, or null if no user is signed in.
     */
    val details: StateFlow<AccountDetails?>

    /**
     * Whether a user is currently signed in, i.e. whether [details] is not null.
     */
    val isSignedIn: Boolean
        get() = details.value != null

    /**
     * Supplies the [Account] with new user details, e.g. after a successful sign in or sign up.
     * The provided [details] must contain an account id under the key specified by [AccountKeys.accountId].
     *
     * This method is intended to be called by the [AccountService] implementation of this [Account]
     * after successful sign in or sign up operations
     */
    fun supplyUserDetails(details: AccountDetails)

    /**
     * Removes the current user details, effectively signing out the user. This method is intended to be called by the [AccountService]
     */
    fun removeUserDetails()
}

/**
 * Observes whether a user is currently signed in to the account.
 */
fun Account.observeIsSignedIn(): Flow<Boolean> =
    details
        .map { it != null }
        .distinctUntilChanged()

/**
 * Observes sign-in events, i.e. when a user signs in to the account.
 */
fun Account.observeSignInEvents(): Flow<Unit> =
    observeIsSignedIn()
        .drop(1)
        .filter { it }
        .map { }

/**
 * Observes sign-out events, i.e. when a user signs out of the account.
 */
fun Account.observeSignOutEvents(): Flow<Unit> =
    observeIsSignedIn()
        .drop(1)
        .filter { !it }
        .map { }
