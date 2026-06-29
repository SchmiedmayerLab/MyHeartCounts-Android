package edu.stanford.spezi.ui

/**
 * The navigation icon a screen presents for dismissing itself, e.g. applied to a [SpeziAppBar].
 */
enum class DismissStyle {

    /**
     * No navigation icon is shown.
     */
    NONE,

    /**
     * A back arrow that returns to the previous screen.
     */
    BACK,

    /**
     * A close icon that dismisses a modally presented screen.
     */
    CLOSE,
}
