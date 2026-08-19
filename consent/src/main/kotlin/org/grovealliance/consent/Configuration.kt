//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.consent

import org.grovealliance.consent.internal.ConsentDocumentDataSource
import org.grovealliance.consent.internal.ConsentDocumentDataSourceImpl
import org.grovealliance.consent.internal.ConsentLayoutMapper
import org.grovealliance.consent.internal.ConsentLayoutMapperImpl
import org.grovealliance.consent.internal.ConsentViewModel
import org.grovealliance.core.ConfigurationBuilder
import org.grovealliance.core.GroveDsl
import org.grovealliance.core.viewmodel.viewModel

/**
 * Registers the consent module within a Grove [ConfigurationBuilder].
 *
 * Example usage:
 * ```kotlin
 * consent {
 *     document { ConsentDocument.Asset("consent.md") }
 *     initialSignatureMetadata {
 *         val user = yourDataSource.getCurrentUser()
 *         SignatureMetadata(
 *             givenName = user.givenName,
 *             familyName = user.familyName,
 *             strokes = emptyList(),
 *         )
 *     }
 * }
 * ```
 */
@GroveDsl
fun ConfigurationBuilder.consent(
    builder: ConsentConfigurationBuilder.() -> Unit,
) {
    val configuration = ConsentConfigurationBuilder().apply(builder).build()
    singleton { configuration }
    viewModel {
        ConsentViewModel(
            configuration = dependency(),
            mapper = dependency(),
            dataSource = dependency(),
        )
    }
    factory<ConsentLayoutMapper> {
        ConsentLayoutMapperImpl(
            dateFormatter = dependency(),
            timeProvider = dependency(),
        )
    }

    factory<ConsentDocumentDataSource> {
        ConsentDocumentDataSourceImpl(
            context = appContext(),
            configuration = dependency(),
            concurrency = dependency(),
        )
    }
}

/**
 * Configures the consent module before it is registered.
 *
 * At minimum, a [document] must be supplied. Initial signature metadata is optional and
 * pre-populates signature fields.
 */
class ConsentConfigurationBuilder {
    private var document: (() -> ConsentDocument)? = null
    private var initialSignatureMetadata: () -> SignatureMetadata = { SignatureMetadata.Empty }

    /**
     * The source from which a consent document is loaded.
     */
    fun document(document: () -> ConsentDocument) {
        this.document = document
    }

    /**
     * Provides signature metadata to pre-populate signature elements.
     */
    fun initialSignatureMetadata(metadata: () -> SignatureMetadata) {
        this.initialSignatureMetadata = metadata
    }

    internal fun build(): ConsentConfiguration {
        return ConsentConfiguration(
            document = requireNotNull(document) { "A consent document must be provided." },
            initialSignatureMetadata = initialSignatureMetadata,
        )
    }
}

internal data class ConsentConfiguration(
    val document: () -> ConsentDocument,
    val initialSignatureMetadata: () -> SignatureMetadata,
)
