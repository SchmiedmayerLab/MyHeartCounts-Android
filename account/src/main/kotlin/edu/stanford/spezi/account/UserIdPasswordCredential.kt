//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.account

/**
 * Credentials for signing in with a user ID and password.
 *
 * @param userId The user's identifier, e.g. email or username.
 * @param password The user's password.
 */
data class UserIdPasswordCredential(
    val userId: String,
    val password: String,
)
