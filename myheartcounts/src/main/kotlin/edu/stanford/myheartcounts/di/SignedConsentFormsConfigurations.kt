//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.di

import edu.stanford.myheartcounts.consentforms.PdfPageRenderer
import edu.stanford.myheartcounts.consentforms.PdfPageRendererImpl
import edu.stanford.myheartcounts.consentforms.SignedConsentFormSource
import edu.stanford.myheartcounts.consentforms.SignedConsentFormSourceImpl
import edu.stanford.myheartcounts.consentforms.SignedConsentFormsMapper
import edu.stanford.myheartcounts.consentforms.SignedConsentFormsMapperImpl
import org.grovealliance.core.ConfigurationBuilder
import org.grovealliance.core.coroutines.Concurrency

/**
 * Registers the participant's signed consent forms: reading them, drawing their pages and their
 * presentation.
 */
fun ConfigurationBuilder.signedConsentFormsConfigurations() {
    singleton<SignedConsentFormSource> {
        SignedConsentFormSourceImpl(
            context = appContext(),
            firestore = dependency(),
            ioDispatcher = dependency<Concurrency>().ioDispatcher(),
        )
    }
    singleton<PdfPageRenderer> {
        PdfPageRendererImpl(
            ioDispatcher = dependency<Concurrency>().ioDispatcher(),
        )
    }
    singleton<SignedConsentFormsMapper> {
        SignedConsentFormsMapperImpl(
            timeProvider = dependency(),
        )
    }
}
