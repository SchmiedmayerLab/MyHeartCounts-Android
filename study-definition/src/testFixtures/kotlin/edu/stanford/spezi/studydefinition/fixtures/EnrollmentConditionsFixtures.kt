//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.studydefinition.fixtures

import edu.stanford.spezi.studydefinition.EnrollmentConditions

/**
 * Fixtures for [EnrollmentConditions]. [create] returns [EnrollmentConditions.None].
 */
object EnrollmentConditionsFixtures {
    fun create(): EnrollmentConditions = EnrollmentConditions.None

    fun createRequiresInvitation(verificationEndpoint: String = ""): EnrollmentConditions.RequiresInvitation =
        EnrollmentConditions.RequiresInvitation(verificationEndpoint)
}
