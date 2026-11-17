//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.verifyemail

/**
 * An intent raised by the email verification screen.
 */
sealed interface VerifyEmailAction {

    /**
     * The participant closed the screen.
     */
    data object Close : VerifyEmailAction

    /**
     * The participant asked to open their email app to find the verification link.
     */
    data object OpenMailSelected : VerifyEmailAction
}
