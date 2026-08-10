//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

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
