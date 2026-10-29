//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard.consent

/**
 * A [MHCConsentSigning] whose answer is set directly; signing is required until told otherwise.
 */
class FakeConsentSigning : MHCConsentSigning {
    private var isRequired = true

    /**
     * Sets what [isSigningRequired] returns.
     */
    fun setSigningRequired(required: Boolean) {
        isRequired = required
    }

    override suspend fun isSigningRequired(): Boolean = isRequired
}
