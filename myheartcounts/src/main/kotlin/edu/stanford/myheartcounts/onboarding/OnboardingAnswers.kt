//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.onboarding

import edu.stanford.myheartcounts.model.Country
import org.grovealliance.consent.ConsentResponses

/**
 * The answers collected across the onboarding steps. Each yes/no field is `null` until answered.
 */
data class OnboardingAnswers(
    val isOfAge: Boolean,
    val country: Country?,
    val understandsEnglish: Boolean?,
    val usesSharedAccount: Boolean?,
    val seeksHelpWhenUnwell: Boolean?,
    val participationIsVoluntary: Boolean?,
    val canWithdrawAnytime: Boolean?,
    val consentResponses: ConsentResponses?,
) {
    companion object {
        /**
         * The initial answers before any onboarding step has been completed.
         */
        val default = OnboardingAnswers(
            isOfAge = false,
            country = null,
            understandsEnglish = null,
            usesSharedAccount = null,
            seeksHelpWhenUnwell = null,
            participationIsVoluntary = null,
            canWithdrawAnytime = null,
            consentResponses = null,
        )
    }
}
