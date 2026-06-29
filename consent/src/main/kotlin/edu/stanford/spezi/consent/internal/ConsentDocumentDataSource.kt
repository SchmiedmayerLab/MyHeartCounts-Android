package edu.stanford.spezi.consent.internal

import android.content.Context
import edu.stanford.spezi.consent.ConsentConfiguration
import edu.stanford.spezi.consent.ConsentDocument
import edu.stanford.spezi.core.coroutines.Concurrency
import edu.stanford.spezi.markdown.MarkdownDocument
import kotlinx.coroutines.withContext

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
