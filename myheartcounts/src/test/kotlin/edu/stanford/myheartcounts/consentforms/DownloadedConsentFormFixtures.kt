//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.consentforms

import java.io.File

/**
 * Builds [DownloadedConsentForm]s; by default of an empty form, at an empty path.
 */
object DownloadedConsentFormFixtures {
    fun create(
        form: SignedConsentForm = SignedConsentFormFixtures.create(),
        file: File = File(""),
        contentUri: String = "",
    ) = DownloadedConsentForm(
        form = form,
        file = file,
        contentUri = contentUri,
    )
}
