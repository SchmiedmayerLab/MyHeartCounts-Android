package edu.stanford.spezi.consent.internal

/**
 * Tag names, attribute names, and special values that define the consent document element vocabulary.
 */
internal object ConsentConstants {

    const val TAG_TOGGLE = "toggle"
    const val TAG_SELECT = "select"
    const val TAG_SIGNATURE = "signature"
    const val TAG_OPTION = "option"
    const val TAG_FOOTNOTE = "footnote"

    const val ATTR_INITIAL_VALUE = "initial-value"
    const val ATTR_EXPECTED_VALUE = "expected-value"

    /**
     * Wildcard `expected-value` meaning any non-empty selection satisfies completion.
     */
    const val EXPECTED_SELECTION_ANY = "*"

    /**
     * The complete set of custom element names recognized when processing a consent document.
     */
    val ELEMENT_NAMES: Set<String> = setOf(
        TAG_TOGGLE, TAG_SELECT, TAG_SIGNATURE, TAG_OPTION, TAG_FOOTNOTE,
    )
}
