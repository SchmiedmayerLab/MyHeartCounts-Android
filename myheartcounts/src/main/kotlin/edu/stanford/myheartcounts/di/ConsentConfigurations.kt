//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.di

import edu.stanford.myheartcounts.standard.consent.ConsentPdfRenderer
import edu.stanford.myheartcounts.standard.consent.MHCConsentDocumentProvider
import edu.stanford.myheartcounts.standard.consent.MHCConsentFormRenderer
import edu.stanford.myheartcounts.standard.consent.MHCConsentFormRendererImpl
import edu.stanford.myheartcounts.standard.consent.MHCConsentHandler
import edu.stanford.myheartcounts.standard.consent.MHCConsentHandlerImpl
import edu.stanford.myheartcounts.standard.consent.MHCConsentSigning
import edu.stanford.myheartcounts.standard.consent.MHCConsentSigningImpl
import org.grovealliance.core.ConfigurationBuilder
import org.grovealliance.core.coroutines.Concurrency

/**
 * Registers the consent document, and rendering, uploading and sharing the signed form.
 */
fun ConfigurationBuilder.consentConfigurations() {
    module {
        MHCConsentDocumentProvider(
            studyBundleProvider = dependency(),
            variantProvider = dependency(),
        )
    }
    factory<MHCConsentSigning> {
        MHCConsentSigningImpl(
            account = dependency(),
            documentProvider = dependency(),
        )
    }
    singleton { ConsentPdfRenderer(context = appContext()) }
    factory<MHCConsentFormRenderer> {
        MHCConsentFormRendererImpl(
            variantProvider = dependency(),
            renderer = dependency(),
        )
    }
    factory<MHCConsentHandler> {
        MHCConsentHandlerImpl(
            documentProvider = dependency(),
            formRenderer = dependency(),
            account = dependency(),
            firestore = dependency(),
            timeProvider = dependency(),
            context = appContext(),
            navigator = dependency(),
            ioDispatcher = dependency<Concurrency>().ioDispatcher(),
        )
    }
}
