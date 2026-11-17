//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.consentforms

import java.time.Instant

/**
 * Builds [SignedConsentForm]s; by default one stored without any details.
 */
object SignedConsentFormFixtures {
    fun create(
        id: String = "",
        title: String? = null,
        version: String? = null,
        signedAt: Instant? = null,
    ) = SignedConsentForm(
        id = id,
        title = title,
        version = version,
        signedAt = signedAt,
    )
}
