//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.study

import org.grovealliance.foundation.fixtures.InstantFixtures
import java.time.Instant

/**
 * Builds [EnrolledStudy]s; by default without a description, enrolled at [InstantFixtures.reference].
 */
object EnrolledStudyFixtures {
    fun create(
        title: String? = null,
        shortExplanation: String? = null,
        enrollmentDate: Instant = InstantFixtures.reference,
    ) = EnrolledStudy(
        title = title,
        shortExplanation = shortExplanation,
        enrollmentDate = enrollmentDate,
    )
}
