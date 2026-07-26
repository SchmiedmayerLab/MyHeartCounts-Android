//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.account

import edu.stanford.spezi.ui.ImageResource
import edu.stanford.spezi.ui.StringResource

/**
 * Represents an authentication provider via which a user can sign in in the application.
 */
interface AuthProvider {
    /**
     * The name of the sign in action, e.g. Sign in with Google
     */
    val actionName: StringResource

    /**
     * An optional icon to be displayed alongside the action name
     */
    val icon: ImageResource?
}
