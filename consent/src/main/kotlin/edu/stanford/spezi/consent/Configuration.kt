//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.consent

import edu.stanford.spezi.consent.internal.ConsentDocumentDataSource
import edu.stanford.spezi.consent.internal.ConsentDocumentDataSourceImpl
import edu.stanford.spezi.consent.internal.ConsentLayoutMapper
import edu.stanford.spezi.consent.internal.ConsentLayoutMapperImpl
import edu.stanford.spezi.consent.internal.ConsentViewModel
import edu.stanford.spezi.core.ConfigurationBuilder
import edu.stanford.spezi.core.SpeziDsl
import edu.stanford.spezi.core.viewmodel.viewModel

/**
 * Registers the consent module within a Spezi [ConfigurationBuilder].
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
@SpeziDsl
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
    private var document: (suspend () -> ConsentDocument)? = null
    private var initialSignatureMetadata: () -> SignatureMetadata = { SignatureMetadata.Empty }

    /**
     * The source from which a consent document is loaded.
     *
     * Resolved when the document is first shown, so it may wait on content that is still loading.
     */
    fun document(document: suspend () -> ConsentDocument) {
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
    val document: suspend () -> ConsentDocument,
    val initialSignatureMetadata: () -> SignatureMetadata,
)
