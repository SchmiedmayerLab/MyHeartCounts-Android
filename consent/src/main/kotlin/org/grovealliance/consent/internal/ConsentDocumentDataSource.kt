//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.consent.internal

import android.content.Context
import kotlinx.coroutines.withContext
import org.grovealliance.consent.ConsentConfiguration
import org.grovealliance.consent.ConsentDocument
import org.grovealliance.core.coroutines.Concurrency
import org.grovealliance.markdown.MarkdownDocument

/**
 * Loads the consent document as a parsed [MarkdownDocument].
 */
internal interface ConsentDocumentDataSource {
    suspend fun loadDocument(): MarkdownDocument
}

/**
 * Reads the document source from configuration and parses it into a [MarkdownDocument],
 * recognising the consent element vocabulary.
 */
internal class ConsentDocumentDataSourceImpl(
    private val context: Context,
    private val configuration: ConsentConfiguration,
    private val concurrency: Concurrency,
) : ConsentDocumentDataSource {
    override suspend fun loadDocument(): MarkdownDocument = withContext(concurrency.ioDispatcher()) {
        val markdownText = when (val source = configuration.document()) {
            is ConsentDocument.Text -> source.text
            is ConsentDocument.Asset -> context.assets.open(source.filename).bufferedReader().use { it.readText() }
        }
        MarkdownDocument.process(
            text = markdownText,
            customElementNames = ConsentConstants.ELEMENT_NAMES,
        )
    }
}
