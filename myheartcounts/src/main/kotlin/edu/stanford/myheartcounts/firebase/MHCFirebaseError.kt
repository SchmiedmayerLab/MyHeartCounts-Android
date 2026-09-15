//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.firebase

/**
 * Errors raised when the app cannot reach its Firebase backend.
 */
sealed class MHCFirebaseError(message: String) : Exception(message) {

    /**
     * Raised when a backend operation needs a signed-in participant and there is none.
     */
    class NotSignedIn : MHCFirebaseError("No participant is signed in.")

    /**
     * Raised when a backend operation runs before the Firebase app has been initialized, which only
     * happens once the participant has selected their region.
     */
    class NotConfigured : MHCFirebaseError("Firebase has not been configured yet.")
}
