package edu.stanford.spezi.consent

/**
 * The source from which a consent document is loaded.
 */
sealed interface ConsentDocument {
    /**
     * Loads the document from an asset file at [filename] (relative to the app's assets root).
     */
    data class Asset(val filename: String) : ConsentDocument

    /**
     * Uses the given markdown [text] directly as the document source.
     */
    data class Text(val text: String) : ConsentDocument
}
